package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;


/**
 * Netty Future utilities.
 */
public class NettyFutures
{
	public static <V> CompletableFuture<V> toCompletable(Future<V> future)
	{
		return new CompletableFuture<V>() {
			{
				future.addListener((f) -> {
					try {
						complete(future.getNow());
					}
					catch (Throwable ex) {
						completeExceptionally(ex);
					}
				});
			}

			public boolean cancel(boolean interrupt)
			{
				try {
					return future.cancel(interrupt);
				}
				finally {
					super.cancel(interrupt);
				}
			}
		};
	}

	public static <V> void completeOrFail(Future<V> future, CompletableFuture<V> completable)
	{
		future.addListener((f) -> {
			try {
				@SuppressWarnings("unchecked")
				V result = (V) f.get();
				completable.complete(result);
			}
			catch (ExecutionException ex) {
				completable.completeExceptionally(ex.getCause());
			}
			catch (Throwable ex) {
				completable.completeExceptionally(ex);
			}
		});
	}

	public static <V> CompletableFuture<V> join(Future<V> one, Future<V> two)
	{
		return new CompletableFuture<V>() {
			{
				AtomicInteger count = new AtomicInteger(2);
				GenericFutureListener<Future<V>> listener = (f) -> {
					try {
						V v = f.getNow();
						if (count.decrementAndGet() == 0)
							complete(v);
					}
					catch (Throwable ex) {
						completeExceptionally(ex);
					}
				};
				one.addListener(listener);
				two.addListener(listener);
			}

			@Override
			public boolean cancel(boolean interrupt)
			{
				try {
					return one.cancel(interrupt)|two.cancel(interrupt);
				}
				finally {
					super.cancel(interrupt);
				}
			}
		};
	}

	public static <V> CompletableFuture<V> join(CompletableFuture<V> one, CompletableFuture<V> two)
	{
		return new CompletableFuture<V>() {
			{
				AtomicInteger count = new AtomicInteger(2);
				BiConsumer<V, Throwable> listener = (v, ex) -> {
					if (ex == null) {
						if (count.decrementAndGet() == 0)
							complete(v);
					}
					else {
						completeExceptionally(ex);
						cancel(true);
					}
				};
				one.whenComplete(listener);
				two.whenComplete(listener);
			}

			@Override
			public boolean cancel(boolean interrupt)
			{
				try {
					return one.cancel(interrupt)|two.cancel(interrupt);
				}
				finally {
					super.cancel(interrupt);
				}
			}
		};
	}

	public static <T> CompletableFuture<CompletableFuture<T>> nestedAllOrCancel(List<CompletableFuture<CompletableFuture<T>>> futures)
	{
		AtomicInteger remaining = new AtomicInteger(futures.size());

		return new CompletableFuture<CompletableFuture<T>>() {
			{
				futures.forEach(f -> {
					f.whenComplete((v, ex) -> {
						if (ex != null) {
							completeExceptionally(ex);
						}
						if (remaining.decrementAndGet() == 0) {
							stepInner();
						}
					});
				});
				whenComplete((v, ex) -> {
					if (ex != null) {
						futures.forEach(f -> {
							f.cancel(true);
							f.thenAccept(sf -> sf.cancel(true));
						});
					}
				});
			}

			private void stepInner()
			{
				complete(new CompletableFuture<T>() {
					{
						futures.forEach(f ->
							f.thenAccept(sf -> sf.whenComplete((v, ex) -> {
								if (ex != null) {
									completeExceptionally(ex);
								}
								else {
									complete(v);
								}
							}))
						);
						whenComplete((v, ex) -> {
							futures.forEach(f -> {
								f.thenAccept(sf -> sf.cancel(true));
							});
						});
					}

					public synchronized boolean cancel(boolean interrupt)
					{
						futures.forEach(f -> {
							f.thenAccept(sf -> sf.cancel(true));
						});
						return super.cancel(interrupt);
					}
				});
			}

			@Override
			public boolean cancel(boolean interrupt)
			{
				futures.forEach((future) -> {
					future.cancel(interrupt);
					future.thenAccept(sf -> sf.cancel(interrupt));
				});
				return super.cancel(interrupt);
			}
		};
	}
}
