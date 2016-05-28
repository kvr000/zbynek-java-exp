package cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.test;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.NettyHttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.NettyPortForwarder;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.PortForwarder;

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
						.listenAddress(InetSocketAddress.createUnresolved("localhost", 4400))
						.build()
				).get().get();
		}
	}
}
