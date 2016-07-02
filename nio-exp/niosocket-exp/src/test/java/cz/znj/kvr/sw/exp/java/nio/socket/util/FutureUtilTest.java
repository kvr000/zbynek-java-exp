package cz.znj.kvr.sw.exp.java.nio.socket.util;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class FutureUtilTest
{
	@Test(expected = NumberFormatException.class)
	public void anyAndCancel_oneFailed_fails() throws Throwable
	{
		CompletableFuture<Void> one = new CompletableFuture<>();
		try {
			FutureUtil.anyAndCancel(ImmutableList.of(
				one,
				FutureUtil.exception(new NumberFormatException())
			)).get();
		}
		catch (ExecutionException e) {
			Assert.assertTrue(one.isCancelled());
			throw e.getCause();
		}
	}

	@Test(expected = CancellationException.class)
	public void anyAndCancel_cancel_allCancelled() throws Throwable
	{
		CompletableFuture<Void> one = new CompletableFuture<>();
		CompletableFuture<Void> two = new CompletableFuture<>();
		try {
			CompletableFuture<Void> future = FutureUtil.anyAndCancel(ImmutableList.of(
				one,
				two
			));
			future.cancel(true);
			future.get();
		}
		catch (ExecutionException e) {
			Assert.assertTrue(one.isCancelled());
			Assert.assertTrue(two.isCancelled());
			throw e.getCause();
		}
	}
}
