package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives.syncfuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by rat on 2015-09-20.
 */
public class SyncFuture
{
	public static void		main(String[] args)
	{
		System.exit(new SyncFuture().run(args));
	}

	public int			run(String[] args)
	{
		FutureTask<?> future = new FutureTask<Object>(() -> { System.out.println("Hello\n"); return null; }) {
		};
		try {
			future.cancel(true);
			future.get();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return 0;
	}
}
