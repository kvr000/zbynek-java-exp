package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import net.dryuf.concurrent.FutureUtil;
import net.dryuf.concurrent.function.ThrowingFunction;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * Netty Future utilities.
 */
public class NettyFutures
{
	/**
	 * Converts Netty Future to CompletableFuture.
	 */
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

	/**
	 * Propagates Netty Future to existing CompletableFuture.
	 */
	public static <V> void copy(Future<V> future, CompletableFuture<V> target)
	{
		future.addListener((f) -> {
			try {
				@SuppressWarnings("unchecked")
				V result = (V) f.get();
				target.complete(result);
			}
			catch (ExecutionException ex) {
				target.completeExceptionally(ex.getCause());
			}
			catch (Throwable ex) {
				target.completeExceptionally(ex);
			}
		});
	}

	/**
	 * Propagates CompletableFuture to existing CompletableFuture.
	 */
	public static <V> void copy(CompletableFuture<V> source, CompletableFuture<V> target)
	{
		source.whenComplete((v, ex) -> FutureUtil.completeOrFail(target, v, ex));
	}

	/**
	 * Adds handler to CompletableFuture chain, no matter whether it is successful or failed.
	 *
	 * @return
	 * 	CompletableFuture representing either the original exception or return value from handler.
	 */
	public static <V, R> CompletableFuture<R> composeAlways(CompletableFuture<V> source, Callable<CompletableFuture<R>> handler)
	{
		return new CompletableFuture<>()
		{
			{
				source.whenComplete((v, ex) -> {
					try {
						handler.call().whenComplete((v2, ex2) -> {
							if (ex != null) {
								completeExceptionally(ex);
							}
							else if (ex2 != null) {
								completeExceptionally(ex2);
							}
							else {
								complete(v2);
							}
						});
					}
					catch (Throwable ex2) {
						completeExceptionally(Objects.requireNonNullElse(ex, ex2));
					}
				});
			}
		};
	}

	/**
	 * Joins two Netty Future objects, propagating any exception or last result.
	 */
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

	/**
	 * Joins two CompletableFuture objects, propagating first exception or last result.
	 */
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

	/**
	 * Converts List of CompletableFuture objects into one CompletableFuture completing once all original futures
	 * are completed.  It cancels all original futures or closes underlying AutoCloseable when some of them fails.
	 */
	public static <T extends AutoCloseable> CompletableFuture<List<T>> nestedAllOrCancel(List<CompletableFuture<T>> futures)
	{
		AtomicInteger remaining = new AtomicInteger(futures.size());

		return new CompletableFuture<List<T>>() {
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
							f.thenAccept(sf -> {
								try {
									sf.close();
								}
								catch (Exception e) {
									// ignore;
								}
							});
						});
					}
				});
			}

			private void stepInner()
			{
				complete(futures.stream()
					.map(CompletableFuture::join)
					.collect(Collectors.toList())
				);
			}

			@Override
			public boolean cancel(boolean interrupt)
			{
				futures.forEach((future) -> {
					future.cancel(interrupt);
					future.thenAccept(sf -> {
						try {
							sf.close();
						}
						catch (Exception e) {
						}
					});
				});
				return super.cancel(interrupt);
			}
		};
	}

	@SneakyThrows
	public static <T> T sneakyCall(ThrowingCall<T> c)
	{
		return c.call();
	}

	@FunctionalInterface
	public interface ThrowingCall<T>
	{
		T call() throws Throwable;
	}
}
