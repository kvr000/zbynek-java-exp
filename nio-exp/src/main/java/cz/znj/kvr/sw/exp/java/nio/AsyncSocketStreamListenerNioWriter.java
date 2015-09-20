package cz.znj.kvr.sw.exp.java.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
public class AsyncSocketStreamListenerNioWriter
{
	public static void		main(String[] args) throws Exception
	{
		System.exit(new AsyncSocketStreamListenerNioWriter().run(args));
	}

	public int			run(String[] args)
	{
		final AsynchronousServerSocketChannel socket;
		try {
			socket = AsynchronousServerSocketChannel.open();
			socket.bind(new InetSocketAddress(3200));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		Thread listenThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				socket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>()
				{
					@Override
					public void completed(AsynchronousSocketChannel result, Object attachment)
					{
						socket.accept(null, this);
						startClient(result);
					}

					@Override
					public void failed(Throwable exc, Object attachment)
					{
						exc.printStackTrace();
					}
				});
				try {
					System.out.println("Started listening on "+socket.getLocalAddress().toString());
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		listenThread.start();

		for (;;) {
			try {
				Thread.sleep(100000000);
			}
			catch (InterruptedException e) {
				break;
			}
		}

		return 0;
	}

	public void			startClient(final AsynchronousSocketChannel client)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(200000000);
		buffer.limit(buffer.capacity());
		client.write(buffer, 5, TimeUnit.SECONDS, buffer, new CompletionHandler<Integer, ByteBuffer>()
		{
			@Override
			public void completed(Integer result, ByteBuffer buffer)
			{
				try {
					if (result < 0) {
						try {
							client.close();
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
						return;
					}
					if (buffer.remaining() > 0) {
						client.write(buffer, 5000, TimeUnit.MILLISECONDS, buffer, this);
					}
					else {
						try {
							client.shutdownOutput();
							client.close();
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				catch (RuntimeException ex) {
					failed(ex, buffer);
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer buffer)
			{
				exc.printStackTrace();
				try {
					client.close();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		try {
			System.out.println("started write on client "+client.getRemoteAddress().toString());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
