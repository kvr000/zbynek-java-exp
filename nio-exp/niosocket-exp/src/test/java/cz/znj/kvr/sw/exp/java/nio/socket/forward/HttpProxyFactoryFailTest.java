package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import com.google.common.collect.ImmutableList;
import net.dryuf.concurrent.FutureUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


/**
 * HttpProxyFactory failure simulator.
 */
public class HttpProxyFactoryFailTest
{
	private HttpProxyFactory httpProxyFactory = new HttpProxyFactory(new PortForwarder(Executors.newCachedThreadPool()));

	@Test(expected = UncheckedIOException.class, timeout = 1000L)
	public void runProxy_doubleBind_fail() throws Throwable
	{
		try {
			CompletableFuture<Void> one = httpProxyFactory.runProxy(
				HttpProxyFactory.Config.builder()
					.listenAddress(new InetSocketAddress(46666))
					.build()
				);
			CompletableFuture<Void> two = httpProxyFactory.runProxy(
				HttpProxyFactory.Config.builder()
					.listenAddress(new InetSocketAddress(46666))
					.build()
			);
			FutureUtil.anyAndCancel(ImmutableList.of(one, two)).get();
		}
		catch (ExecutionException ex) {
			throw ex.getCause();
		}
	}

	@Test(expected = CancellationException.class, timeout = 1000L)
	public void runProxy_cancel_kills() throws Throwable
	{
		try {
			CompletableFuture<Void> one = httpProxyFactory.runProxy(
				HttpProxyFactory.Config.builder()
					.listenAddress(new InetSocketAddress(16666))
					.build()
			);
			new Socket("localhost", 16666).close();
			one.cancel(true);
			for (;;) {
				try {
					new Socket("localhost", 16666).close();
					Thread.sleep(10);
					continue;
				}
				catch (IOException ex) {
					break;
				}
			}
			one.get();
		}
		catch (ExecutionException ex) {
			throw ex.getCause();
		}
	}
}
