package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpserver.DummyHttpServerFactoryEndTest;
import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.test.ClientServerTester;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.testng.Assert.assertEquals;


@Log4j2
public class NettyHttpProxyFactoryEndTest
{
	@Test(timeOut = 30_000L)
	public void testHttpProxy() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = DummyHttpServerFactoryEndTest.runHttpServer(tester);
			InetSocketAddress proxyAddress = runHttpProxy(tester);

			String hostname = serverAddress.getHostString() + ":" + serverAddress.getPort();
			DummyHttpServerFactoryEndTest.runHttpClient(tester, hostname, 1, proxyAddress);
		}
	}

	public static InetSocketAddress runHttpProxy(ClientServerTester tester)
	{
		return runHttpProxy(tester, InetSocketAddress.createUnresolved("localhost", 0));
	}

	public static <T extends SocketAddress> T runHttpProxy(ClientServerTester tester, T listenAddress)
	{
		Server server = new NettyHttpProxyFactory(tester.nettyEngine()).runProxy(
			HttpProxyFactory.Config.builder()
				.listenAddress(AddressSpec.fromSocketAddress(listenAddress))
				.build()
		).join();
		tester.addServer(server);
		@SuppressWarnings("unchecked")
		T address = (T) server.listenAddress();
		log.info("HttpProxy listening: {}", address);
		return address;
	}
}
