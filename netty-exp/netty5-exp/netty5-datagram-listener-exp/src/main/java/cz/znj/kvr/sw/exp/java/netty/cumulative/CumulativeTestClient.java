package cz.znj.kvr.sw.exp.java.netty.cumulative;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
public class CumulativeTestClient
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new CumulativeTestClient().run());
	}

	public                          CumulativeTestClient() throws Exception
	{
	}

	public int			run()
	{
		Stopwatch stopWatch = Stopwatch.createStarted();
		runClient(0);
		System.out.println("Time spent: "+stopWatch.elapsed(TimeUnit.MILLISECONDS)+" ms");
		return 0;
	}

	public void			runClient(int id)
	{
		try {
			SocketChannel ch = SocketChannel.open();
			ch.connect(serverAddress);

			ch.write(ByteBuffer.wrap(new byte[]{ '0' }));
			Thread.sleep(500);
			ch.write(ByteBuffer.wrap("123\n456\n".getBytes()));
			ByteBuffer input = ByteBuffer.allocate(1024);
			ch.read(input);
			input.flip();
			byte[] bytes = new byte[input.remaining()];
			input.get(bytes);
			System.out.println(new String(bytes));
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	InetSocketAddress		serverAddress = new InetSocketAddress(Inet4Address.getByName("localhost"), 4200);
}
