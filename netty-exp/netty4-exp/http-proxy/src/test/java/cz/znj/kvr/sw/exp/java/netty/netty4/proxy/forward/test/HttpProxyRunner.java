package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.test;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.NettyHttpProxyFactory;

import java.net.InetSocketAddress;


/**
 * Proxy runner
 */
public class HttpProxyRunner
{
	public static void main(String[] args) throws Exception
	{
		try (NettyRuntime nettyRuntime = new NettyRuntime()) {
			new NettyHttpProxyFactory(nettyRuntime)
				.runProxy(
					HttpProxyFactory.Config.builder()
						.listenAddress(InetSocketAddress.createUnresolved("localhost", 4444))
						.build()
				).get().get();
		}
	}
}
