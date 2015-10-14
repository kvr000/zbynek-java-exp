package cz.znj.kvr.sw.exp.java.spring.concurrent;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class FutureTest
{
	@Test
	public void                     testSuccessListeners()
	{
		AtomicReference ran = new AtomicReference();
		SettableListenableFuture<Object> future = new SettableListenableFuture<Object>();
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onFailure(Throwable throwable) {
				Assert.fail("Unexpected success on cancelled future");
			}

			@Override
			public void onSuccess(Object o) {
				ran.set(o);
			}
		});
		future.set(Integer.valueOf(0));
		Assert.assertEquals(0, (int) ran.get());
	}

	@Test
	public void                     testExceptionListeners()
	{
		AtomicReference ran = new AtomicReference();
		SettableListenableFuture<Object> future = new SettableListenableFuture<Object>();
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onFailure(Throwable throwable) {
				ran.set(throwable);
			}

			@Override
			public void onSuccess(Object o) {
				Assert.fail("Unexpected success on excepted future");
			}
		});
		future.setException(new NumberFormatException());
		Assert.assertTrue(ran.get() instanceof NumberFormatException);
	}

	@Test
	public void                     testCancelListeners()
	{
		AtomicReference ran = new AtomicReference();
		SettableListenableFuture<Object> future = new SettableListenableFuture<Object>();
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onFailure(Throwable throwable) {
				ran.set(throwable);
			}

			@Override
			public void onSuccess(Object o) {
				Assert.fail("Unexpected success on cancelled future");
			}
		});
		future.cancel(true);
		Assert.assertTrue(ran.get() instanceof CancellationException);
	}
}
