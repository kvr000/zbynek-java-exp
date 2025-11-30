package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import com.google.common.base.Ascii;
import com.google.common.primitives.Bytes;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.base.function.ThrowingFunction;

import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple HTTP proxy implementation.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HttpProxyFactory
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
	private static final byte[] HOST_HEADER = "host".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CLOSE = "close".getBytes(StandardCharsets.UTF_8);

	private final PortForwarder portForwarder;

	public CompletableFuture<Void> runProxy(Config config) throws IOException
	{
		return portForwarder.runListener((AsynchronousServerSocketChannel listener) -> {
			try {
				listener.bind(config.listenAddress);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to bind to: "+config.listenAddress, e);
			}
			CompletableFuture<Void> future = new CompletableFuture<Void>()
			{
				@Override
				public synchronized boolean cancel(boolean interrupt)
				{
					if (!super.cancel(interrupt))
						return false;
					portForwarder.closeChannel(this, listener);
					return true;
				}

				private void fail(Throwable ex)
				{
					completeExceptionally(ex);
					portForwarder.closeChannel(null, listener);
				}

				public CompletableFuture<Void> initialize()
				{
					try {
						listener.accept(
							0,
							new CompletionHandler<AsynchronousSocketChannel, Integer>()
							{
								@Override
								public void completed(AsynchronousSocketChannel client, Integer attachment)
								{
									portForwarder.createdChannel(client);
									listener.accept(0, this);
									runServer(client, config)
										.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
								}

								@Override
								public void failed(Throwable exc, Integer attachment)
								{
									fail(new IOException("Failed to accept on: "+config.listenAddress, exc));
								}
							}
						);
					}
					catch (Throwable ex) {
						fail(new IOException("Failed to accept on: "+config.listenAddress, ex));
					}
					return this;
				}
			}.initialize();

			return future;
		});
	}

	CompletableFuture<Void> runServer(AsynchronousSocketChannel client, Config config)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			private AsynchronousSocketChannel server;

			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				portForwarder.closeChannel(this, client);
				portForwarder.closeChannel(this, server);
				return true;
			}

			public CompletableFuture<Void> initialize()
			{
				CompletableFuture<Void> future0 = this;
				ByteBuffer header0 = ByteBuffer.allocate(2048);
				try {
					client.read(header0, 0, new CompletionHandler<Integer, Integer>()
					{
						private ByteBuffer header = header0;

						@Override
						public void completed(Integer result, Integer attachment)
						{
							try {
								if (processHeader())
									return;
								if (result <= 0)
									throw new IOException("Failed to read HTTP request headers");
								if (header.position() == header.capacity()) {
									if (header.position() >= 32768) {
										throw new IOException("HTTP request headers exceed max value: "+header.position());
									}
									header = growByteBuffer(header, header.position()*2);
								}
								client.read(header, 0, this);
							}
							catch (Throwable ex) {
								completeExceptionally(ex);
							}
						}

						@Override
						public void failed(Throwable exc, Integer attachment)
						{
							completeExceptionally(exc);
						}

						private boolean processHeader() throws IOException
						{
							int endHeader = 0; // points after end of last header, before headers and body delimiter
							for (;;) {
								endHeader = findNextLine(header, endHeader);
								if (endHeader < 0)
									return false;
								if (endHeader < header.position() && header.get(endHeader) == '\n') {
									break;
								}
								else if (endHeader+1 < header.position() && header.get(endHeader) == '\r' && header.get(endHeader+1) == '\n') {
									break;
								}
							}
							Matcher m = HOST_PATTERN.matcher(new String(header.array(), 0, endHeader, StandardCharsets.UTF_8));
							if (!m.find()) {
								portForwarder.writeAndShutdown(client, ByteBuffer.wrap("HTTP/1.0 400 Bad Request\r\nConnection: close\r\n\r\nMissing host header or connect request\r\n".getBytes(StandardCharsets.UTF_8)))
									.whenComplete((v, ex) -> completeExceptionally(new IOException("Invalid HTTP request, no host header or connect request")));
								return true;
							}
							final String remapped;
							InetSocketAddress remote;
							ByteBuffer output;
							ByteBuffer clientOutput;
							if (m.group(2) != null) {
								remapped = remappedHost(config, m.group(2), Optional.ofNullable(m.group(3)).orElse("443"));
								int added = header.get(endHeader) == '\r' ? 2 : 1;
								output = header;
								output.flip();
								output.position(endHeader+added);
								clientOutput = ByteBuffer.wrap((m.group(4)+" 200 OK\r\n\r\n").getBytes(StandardCharsets.UTF_8));
							}
							else {
								if (m.group(1).equalsIgnoreCase("connect")) {
									portForwarder.writeAndShutdown(client, ByteBuffer.wrap("HTTP/1.0 400 Bad Request\r\ncontent-type: text/plain\r\nconnection: close\r\n\r\nCONNECT method not matching host[:port] HTTP/x.y\r\n".getBytes(StandardCharsets.UTF_8)))
										.whenComplete((v, ex) -> completeExceptionally(new IOException("Invalid HTTP request, CONNECT method not matching host[:port] HTTP/x.y")));
									return true;
								}
								String host = m.group(6) != null ? m.group(6) : m.group(8);
								String port = Optional.ofNullable(m.group(7) != null ? m.group(7) : m.group(9)).orElse("80");
								remapped = remappedHost(config, host, port);
								int oldLength = header.position();
								header = replaceHeaderValue(header, 0, endHeader, CONNECTION_HEADER, (old) -> CLOSE);
								endHeader += header.position()-oldLength;
								oldLength = header.position();
								header = replaceHeaderValue(header, 0, endHeader, HOST_HEADER, (old) -> remapped.getBytes(StandardCharsets.UTF_8));
								endHeader += header.position()-oldLength;
								for (Map.Entry<String, String> entry: Optional.ofNullable(config.addedHeaders).orElse(Collections.emptyMap()).entrySet()) {
									oldLength = header.position();
									header = replaceHeaderValue(header, 0, endHeader, entry.getKey().getBytes(StandardCharsets.UTF_8), (old) -> entry.getValue().getBytes(StandardCharsets.UTF_8));
									endHeader += header.position()-oldLength;
								}
								header = replaceHttpMethodHost(header, endHeader, remapped.getBytes(StandardCharsets.UTF_8));
								header.flip();
								output = header;
								clientOutput = ByteBuffer.allocate(0);
							}
							remote = getServerAddress(config, remapped);
							portForwarder.connect(
								(remote0) -> resolveServer(client, remote0),
								remote,
								(serverArg) -> server = serverArg,
								new CompletionHandler<Void, Integer>()
								{
									@Override
									public void completed(Void result, Integer attachment)
									{
										portForwarder.writeFully(client, clientOutput)
											.thenCompose((v) -> portForwarder.writeFully(server, output))
											.thenCompose((v) -> portForwarder.runBothForward(client, server))
											.whenComplete((v, ex) -> FutureUtil.completeOrFail(future0, v, ex));
									}

									@Override
									public void failed(Throwable exc, Integer attachment)
									{
										ByteBuffer response = ByteBuffer.wrap(("HTTP/1.0 503 Cannot connect\r\ncontent-type: text/plain\r\nconnection: close\r\n\r\nFailed to connect to "+remote+" : "+exc.getMessage()+"\n").getBytes(StandardCharsets.UTF_8));
										portForwarder.writeAndShutdown(client, response);
										completeExceptionally(new IOException("Failed to connect to: "+remote, exc));
									}
								}
							);
							return true;
						}
					});
				}
				catch (Throwable ex) {
					this.completeExceptionally(ex);
				}
				return this
					.whenComplete((v, ex) -> {
						portForwarder.closeChannel(null, client);
						portForwarder.closeChannel(null, server);
					});
			}
		}.initialize();
		return future;
	}

	CompletableFuture<SocketAddress> resolveServer(AsynchronousSocketChannel client, SocketAddress unresolved)
	{
		return portForwarder.resolve(unresolved)
			.thenApply(ThrowingFunction.sneaky((v) -> {
				if (v.equals(client.getLocalAddress())) {
					throw new IOException("Request connecting to same proxy");
				}
				return v;
			}));
	}

	static int findHeader(ByteBuffer buffer, byte[] needle, int start, int end) {
		if (needle.length == 0)
			return 0;
		OUT: for (int i = start; i < end-needle.length; ++i) {
			if (buffer.get(i) != '\n')
				continue;
			int s = i+1;
			for (; s < end; ++s) {
				byte c = buffer.get(s);
				if (c != ' ' && c != '\t')
					break;
			}
			if (s+needle.length > end)
				return -1;
			for (int j = 0; j < needle.length; ++j) {
				if (Ascii.toLowerCase((char) buffer.get(s+j)) != Ascii.toLowerCase((char) needle[j]))
					continue OUT;
			}
			for (int k = s+needle.length; k < end; ++k) {
				byte c = buffer.get(k);
				if (c == ' ' || c == '\t')
					continue;
				if (c != ':')
					continue OUT;
				for (++k; k < end; k++) {
					if (buffer.get(k) == '\n')
						return i+1;
				}
				// no new line, no need to search for the rest
				return -1;
			}
		}
		return -1;
	}

	static int findNextLine(ByteBuffer buffer, int offset) {
		int length = buffer.position();
		for (int i = offset; i < length; ++i) {
			if (buffer.get(i) == '\n')
				return i+1;
		}
		return -1;
	}

	static ByteBuffer growByteBuffer(ByteBuffer in, int newSize)
	{
		ByteBuffer copy = ByteBuffer.allocate(newSize);
		in.flip();
		copy.put(in);
		return copy;
	}

	static ByteBuffer replaceHeaderValue(ByteBuffer in, int start, int end, byte[] headerName, Function<byte[], byte[]> replacer)
	{
		int p = findHeader(in, headerName, start, end);
		if (p >= 0) {
			int valueStart;
			for (valueStart = p; in.get(valueStart) != ':'; ++valueStart) ;
			for (++valueStart; isSpace(in.get(valueStart)); ++valueStart) ;
			int lineEnd;
			for (lineEnd = valueStart; in.get(lineEnd) != '\n'; ++lineEnd) ;
			int valueEnd;
			for (valueEnd = lineEnd; isSpace(in.get(valueEnd-1)); --valueEnd) ;
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

	static ByteBuffer replaceHttpMethodHost(ByteBuffer in, int headerEnd, byte[] host)
	{
		int p;
		for (p = 0; isSpace(in.get(p)); ++p) ;
		for ( ; Character.isAlphabetic(in.get(p)); ++p) ;
		for ( ; isSpace(in.get(p)); ++p) ;
		if (in.get(p) == 'h' && in.get(p+1) == 't' && in.get(p+2) == 't' && in.get(p+3) == 'p' && in.get(p+4) == ':' && in.get(p+5) == '/' && in.get(p+6) == '/') {
			int e;
			for (e = p +=  7; !isSpace(in.get(e)) && in.get(e) != '/'; ++e) ;
			return replaceBuffer(in, p, e, host);
		}
		else {
			return in;
		}
	}

	static private byte[] copyBytes(ByteBuffer in, int start, int end)
	{
		byte[] bytes = new byte[end-start];
		for (int i = 0; i < end-start; ++i) {
			bytes[i] = in.get(start+i);
		}
		return bytes;
	}

	static ByteBuffer replaceBuffer(ByteBuffer in, int start, int end, byte[] value)
	{
		int length = in.position();
		int diff = value.length-(end-start);
		if (in.position()+diff <= in.capacity()) {
			if (diff > 0) {
				for (int i = length-end; --i >= 0; ) {
					in.put(end+diff+i, in.get(end+i));
				}
			}
			else if (diff < 0) {
				for (int i = 0; i < length-end; ++i) {
					in.put(end+diff+i, in.get(end+i));
				}
			}
			for (int i = 0; i < value.length; ++i) {
				in.put(start+i, value[i]);
			}
			in.position(length+diff);
			return in;
		}
		else {
			ByteBuffer out = ByteBuffer.allocate(length+diff);
			for (int i = 0; i < start; ++i) {
				out.put(in.get(i));
			}
			out.put(value);
			for (int i = end; i < length; ++i) {
				out.put(in.get(i));
			}
			return out;
		}
	}

	static boolean isSpace(byte c)
	{
		return c == ' ' || c == '\t' || c == '\r';
	}

	static String remappedHost(Config config, String host, String port)
	{
		String remapped = Optional.ofNullable(Optional.ofNullable(config.remapHosts).orElse(Collections.emptyMap()).get(host+":"+port))
			.orElseGet(() -> Optional.ofNullable(Optional.ofNullable(config.remapHosts).orElse(Collections.emptyMap()).get(host))
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

	@Builder
	@Value
	public static class Config
	{
		private final InetSocketAddress listenAddress;

		private final Map<String, String> remapHosts;

		private final Map<String, String> addedHeaders;
	}
}
