package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.echo.EchoEndTest;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.test.ClientServerTester;
import lombok.extern.log4j.Log4j2;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


@Log4j2
public class PortForwarderFactoryEndTest
{
	@Test
	public void testForward() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = EchoEndTest.runEchoServer(tester);

			SocketAddress forward0Address = runForward(tester, serverAddress);

			EchoEndTest.runEchoClient(tester, forward0Address);
		}
	}

	@Test
	public void testLongForward() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = EchoEndTest.runEchoServer(tester);

			SocketAddress last = serverAddress;
			for (int i = 0; i < 10; ++i) {
				last = runForward(tester, last);
			}

			EchoEndTest.runEchoClient(tester, last);
		}
	}

	public static InetSocketAddress runForward(ClientServerTester tester, SocketAddress destination)
	{
		return runForward(tester, destination, InetSocketAddress.createUnresolved("localhost", 0));
	}

	public static <T extends SocketAddress> T runForward(ClientServerTester tester, SocketAddress destination, T source)
	{
		Server forward0 = new NettyPortForwarderFactory(tester.nettyEngine()).runForward(
			PortForwarderFactory.ForwardConfig.builder()
				.connect(AddressSpec.fromSocketAddress(destination))
				.bind(AddressSpec.fromSocketAddress(source))
				.build()
		).join();
		tester.addServer(forward0);

		@SuppressWarnings("unchecked")
		T address = (T) forward0.listenAddress();
		log.info("Forwarder listening: {}", address);
		return address;
	}
}
