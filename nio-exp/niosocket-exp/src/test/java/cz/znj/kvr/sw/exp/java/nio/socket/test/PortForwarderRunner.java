package cz.znj.kvr.sw.exp.java.nio.socket.test;

import cz.znj.kvr.sw.exp.java.nio.socket.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.nio.socket.forward.PortForwarder;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * Proxy runner
 */
public class PortForwarderRunner
{
	public static void main(String[] args) throws Exception
	{
		new PortForwarder()
			.runForwards(List.of(
				PortForwarder.ForwardConfig.builder()
					.bindProto("tcp4")
					.bindHost("localhost")
					.bindPort(3333)
					.connectProto("tcp4")
					.connectHost("localhost")
					.connectPort(4444)
					.build()
			)).get();
	}
}
