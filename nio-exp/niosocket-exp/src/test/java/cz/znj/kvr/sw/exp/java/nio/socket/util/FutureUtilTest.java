package cz.znj.kvr.sw.exp.java.nio.socket.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class FutureUtilTest
{
	@Test(expected = NumberFormatException.class)
	public void anyAndCancel_oneFailed_fails() throws Throwable
	{
		try {
			FutureUtil.anyAndCancel(ImmutableList.of(
				new CompletableFuture<>(),
				FutureUtil.exception(new NumberFormatException())
			)).get();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (ExecutionException e) {
			throw e.getCause();
		}
	}
}
