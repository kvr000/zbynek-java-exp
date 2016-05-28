package cz.znj.kvr.sw.exp.java.netty.netty4.server.common;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.CompletableFuture;
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
}
