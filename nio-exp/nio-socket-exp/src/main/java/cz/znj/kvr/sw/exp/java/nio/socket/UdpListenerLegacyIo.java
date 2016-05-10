package cz.znj.kvr.sw.exp.java.nio.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;


/**
 * Created by rat on 2015-09-20.
 */
public class UdpListenerLegacyIo
{
	public static void		main(String[] args) throws Exception
	{
		final DatagramSocket socket = new DatagramSocket(new InetSocketAddress(3200));
		Thread listenThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
				try {
					Thread.sleep(1000);
					socket.receive(packet);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		listenThread.start();
		Thread.sleep(1000);
		listenThread.interrupt();
		listenThread.join();
		System.exit(0);
	}
}
