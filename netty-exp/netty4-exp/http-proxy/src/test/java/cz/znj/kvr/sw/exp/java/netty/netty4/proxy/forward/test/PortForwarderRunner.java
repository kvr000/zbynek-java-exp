package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.test;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.NettyPortForwarderFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.PortForwarderFactory;


/**
 * Proxy runner
 */
public class PortForwarderRunner
{
	public static void main(String[] args) throws Exception
	{
		try (NettyEngine nettyEngine = new NettyEngine()) {
			Server.waitOneAndClose(NettyFutures.nestedAllOrCancel(new NettyPortForwarderFactory(nettyEngine)
				.runForwards(ImmutableList.of(
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3300)
							.build())
						.connect(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.build(),
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.connect(AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.build(),
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.connect(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3302)
							.build())
						.build()
				))).get()).get();
			throw new IllegalStateException("Unreachable");
		}
	}
}
