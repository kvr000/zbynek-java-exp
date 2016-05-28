package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.test;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.NettyPortForwarder;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.PortForwarder;


/**
 * Proxy runner
 */
public class PortForwarderRunner
{
	public static void main(String[] args) throws Exception
	{
		try (NettyRuntime nettyRuntime = new NettyRuntime()) {
			new NettyPortForwarder(nettyRuntime)
				.runForwards(ImmutableList.of(
					PortForwarder.ForwardConfig.builder()
						.bind(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3300)
							.build())
						.connect(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.build(),
					PortForwarder.ForwardConfig.builder()
						.bind(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.connect(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.build(),
					PortForwarder.ForwardConfig.builder()
						.bind(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.connect(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3302)
							.build())
						.build()
				)).get().get();
			throw new IllegalStateException("Unreachable");
		}
	}
}
