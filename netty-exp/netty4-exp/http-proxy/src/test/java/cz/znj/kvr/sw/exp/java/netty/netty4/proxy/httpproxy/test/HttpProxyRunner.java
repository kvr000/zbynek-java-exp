package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy.test;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy.NettyHttpProxyFactory;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyEngine;


/**
 * Proxy runner
 */
public class HttpProxyRunner
{
	public static void main(String[] args) throws Exception
	{
		try (NettyEngine nettyEngine = new NettyEngine()) {
			new NettyHttpProxyFactory(nettyEngine)
				.runProxy(
					HttpProxyFactory.Config.builder()
						.listenAddress(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(4444)
							.build()
						)
						.build()
				).get().closedFuture().get();
		}
	}
}
