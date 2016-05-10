package cz.znj.kvr.sw.exp.java.nio.socket.test;

import net.dryuf.concurrent.FutureUtil;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;


/**
 * Simple HTTP server for testing.
 */
public class TestingHttpClient
{
	byte[] requestBytes = "GET / HTTP/1.0\r\nHost:localhost:9999\r\nconnection:close\r\n\r\n".getBytes(StandardCharsets.UTF_8);

	InetSocketAddress serverAddress = new InetSocketAddress("localhost", 4444);

	AtomicLong nextStats = new AtomicLong(System.currentTimeMillis());

	long lastPerfCount = 0;
	AtomicLong perfCount = new AtomicLong();

	long endTime = nextStats.get()+60_000;

	public static void main(String[] args) throws Exception
	{
		new TestingHttpClient().run(args);
	}

	private int run(String[] args) throws Exception
	{
		return execute();
	}

	private int execute() throws Exception
	{
		CompletableFuture<Void> future = CompletableFuture.allOf(IntStream.range(0, 256)
			.mapToObj(this::runStream)
			.toArray(s -> new CompletableFuture[s])
		);
		future.get();
		return 0;
	}

	private CompletableFuture<Void> runStream(int index)
	{
		return runOneRequest()
			.thenCompose(r -> finishedRequest(index));
	}

	private CompletableFuture<Void> finishedRequest(int index)
	{
		perfCount.incrementAndGet();
		long time = System.currentTimeMillis();
		long nextStatsValue = nextStats.get();
		if (time >= nextStatsValue && nextStats.compareAndSet(nextStatsValue, nextStatsValue+1_000)) {
			// well, we should have some better synchronization including above, but conflict very unlikely
			synchronized (this) {
				System.err.println("Request/s: "+(perfCount.get()-lastPerfCount));
				lastPerfCount = perfCount.get();
			}
		}
		if (time >= endTime) {
			return CompletableFuture.completedFuture(null);
		}
		return runStream(index);
	}

	private CompletableFuture<Void> runOneRequest()
	{
		try {
			CompletableFuture<Void> future = new CompletableFuture<>();
			AsynchronousSocketChannel server = AsynchronousSocketChannel.open();
			ByteBuffer requestData = ByteBuffer.wrap(requestBytes);
			server.connect(serverAddress, null, new CompletionHandler<Void, Object>()
				{
					@Override
					public void completed(Void result, Object attachment)
					{
						writeAndShutdown(server, requestData)
							.thenCompose((r) -> readFully(server))
							.whenComplete((r, ex) -> IOUtils.closeQuietly(server))
							.whenComplete((r, ex) -> {
								FutureUtil.completeOrFail(future, null, ex);
							});
					}

					@Override
					public void failed(Throwable exc, Object attachment)
					{
						IOUtils.closeQuietly(server);
						future.completeExceptionally(exc);
					}
				}
			);
			return future;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CompletableFuture<Void> writeFully(AsynchronousSocketChannel socket, ByteBuffer buffer)
	{
		if (!buffer.hasRemaining())
			return CompletableFuture.completedFuture(null);

		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				return true;
			}
		};
		try {
			socket.write(buffer, 0, new CompletionHandler<Integer, Integer>()
			{
				@Override
				public void completed(Integer result, Integer attachment)
				{
					if (!buffer.hasRemaining()) {
						future.complete(null);
					}
					else {
						try {
							socket.write(buffer, 0, this);
						}
						catch (Throwable ex) {
							future.completeExceptionally(ex);
						}
					}
				}

				@Override
				public void failed(Throwable exc, Integer attachment)
				{
					future.completeExceptionally(exc);
				}
			});
		}
		catch (Throwable ex) {
			future.completeExceptionally(ex);
		}
		return future;
	}

	private CompletableFuture<Void> writeAndShutdown(AsynchronousSocketChannel socket, ByteBuffer buffer)
	{
		return writeFully(socket, buffer)
			.whenComplete((v, ex) -> {
				if (ex == null) {
					try {
						socket.shutdownOutput();
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			});
	}

	private CompletableFuture<byte[]> readFully(AsynchronousSocketChannel socket)
	{
		try {
			CompletableFuture<byte[]> future = new CompletableFuture<byte[]>()
			{
				ByteBuffer buf = ByteBuffer.allocateDirect(4096);

				{
					socket.read(buf, null, new CompletionHandler<Integer, Object>() {
						@Override
						public void completed(Integer result, Object attachment)
						{
							if (result <= 0) {
								buf.flip();
								byte[] bytes = new byte[buf.limit()];
								buf.get(bytes);
								complete(bytes);
							}
							else {
								if (buf.position() == buf.capacity()) {
									buf = growByteBuffer(buf, buf.capacity()*2);
								}
								socket.read(buf, null, this);
							}
						}

						@Override
						public void failed(Throwable exc, Object attachment)
						{
							completeExceptionally(exc);
						}
					});
				}
			};
			return future;
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	static ByteBuffer growByteBuffer(ByteBuffer in, int newSize)
	{
		ByteBuffer copy = ByteBuffer.allocateDirect(newSize);
		in.flip();
		copy.put(in);
		return copy;
	}
}
