package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy;

import com.google.common.base.Ascii;
import com.google.common.primitives.Bytes;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyServer;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.pipeline.FullFlowControlHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.DuplexChannelConfig;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * HTTP proxy factory.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class NettyHttpProxyFactory implements HttpProxyFactory
{
	static final Pattern HOST_PATTERN =
		Pattern.compile(
			"^(\\w+)\\s(?:" +
				"(?:(?<=connect\\s)\\s*([^\\s/]+?)(?::(\\d+))?\\s+(\\S+)\\s*\\n)|" +
				"(?:(?<!connect\\s)\\s*(\\w+)://([^\\s/:]+)(?::(\\d+))?(?:/|\\s+))|" +
				"(?:.*\\nhost:\\s*(\\S+?)(?::(\\d+))?\\s*\\n)" +
				")",
			Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
	private static final byte[] CONNECTION_HEADER = "connection".getBytes(StandardCharsets.UTF_8);
	private static final byte[] PROXY_CONNECTION_HEADER = "proxy-connection".getBytes(StandardCharsets.UTF_8);
	private static final byte[] HOST_HEADER = "host".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CLOSE = "close".getBytes(StandardCharsets.UTF_8);

	private final NettyEngine nettyEngine;

	@Override
	public CompletableFuture<Server> runProxy(Config config)
	{
		try {
			return new CompletableFuture<Server>() {
				CompletableFuture<ServerChannel> listenFuture;

				{
					listenFuture = nettyEngine.listen(
							config.getListenAddress(),
							new ChannelInitializer<DuplexChannel>()
							{
								@Override
								protected void initChannel(DuplexChannel ch) throws Exception
								{
									initializeServer(ch, config);
								}
							}
						)
						.whenComplete((v, ex) -> {
							if (ex != null) {
								completeExceptionally(ex);
							}
							else {
								complete(new NettyServer(
									v
								));
							}
						});
				}

				@Override
				public boolean cancel(boolean interrupt)
				{
					listenFuture.cancel(true);
					return super.cancel(interrupt);
				}
			};
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	CompletableFuture<Void> initializeServer(Channel client, Config config)
	{
		return new CompletableFuture<Void>() {
			{
				client.config().setAutoRead(false);
				((DuplexChannelConfig) client.config()).setAllowHalfClosure(true);
				client.pipeline().addLast(
					new FullFlowControlHandler(),
					new RequestReaderHandler(config, this)
				);
				whenComplete((v, ex) -> {
					if (ex != null) {
						client.close();
						log.error("Error processing proxy request", ex);
					}
				});
			}
		};
	}

	CompletableFuture<SocketAddress> resolveServer(Channel client, SocketAddress unresolved)
	{
		return nettyEngine.resolve(unresolved)
			.thenApply((v) -> {
				try {
					if (v.equals(client.localAddress())) {
						throw new IOException("Request connecting to same proxy");
					}
					return v;
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
	}

	static int findHeader(ByteBuf buffer, byte[] needle) {
		int end = buffer.writerIndex();
		if (needle.length == 0)
			return 0;
		OUT: for (int i = 0; i < end-needle.length; ++i) {
			if (buffer.getByte(i) != '\n')
				continue;
			int s = i+1;
			for (; s < end; ++s) {
				byte c = buffer.getByte(s);
				if (c != ' ' && c != '\t')
					break;
			}
			if (s+needle.length > end)
				return -1;
			for (int j = 0; j < needle.length; ++j) {
				if (Ascii.toLowerCase((char) buffer.getByte(s+j)) != Ascii.toLowerCase((char) needle[j]))
					continue OUT;
			}
			for (int k = s+needle.length; k < end; ++k) {
				byte c = buffer.getByte(k);
				if (c == ' ' || c == '\t')
					continue;
				if (c != ':')
					continue OUT;
				for (++k; k < end; k++) {
					if (buffer.getByte(k) == '\n')
						return i+1;
				}
				// no new line, no need to search for the rest
				return -1;
			}
		}
		return -1;
	}

	static int findNextLine(ContinuousByteBuf buffer, int offset) {
		for (int i = offset; ; ++i) {
			int c = buffer.get(i);
			if (c < 0)
				return -1;
			if (c == '\n')
				return i+1;
		}
	}

	static ByteBuf replaceHeaderValue(ByteBuf in, byte[] headerName, Function<byte[], byte[]> replacer)
	{
		int end = in.writerIndex();
		if (in.getByte(end-1) != '\n') {
			throw new IllegalArgumentException("Expected \\n at end of header buffer");
		}
		--end;
		if (in.getByte(end-1) == '\r') {
			--end;
		}
		int p = findHeader(in, headerName);
		if (p >= 0) {
			int valueStart;
			for (valueStart = p; in.getByte(valueStart) != ':'; ++valueStart) ;
			for (++valueStart; isSpace(in.getByte(valueStart)); ++valueStart) ;
			int lineEnd;
			for (lineEnd = valueStart; in.getByte(lineEnd) != '\n'; ++lineEnd) ;
			int valueEnd;
			for (valueEnd = lineEnd; isSpace(in.getByte(valueEnd-1)); --valueEnd) ;
			byte[] oldValue = copyBytes(in, valueStart, valueEnd);
			byte[] newValue = replacer.apply(oldValue);
			if (newValue == null) {
				return replaceBuffer(in, p, lineEnd+1, new byte[0]);
			}
			else {
				return replaceBuffer(in, valueStart, valueEnd, newValue);
			}
		}
		else {
			byte[] newValue = replacer.apply(null);
			if (newValue == null) {
				return in;
			}
			else {
				return replaceBuffer(in, end, end, Bytes.concat(headerName, new byte[]{ ':', ' ' }, newValue, new byte[]{ '\r', '\n' }));
			}
		}
	}

	static ByteBuf replaceHttpMethodHost(ByteBuf in, byte[] host)
	{
		int p;
		for (p = 0; isSpace(in.getByte(p)); ++p) ;
		for ( ; Character.isAlphabetic(in.getByte(p)); ++p) ;
		for ( ; isSpace(in.getByte(p)); ++p) ;
		if (in.getByte(p) == 'h' && in.getByte(p+1) == 't' && in.getByte(p+2) == 't' && in.getByte(p+3) == 'p' && in.getByte(p+4) == ':' && in.getByte(p+5) == '/' && in.getByte(p+6) == '/') {
			int e;
			for (e = p +=  7; !isSpace(in.getByte(e)) && in.getByte(e) != '/'; ++e) ;
			return replaceBuffer(in, p, e, host);
		}
		else {
			return in;
		}
	}

	static private byte[] copyBytes(ByteBuf in, int start, int end)
	{
		byte[] bytes = new byte[end-start];
		for (int i = 0; i < end-start; ++i) {
			bytes[i] = in.getByte(start+i);
		}
		return bytes;
	}

	static ByteBuf replaceBuffer(ByteBuf in, int start, int end, byte[] value)
	{
		int length = in.writerIndex();
		int diff = value.length-(end-start);
		if (in.writerIndex()+diff <= in.capacity()) {
			if (diff > 0) {
				for (int i = length-end; --i >= 0; ) {
					in.setByte(end+diff+i, in.getByte(end+i));
				}
			}
			else if (diff < 0) {
				for (int i = 0; i < length-end; ++i) {
					in.setByte(end+diff+i, in.getByte(end+i));
				}
			}
			for (int i = 0; i < value.length; ++i) {
				in.setByte(start+i, value[i]);
			}
			in.writerIndex(length+diff);
			return in;
		}
		else {
			ByteBuf out = in.alloc().buffer(length+diff);
			for (int i = 0; i < start; ++i) {
				out.writeByte(in.getByte(i));
			}
			out.writeBytes(value);
			for (int i = end; i < length; ++i) {
				out.writeByte(in.getByte(i));
			}
			ReferenceCountUtil.release(in);
			return out;
		}
	}

	static boolean isSpace(byte c)
	{
		return c == ' ' || c == '\t' || c == '\r';
	}

	static String remappedHost(Config config, String host, String port)
	{
		String remapped = Optional.ofNullable(Optional.ofNullable(config.getRemapHosts()).orElse(Collections.emptyMap()).get(host+":"+port))
			.orElseGet(() -> Optional.ofNullable(Optional.ofNullable(config.getRemapHosts()).orElse(Collections.emptyMap()).get(host))
				.orElse(host)
			);
		int colon = remapped.lastIndexOf(':');
		return colon < 0 ? remapped+":"+port : remapped;
	}

	static InetSocketAddress getServerAddress(Config config, String hostWithPort)
	{
		int colon = hostWithPort.lastIndexOf(':');
		InetSocketAddress address = InetSocketAddress.createUnresolved(hostWithPort.substring(0, colon), Integer.parseInt(hostWithPort.substring(colon+1)));
		return address;
	}

	@RequiredArgsConstructor
	public class RequestReaderHandler extends ChannelInboundHandlerAdapter
	{
		private final Config config;

		private final CompletableFuture<Void> finishPromise;

		private ByteBuf header = Unpooled.buffer(512, 32768);

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception
		{
			((DuplexChannelConfig) ctx.channel().config()).setAutoRead(false);
			((DuplexChannelConfig) ctx.channel().config()).setAllowHalfClosure(true);
			NettyFutures.copy(ctx.channel().closeFuture(), finishPromise);
			super.handlerAdded(ctx);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception
		{
			ctx.read();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				if (!processHeader(ctx, (ByteBuf) msg)) {
					if (header.readableBytes() >= header.maxCapacity()) {
						respondAndClose(ctx, "400 Bad Request", new IOException("Failed to read HTTP request headers, exceeded 32768"));
						return;
					}
					ctx.read();
				}
			}
			catch (Throwable ex) {
				respondAndClose(ctx, "500 Internal Server Error", new IOException("Internal error processing request", ex));
			}
			finally {
				ReferenceCountUtil.release(msg);
			}
		}

		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof ChannelInputShutdownEvent) {
				log.error("Unexpected EOF when reading HTTP request");
			}
		}

		private boolean processHeader(ChannelHandlerContext ctx, ByteBuf pending)
		{
			ContinuousByteBuf reader = new ContinuousByteBuf(header, pending);
			int endHeader = 0; // points after end of last header, before headers and body delimiter
			for (;;) {
				endHeader = findNextLine(reader, endHeader);
				if (endHeader < 0)
					return false;
				if (reader.get(endHeader) == '\n') {
					break;
				}
				else if (reader.get(endHeader) == '\r' && reader.get(endHeader+1) == '\n') {
					break;
				}
			}
			String stringHeader = header.toString(StandardCharsets.UTF_8);
			Matcher m = HOST_PATTERN.matcher(stringHeader);
			if (!m.find()) {
				respondAndClose(ctx, "400 Bad Request", new IOException("Missing host header or connect request"));
				return true;
			}
			final String remapped;
			InetSocketAddress remote;
			ByteBuf output;
			ByteBuf clientOutput;
			if (m.group(2) != null) {
				// CONNECT request
				remapped = remappedHost(config, m.group(2), Optional.ofNullable(m.group(3)).orElse("443"));
				output = Unpooled.EMPTY_BUFFER;
				clientOutput = Unpooled.wrappedBuffer((m.group(4)+" 200 OK\r\n\r\n").getBytes(StandardCharsets.UTF_8));
			}
			else {
				if (m.group(1).equalsIgnoreCase("connect")) {
					respondAndClose(ctx, "400 Bad Request", new IOException("CONNECT method not matching host[:port] HTTP/x.y"));
					return true;
				}
				String host = m.group(6) != null ? m.group(6) : m.group(8);
				String port = Optional.ofNullable(m.group(7) != null ? m.group(7) : m.group(9)).orElse("80");
				remapped = remappedHost(config, host, port);
				header = replaceHeaderValue(header, CONNECTION_HEADER, (old) -> CLOSE);
				header = replaceHeaderValue(header, PROXY_CONNECTION_HEADER, (old) -> old != null ? CLOSE : null);
				header = replaceHeaderValue(header, HOST_HEADER, (old) -> remapped.getBytes(StandardCharsets.UTF_8));
				for (Map.Entry<String, String> entry: Optional.ofNullable(config.getAddedHeaders()).orElse(Collections.emptyMap()).entrySet()) {
					header = replaceHeaderValue(header, entry.getKey().getBytes(StandardCharsets.UTF_8), (old) -> entry.getValue().getBytes(StandardCharsets.UTF_8));
				}
				header = replaceHttpMethodHost(header, remapped.getBytes(StandardCharsets.UTF_8));
				output = header;
				clientOutput = Unpooled.EMPTY_BUFFER;
			}
			final ByteBuf serverOutput;
			if (pending.isReadable()) {
				ReferenceCountUtil.retain(serverOutput = pending);
			}
			else {
				serverOutput = Unpooled.EMPTY_BUFFER;
			}
			remote = getServerAddress(config, remapped);
			ctx.channel().config().setAutoRead(false);
			ctx.pipeline().remove(RequestReaderHandler.class);
			resolveServer(ctx.channel(), remote)
				.thenCompose((address) ->
					nettyEngine.connect(null, address, new ChannelInitializer<Channel>() {
						@Override
						protected void initChannel(Channel server) throws Exception
						{
							server.config().setAutoRead(false);
							((DuplexChannelConfig) server.config()).setAllowHalfClosure(true);
							server.pipeline().addLast(new FullFlowControlHandler());
						}
					})
				)
					.whenComplete((server, ex) -> {
						if (ex != null) {
							ReferenceCountUtil.release(serverOutput);
							respondAndClose(ctx, "503 Cannot connect", new IOException("Failed to connect to "+remote+" : "+ex.getMessage(), ex));
						}
						else {
							DuplexChannel client = (DuplexChannel) ctx.channel();
							server.write(output);
							ChannelFuture serverFuture = server.writeAndFlush(serverOutput);
							ChannelFuture clientFuture = client.writeAndFlush(clientOutput);
							NettyFutures.join(serverFuture, clientFuture)
								.thenCompose((v) ->
									nettyEngine.forwardDuplex(client, server)
								)
								.whenComplete((v, ex2) -> {
										NettyFutures.join(client.close(), server.close())
											.whenComplete((v3, ex3) -> {
												FutureUtil.completeOrFail(finishPromise, v, ex2);
											});
									}
								);
						}
					});
			return true;
		}

		private void respondAndClose(ChannelHandlerContext ctx, String status, Exception response)
		{
			try {
				DuplexChannel channel = (DuplexChannel)ctx.channel();
				ByteBuf output = Unpooled.buffer();
				output.writeCharSequence("HTTP/1.0 "+status+"\r\ncontent-type: text/plain\r\nconnection: close\r\n", StandardCharsets.UTF_8);
				byte[] content = response.toString().getBytes(StandardCharsets.UTF_8);
				output.writeCharSequence("content-length: "+(content.length+1)+"\r\n\r\n", StandardCharsets.UTF_8);
				output.writeBytes(content);
				output.writeByte('\n');
				nettyEngine.writeAndClose(channel, output)
					.whenComplete((v, ex) ->
						finishPromise.completeExceptionally(new IOException(response))
					);
			}
			catch (Throwable ex) {
				finishPromise.completeExceptionally(ex);
			}
		}
	}

	@RequiredArgsConstructor
	private static class ContinuousByteBuf
	{
		private final ByteBuf content;

		private final ByteBuf source;

		public int get(int position)
		{
			while (position >= content.writerIndex()) {
				try {
					content.writeByte(source.readByte());
				}
				catch (IndexOutOfBoundsException ex) {
					return -1;
				}
			}
			return content.getByte(position);
		}
	}
}
