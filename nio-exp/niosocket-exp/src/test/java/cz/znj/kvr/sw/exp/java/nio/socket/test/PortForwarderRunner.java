package cz.znj.kvr.sw.exp.java.nio.socket.test;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.nio.socket.forward.PortForwarder;


/**
 * Proxy runner
 */
public class PortForwarderRunner
{
	public static void main(String[] args) throws Exception
	{
		new PortForwarder()
			.runForwards(ImmutableList.of(
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
