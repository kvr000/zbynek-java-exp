package cz.znj.kvr.sw.exp.java.concurrencyexp.scheduled;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
public class ScheduledExecutor
{
	public static void		main(String[] args)
	{
		System.exit(new ScheduledExecutor().run(args));
	}

	public int			run(String[] args)
	{
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
		ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
			System.out.println("hello");
		}, 0, 100, TimeUnit.MILLISECONDS);
		new Thread(() -> {
			try {
				Thread.sleep(550);
				future.cancel(true);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}).start();
		try {
			future.get();
			throw new IllegalStateException();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		catch (CancellationException e) {
			// ok
		}
		System.out.println("ok, done");
		return 0;
	}
}
