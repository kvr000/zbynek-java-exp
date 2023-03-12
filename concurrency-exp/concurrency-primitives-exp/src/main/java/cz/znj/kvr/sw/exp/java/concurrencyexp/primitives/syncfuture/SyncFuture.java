package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives.syncfuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


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
