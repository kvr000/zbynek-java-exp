package cz.znj.kvr.sw.java.concurrencyexp.dynamicexecutor;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by rat on 2015-09-20.
 */
public class FsDynamicExecutor
{
	public static void		main(String[] args)
	{
		ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		AtomicLong counter = new AtomicLong();
		for (String file: args) {
			es.submit(new FileProcessor(es, counter, new File(file)));
			counter.incrementAndGet();
		}
		synchronized (counter) {
			if (counter.get() != 0) {
				try {
					counter.wait();
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		es.shutdown();
		try {
			es.awaitTermination(0, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static class FileProcessor implements Runnable
	{
		private final ExecutorService	es;
		private final AtomicLong	counter;
		private final File		file;

		public				FileProcessor(ExecutorService es, AtomicLong counter, File file)
		{
			this.es = es;
			this.counter = counter;
			this.file = file;
		}

		public void			run()
		{
			File[] children = null;
			try {
				try {
					if (!file.exists())
						return;
					System.out.println("Producer "+0+": \""+file.getPath()+"\" "+file.length());
					if (!file.isDirectory())
						return;
					children = file.listFiles();
					if (children == null)
						return;
				}
				catch (Exception ex) {
				}
				for (File child : children) {
					es.submit(new FileProcessor(es, counter, child));
					counter.incrementAndGet();
				}
			}
			finally {
				if (counter.decrementAndGet() == 0) {
					synchronized (counter) {
						counter.notifyAll();
					}
				}
			}
		}
	}
}
