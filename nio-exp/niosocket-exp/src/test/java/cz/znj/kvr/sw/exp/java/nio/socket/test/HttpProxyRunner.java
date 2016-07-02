package cz.znj.kvr.sw.exp.java.nio.socket.test;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.nio.socket.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.nio.socket.forward.PortForwarder;

import java.net.InetSocketAddress;


/**
 * Proxy runner
 */
public class HttpProxyRunner
{
	public static void main(String[] args) throws Exception
	{
		new HttpProxyFactory(new PortForwarder())
			.runProxy(
				HttpProxyFactory.Config.builder()
					.listenAddress(new InetSocketAddress("localhost", 4444))
					.remapHosts(ImmutableMap.of("example.com", "www.w3.org"))
					.build()
			)
			.get();
	}
}
