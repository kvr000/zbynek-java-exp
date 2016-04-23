package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Occupying CPU to put stress on other processes.
 */
public class TakeCpuMain
{
	private ExecutorService executor = Executors.newCachedThreadPool();

	public static void main(String[] args) throws InterruptedException
	{
		System.exit(new TakeCpuMain().run(args));
	}

	public int run(String[] args) throws InterruptedException
	{
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
			executor.execute(() -> { for (;;) { } });
		}
		executor.shutdown();
		// let wait for 1 hour only, in case I forget
		executor.awaitTermination(1, TimeUnit.HOURS);
		return 0;
	}
}
