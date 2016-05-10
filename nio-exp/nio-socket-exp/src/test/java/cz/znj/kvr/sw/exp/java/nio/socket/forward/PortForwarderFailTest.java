package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


/**
 * PortForwarder failure simulator.
 */
public class PortForwarderFailTest
{
	@Test(expected = UncheckedIOException.class, timeout = 1000L)
	public void runForwards_doubleBind_fail() throws Throwable
	{
		try {
			new PortForwarder(Executors.newCachedThreadPool())
				.runForwards(ImmutableList.of(
					PortForwarder.ForwardConfig.builder()
						.bindProto("tcp4")
						.bindHost("localhost")
						.bindPort(46667)
						.connectProto("tcp4")
						.connectHost("localhost")
						.connectPort(4444)
						.build(),
					PortForwarder.ForwardConfig.builder()
						.bindProto("tcp4")
						.bindHost("localhost")
						.bindPort(46667)
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
