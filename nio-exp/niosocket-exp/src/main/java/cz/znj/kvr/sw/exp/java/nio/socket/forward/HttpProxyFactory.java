package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import cz.znj.kvr.sw.exp.java.nio.socket.util.FutureUtil;
import com.google.common.base.Ascii;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple HTTP proxy implementation, old school thread fork based.
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
	private static final byte[] CONNECTION_CLOSE = "connection: close\r\n".getBytes(StandardCharsets.UTF_8);

	private final PortForwarder portForwarder;

	public CompletableFuture<Void> runProxy(InetSocketAddress listenAddress) throws IOException
	{
		return portForwarder.runListener((AsynchronousServerSocketChannel listener) -> {
			try {
				listener.bind(listenAddress);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to bind to: "+listenAddress, e);
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
			};

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
							runServer(client)
								.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
						}

						@Override
						public void failed(Throwable exc, Integer attachment)
						{
							future.completeExceptionally(exc);
						}
					}
				);
			}
			catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
			return future.whenComplete((v, ex) -> portForwarder.closeChannel(null, listener));
		});
	}

	CompletableFuture<Void> runServer(AsynchronousSocketChannel client)
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
							InetSocketAddress remote;
							ByteBuffer output;
							ByteBuffer clientOutput;
							if (m.group(2) != null) {
								remote = new InetSocketAddress(m.group(2), m.group(3) != null ? Integer.parseInt(m.group(3)) : 443);
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
								remote = new InetSocketAddress(host, Integer.parseInt(port));
								output = ByteBuffer.allocate(header.position()+32); // enough for connection: close\r\n
								int rest = 0;
								int connectionHeader = findHeader(header, CONNECTION_HEADER, 0);
								if (connectionHeader >= 0 && connectionHeader < endHeader) {
									int nl = findNextLine(header, connectionHeader);
									output.put(header.array(), 0, connectionHeader);
									rest = nl;
								}
								output.put(header.array(), rest, endHeader-rest);
								output.put(CONNECTION_CLOSE);
								output.put(header.array(), endHeader, header.position()-endHeader);
								output.flip();
								clientOutput = ByteBuffer.allocate(0);
							}
							portForwarder.connect(remote, (serverArg) -> server = serverArg, new CompletionHandler<Void, Integer>()
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

	private int findHeader(ByteBuffer buffer, byte[] needle, int offset) {
		if (needle.length == 0)
			return 0;
		int length = buffer.position();
		OUT: for (int i = offset; i < length-needle.length; ++i) {
			if (buffer.get(i) != '\n')
				continue;
			for (int j = 0; j < needle.length; ++j) {
				if (Ascii.toLowerCase((char) buffer.get(i+1+j)) != Ascii.toLowerCase((char) needle[j]))
					continue OUT;
			}
			for (int k = i+1+needle.length; k < length; ++k) {
				byte c = buffer.get(k);
				if (c == ' ' || c == '\t')
					continue;
				if (c != ':')
					continue OUT;
				for (++k; k < length; k++) {
					if (buffer.get(k) == '\n')
						return i+1;
				}
				// no new line, no need to search for the rest
				return -1;
			}
		}
		return -1;
	}

	private int findNextLine(ByteBuffer buffer, int offset) {
		int length = buffer.position();
		for (int i = offset; i < length; ++i) {
			if (buffer.get(i) == '\n')
				return i+1;
		}
		return -1;
	}

	private ByteBuffer growByteBuffer(ByteBuffer in, int newSize)
	{
		ByteBuffer copy = ByteBuffer.allocate(newSize);
		in.flip();
		copy.put(in);
		return copy;
	}
}
