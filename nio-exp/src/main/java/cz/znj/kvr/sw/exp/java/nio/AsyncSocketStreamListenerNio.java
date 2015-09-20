package cz.znj.kvr.sw.exp.java.nio;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
public class AsyncSocketStreamListenerNio
{
	public static void		main(String[] args) throws Exception
	{
		System.exit(new AsyncSocketStreamListenerNio().run(args));
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
		final ByteBuffer buffer = ByteBuffer.allocate(4096);
		client.read(buffer, 5, TimeUnit.SECONDS, buffer, new CompletionHandler<Integer, ByteBuffer >() {
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
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
					byte[] packet = new byte[buffer.position()];
					System.arraycopy(buffer.array(), 0, packet, 0, packet.length);
					System.out.println("Got data from client ("+result+"): "+new String(packet));
					if (buffer.remaining() == 0)
						throw new IllegalArgumentException("Ran out of buffer");
					client.read(buffer, 120, TimeUnit.SECONDS, buffer, this);
				}
				catch (RuntimeException ex) {
					failed(ex, buffer);
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer buffer) {
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
			System.out.println("started read on client "+client.getRemoteAddress().toString());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
