package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives.scheduled;

import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;


public class CancellingScheduledFutureTest
{
	@Test
	public void executeScheduled_insideException_cancelled() throws Exception
	{
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(Integer.MAX_VALUE);
		AtomicInteger counter = new AtomicInteger();
		try {
			ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
				() -> {
					counter.incrementAndGet();
					throw new CancellationException();
				},
				0,
				10,
				TimeUnit.MILLISECONDS
			);
			ExecutionException ex = expectThrows(ExecutionException.class, () -> future.get());
			assertThat(ex.getCause(), instanceOf(CancellationException.class));
			Thread.sleep(200);
			assertEquals(counter.get(), 1);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	public void executeScheduled_singleActive_filteredExecutions() throws Exception
	{
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(Integer.MAX_VALUE);
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger active = new AtomicInteger();
		try {
			ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
				() -> {
					if (!active.compareAndSet(0, 1)) {
						return;
					}
					CompletableFuture.delayedExecutor(60, TimeUnit.MILLISECONDS)
						.execute(() -> {
							counter.incrementAndGet();
							active.set(0);
						});
				},
				0,
				10,
				TimeUnit.MILLISECONDS
			);
			Thread.sleep(170);
			future.cancel(true);
			assertThat(counter.get(), lessThanOrEqualTo(4)); // relax on delays
		}
		finally {
			executor.shutdown();
		}
	}
}
