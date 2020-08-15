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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple HTTP proxy implementation, old school thread fork based.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HttpProxyFactory
{
	private static final Pattern HOST_PATTERN =
		Pattern.compile("^(?:\\w+\\s+http://([^\\s/:]+)(?::(\\d+))?(?:/|\\s))|(?:host:\\s*(\\S+?)(?::(\\d+))?\\s*\\r?\\n)|(?:connect\\s*(\\S+?)(?::(\\d+))?\\s+(\\S+)\\s*\\r?\\n)", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
	private static final byte[] NL_CONNECTION_COLON = "\nConnection:".getBytes(StandardCharsets.UTF_8);
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
			var future = new CompletableFuture<Void>()
			{
				@Override
				public synchronized boolean cancel(boolean interrupt)
				{
					if (!super.cancel(interrupt))
						return false;
					portForwarder.close(this, listener);
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
			return future.whenComplete((v, ex) -> portForwarder.close(null, listener));
		});
	}

	private CompletableFuture<Void> runServer(AsynchronousSocketChannel client)
	{
		var future = new CompletableFuture<Void>() {
			private AsynchronousSocketChannel server;

			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				portForwarder.close(this, client);
				portForwarder.close(this, server);
				return true;
			}
		};
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
							if (header.position() == 16384) {
								throw new IOException("HTTP request headers exceed max value: "+header.position());
							}
							header = growByteBuffer(header, header.position()*2);
						}
						client.read(header, 0, this);
					}
					catch (Throwable ex) {
						future.completeExceptionally(ex);
					}
				}

				@Override
				public void failed(Throwable exc, Integer attachment)
				{
					future.completeExceptionally(exc);
				}

				private boolean processHeader() throws IOException
				{
					int endHeader = 0; // points after end of last header, before headers and body delimiter
					for (; ; ) {
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
							.whenComplete((v, ex) -> future.completeExceptionally(new IOException("Invalid HTTP request, no host header or connect request")));
						return true;
					}
					InetSocketAddress remote;
					ByteBuffer output;
					ByteBuffer clientOutput;
					if (m.group(5) != null) {
						remote = new InetSocketAddress(m.group(5), m.group(6) != null ? Integer.parseInt(m.group(6)) : 443);
						int added = header.get(endHeader) == '\r' ? 2 : 1;
						output = header;
						output.flip();
						output.position(endHeader+added);
						clientOutput = ByteBuffer.wrap((m.group(7)+" 200 OK\r\n\r\n").getBytes(StandardCharsets.UTF_8));
					}
					else {
						String host = m.group(1) != null ? m.group(1) : m.group(3);
						String port = Objects.requireNonNullElse(m.group(2) != null ? m.group(2) : m.group(4), "80");
						remote = new InetSocketAddress(host, Integer.parseInt(port));
						output = ByteBuffer.allocate(header.position()+32); // enough for connection: close\r\n
						int rest = 0;
						int connectionHeader = indexOfIgnoreCase(header, NL_CONNECTION_COLON, 0);
						if (connectionHeader >= 0 && connectionHeader < endHeader) {
							int nl = findNextLine(header, ++connectionHeader);
							output.put(header.array(), 0, connectionHeader);
							rest = nl;
						}
						output.put(header.array(), rest, endHeader-rest);
						output.put(CONNECTION_CLOSE);
						output.put(header.array(), endHeader, header.position()-endHeader);
						output.flip();
						clientOutput = ByteBuffer.allocate(0);
					}
					portForwarder.connect(remote, (server) -> future.server = server, new CompletionHandler<>()
						{
							@Override
							public void completed(Void result, Integer attachment)
							{
								portForwarder.writeFully(client, clientOutput)
									.thenCompose((v) -> portForwarder.writeFully(future.server, output))
									.thenCompose((v) -> portForwarder.runBothForward(client, future.server))
									.whenComplete((v, ex) -> FutureUtil.completeOrFail(future, v, ex));
							}

							@Override
							public void failed(Throwable exc, Integer attachment)
							{
								future.completeExceptionally(new IOException("Failed to connect to: "+remote, exc));
							}
						}
					);
					return true;
				}
			});
		}
		catch (Throwable ex) {
			future.completeExceptionally(ex);
		}
		return future
			.whenComplete((v, ex) -> {
				portForwarder.close(null, client);
				portForwarder.close(null, future.server);
			});
	}

	private int indexOfIgnoreCase(ByteBuffer buffer, byte[] needle, int offset) {
		if (needle.length == 0)
			return 0;
		int length = buffer.position();
		OUT: for (int i = offset; i < length-needle.length; ++i) {
			for (int j = 0; j < needle.length; ++j) {
				if (Ascii.toLowerCase((char) buffer.get(i+j)) != Ascii.toLowerCase((char) needle[j]))
					continue OUT;
			}
			return i;
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
