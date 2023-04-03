package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives.future;

import net.dryuf.concurrent.function.ThrowingBiFunction;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.expectThrows;


public class CompletableFutureTest
{
	@Test
	public void whenComplete_whenThrows_thenPropagated()
	{
		CompletableFuture<Object> future = CompletableFuture.completedFuture(null)
				.whenComplete((v, ex) -> {
					throw new NumberFormatException();
				});
		ExecutionException ex = expectThrows(ExecutionException.class, future::get);

		assertThat(ex.getCause(), instanceOf(NumberFormatException.class));
	}

	@Test
	public void handle_whenThrows_thenPropagated()
	{
		CompletableFuture<Object> future = CompletableFuture.completedFuture(null)
				.handle(ThrowingBiFunction.sneaky((v, ex) -> {
					throw new IOException();
				}));
		ExecutionException ex = expectThrows(ExecutionException.class, future::get);

		assertThat(ex.getCause(), instanceOf(IOException.class));
	}
}
