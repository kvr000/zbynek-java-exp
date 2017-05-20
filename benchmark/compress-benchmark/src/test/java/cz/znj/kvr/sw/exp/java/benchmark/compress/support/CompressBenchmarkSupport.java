package cz.znj.kvr.sw.exp.java.benchmark.compress.support;

import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;


/**
 * Small data supplier of key and value.
 */
public class CompressBenchmarkSupport
{
	public static byte[] generateInput(int length)
	{
		byte[] out = new byte[length];
		for (int i = 0; i < length; ++i) {
			out[i] = (byte)RandomUtils.nextInt(0, 256);
		}
		return out;
	}

	public static void runParallel(int threads, Consumer<Integer> runner)
	{
		List<Future<Void>> futures = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		for (int i = 0; i < threads; ++i) {
			int ii = i;
			executor.submit(() -> runner.accept(ii));
		}
		executor.shutdown();
		for (Future<Void> future: futures) {
			try {
				future.get();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
