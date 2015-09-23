package cz.znj.kvr.sw.exp.java.netty;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created by rat on 2015-09-20.
 */
public class LoadDatagramClient
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new LoadDatagramClient().run());
	}

	public 				LoadDatagramClient() throws Exception
	{
	}

	public int			run()
	{
		Stopwatch stopWatch = Stopwatch.createStarted();
		LinkedList<Thread> threads = new LinkedList<>();
		for (int i = 0; i < 2; ++i) {
			int id = i;
			Thread t = new Thread(() -> {
				runClient(id);
			});
			t.start();
			threads.add(t);
		}
		for (Thread thread: threads) {
			try {
				thread.join();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("Time spent: "+stopWatch.elapsed(TimeUnit.MILLISECONDS)+" ms");
		return 0;
	}

	public void			runClient(int id)
	{
		try {
			DatagramChannel ch = DatagramChannel.open();
			ch.connect(serverAddress);
			for (int i = 0; i < 16; ++i) {
				ch.write(ByteBuffer.wrap(String.format("%d", id*1000000+i).getBytes()));
			}
			for (int i = 0; i < 16; ++i) {
				ch.read(ByteBuffer.allocate(1600));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	InetSocketAddress		serverAddress = new InetSocketAddress(Inet4Address.getByName("localhost"), 4200);
}
