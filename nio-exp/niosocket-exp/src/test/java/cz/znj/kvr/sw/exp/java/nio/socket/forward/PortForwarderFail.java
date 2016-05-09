package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * PortForwarder failure simulator.
 */
public class PortForwarderFail
{
	@Test(expected = IOException.class)
	public void runForwards_doubleBind_fail() throws Throwable
	{
		try {
			new PortForwarder()
				.runForwards(ImmutableList.of(
					PortForwarder.ForwardConfig.builder()
						.bindProto("tcp4")
						.bindHost("localhost")
						.bindPort(7777)
						.connectProto("tcp4")
						.connectHost("localhost")
						.connectPort(4444)
						.build(),
					PortForwarder.ForwardConfig.builder()
						.bindProto("tcp4")
						.bindHost("localhost")
						.bindPort(7777)
						.connectProto("tcp4")
						.connectHost("localhost")
						.connectPort(4444)
						.build()
				)).get();
		}
		catch (ExecutionException ex) {
			throw ex.getCause();
		}
	}
}
