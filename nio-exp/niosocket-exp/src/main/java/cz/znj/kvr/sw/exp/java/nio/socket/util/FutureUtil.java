package cz.znj.kvr.sw.exp.java.nio.socket.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Future utilities.
 */
public class FutureUtil
{
	/**
	 * Returns future which is already completed exceptionally.
	 *
	 * @param ex
	 * 	exception cause the failure
	 * @param <T>
	 *      type of future
	 *
	 * @return
	 * 	exceptionally completed future.
	 */
	public static <T> CompletableFuture<T> exception(Throwable ex)
	{
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(ex);
		return future;
	}

	/**
	 * Runs futures and cancels them when any of them exits.
	 *
	 * @param futures
	 * 	list of futures to monitor and cancel
	 *
	 * @param <T>
	 * 	type of Future
	 *
	 * @return
	 * 	new Future monitoring all underlying futures.
	 */
	public static <T> CompletableFuture<T> anyAndCancel(List<CompletableFuture<T>> futures)
	{
		Consumer<Boolean> doCancel = (interrupt) -> futures.forEach(future -> future.cancel(interrupt));
		CompletableFuture<T> result = new CompletableFuture<T>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				doCancel.accept(interrupt);
				return true;
			}
		};
		for (CompletableFuture<T> future: futures) {
			future.whenComplete((v, ex) -> completeOrFail(result, v, ex));
		}
		return result.whenComplete((v, ex) -> doCancel.accept(true));
	}

	/**
	 * Completes or fails the other Future, based on provided result.
	 *
	 * @param future
	 * 	future to update
	 * @param value
	 * 	value to set as a result, in case ex is null
	 * @param ex
	 * 	exception to fail the future, in case ex is not null
	 * @param <T>
	 *      type of future
	 *
	 * @return
	 * 	the same future
	 */
	public static <T> CompletableFuture<T> completeOrFail(CompletableFuture<T> future, T value, Throwable ex)
	{
		if (ex != null)
			future.completeExceptionally(ex);
		else
			future.complete(value);
		return future;
	}

	/**
	 * Wraps exception consumer into Future.whenComplete consumer.
	 *
	 * @param consumer
	 * 	exception consumer
	 * @param <T>
	 *      type of Future
	 * @param <X>
	 *      type of consumed exception
	 *
	 * @return
	 * 	Future.whenComplete BiConsumer calling provided consumer only in case of failure.
	 */
	public static <T, X extends Throwable> BiConsumer<T, X> whenException(Consumer<X> consumer)
	{
		return (v, ex) -> {
			if (ex != null)
				consumer.accept(ex);
		};
	}
}
