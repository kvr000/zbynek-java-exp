package cz.znj.kvr.sw.exp.java.corejava.lang.syntax;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Various tests for try-with-resources.
 */
public class TryWithResourcesTest
{
	@Test
	public void testClosedBeforeCatch()
	{
		AtomicBoolean closed = new AtomicBoolean(false);
		try (AutoCloseable closeable = () -> closed.set(true)) {
			throw new Exception("test");
		}
		catch (Exception e) {
			Assert.assertEquals(closed.get(), true);
		}
	}

	@Test(expectedExceptions = ConnectException.class)
	public void testClosedBeforeFinally() throws Exception
	{
		AtomicBoolean closed = new AtomicBoolean(false);
		try (AutoCloseable closeable = () -> closed.set(true)) {
			throw new ConnectException("test");
		}
		finally {
			Assert.assertEquals(closed.get(), true);
		}
	}

	@Test(expectedExceptions = ConnectException.class)
	public void testTryOrderUncaught() throws Exception
	{
		AtomicInteger state = new AtomicInteger(0);
		try {
			try (AutoCloseable closeable = () -> incrementState(state, 0)) {
				throw new ConnectException("test");
			}
			catch (IllegalAccessException ex) {
				Assert.fail("Should not reach this");
			}
			finally {
				incrementState(state, 1);
			}
		}
		finally {
			incrementState(state, 2);
		}
	}

	@Test
	public void testTryOrderCaught() throws Exception
	{
		AtomicInteger state = new AtomicInteger(0);
		try {
			try (AutoCloseable closeable = () -> incrementState(state, 0)) {
				throw new ConnectException("test");
			}
			catch (ConnectException ex) {
				incrementState(state, 1);
			}
			finally {
				incrementState(state, 2);
			}
		}
		finally {
			incrementState(state, 3);
		}
	}

	@Test(expectedExceptions = ConnectException.class)
	public void testTryOrderRethrown() throws Exception
	{
		AtomicInteger state = new AtomicInteger(0);
		try {
			try (AutoCloseable closeable = () -> incrementState(state, 0)) {
				throw new ConnectException("test");
			}
			catch (ConnectException ex) {
				incrementState(state, 1);
				throw ex;
			}
			finally {
				incrementState(state, 2);
			}
		}
		finally {
			incrementState(state, 3);
		}
	}

	@Test(expectedExceptions = ConnectException.class)
	public void testMultiTryOrderRethrown() throws Exception
	{
		AtomicInteger state = new AtomicInteger(0);
		try {
			try (
					AutoCloseable closeable = () -> incrementState(state, 1);
					AutoCloseable closeable1 = () -> incrementState(state, 0);
			) {
				throw new ConnectException("test");
			}
			catch (ConnectException ex) {
				incrementState(state, 2);
				throw ex;
			}
			finally {
				incrementState(state, 3);
			}
		}
		finally {
			incrementState(state, 4);
		}
	}

	private void incrementState(AtomicInteger state, int expected) {
		int old = state.compareAndExchange(expected, expected+1);
		Assert.assertEquals(old, expected);
	}
}
