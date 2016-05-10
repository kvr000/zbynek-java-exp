package cz.znj.kvr.sw.exp.java.nio.socket;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;


/**
 * Created by rat on 2015-09-20.
 */
public class UdpListenerNio
{
	public static void		main(String[] args) throws Exception
	{
		final DatagramChannel socket = DatagramChannel.open();
		socket.socket().bind(new InetSocketAddress(3200));
		final ByteBuffer packet = ByteBuffer.allocate(2048);
		final Thread listenThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				for (; ; ) {
					try {
						SocketAddress address = socket.receive(packet);
						byte[] bytes = new byte[packet.position()];
						System.arraycopy(packet.array(), 0, bytes, 0, packet.position());
						System.out.println("Got message from "+address+":\n"+ReflectionToStringBuilder.reflectionToString(bytes));
						packet.clear();
					}
					catch (ClosedByInterruptException ex) {
						System.out.println("Interrupted");
						return;
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		listenThread.start();
		//Thread.sleep(1000);
		listenThread.interrupt();
		listenThread.join();
		System.exit(0);
	}
}
