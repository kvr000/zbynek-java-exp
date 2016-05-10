package cz.znj.kvr.sw.exp.java.nio.socket.test;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;


/**
 * Test closed connection asynchronous read behavior.
 *
 * Does not work as expected, no way to distinguish between shutdown and connection reset by peer.
 */
public class AsyncSocketStreamPeerClosedTest
{
	@Test
	public void testPeerReset() throws Exception
	{
		CompletableFuture<Integer> future = new CompletableFuture<>();
		int port = createListener(future);

		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("localhost", port));
		socket.close();

		Assert.assertEquals(-1, (int) future.get()); // this returns -1
	}

	@Test
	public void testPeerShutdown() throws Exception
	{
		CompletableFuture<Integer> future = new CompletableFuture<>();
		int port = createListener(future);

		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("localhost", port));
		socket.shutdownOutput();
		socket.close();

		Assert.assertEquals(-1, (int) future.get()); // this returns -1 too
	}

	private int createListener(CompletableFuture<Integer> future) throws IOException
	{
		AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open();
		listener.bind(new InetSocketAddress("localhost", 0));
		int port = ((InetSocketAddress) listener.getLocalAddress()).getPort();
		listener.accept(0, new CompletionHandler<AsynchronousSocketChannel, Integer>()
		{
			@Override
			public void completed(AsynchronousSocketChannel client, Integer attachment)
			{
				client.read(ByteBuffer.allocate(16), 0, new CompletionHandler<Integer, Integer>()
				{
					@Override
					public void completed(Integer result, Integer attachment)
					{
						client.read(ByteBuffer.allocate(1), 0, new CompletionHandler<Integer, Integer>()
							{
								@Override
								public void completed(Integer result,
										      Integer attachment)
								{
									future.complete(result);
								}

								@Override
								public void failed(Throwable exc, Integer attachment)
								{
									future.completeExceptionally(exc);
								}
							}
						);
					}

					@Override
					public void failed(Throwable exc, Integer attachment)
					{
						future.completeExceptionally(exc);
					}
				});
			}

			@Override
			public void failed(Throwable exc, Integer attachment)
			{
				future.completeExceptionally(exc);
			}
		});
		return port;
	}
}
