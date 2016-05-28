package cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.test;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.NettyPortForwarder;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.forward.PortForwarder;


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
							.port(3333)
							.build())
						.connect(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(2222)
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
							.port(4444)
							.build())
						.build(),
					PortForwarder.ForwardConfig.builder()
						.bind(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(2222)
							.build())
						.connect(PortForwarder.ForwardConfig.AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.build()
				)).get().get();
		}
	}
}
