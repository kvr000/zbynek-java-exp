package cz.znj.kvr.sw.exp.java.process.processwatcher.watcher;

import cz.znj.kvr.sw.exp.java.process.processwatcher.spec.Specification;

import java.util.concurrent.CompletableFuture;


/**
 * ProcessWatcher component, the one which actually does all the task running.
 */
public interface ProcessWatcherExecutor
{
	/**
	 * Executes the job tree, according to specification.
	 *
	 * @param specification
	 * 	job tree specification.
	 *
	 * @return
	 * 	future eventually containing the first failure.
	 */
	Context execute(Specification specification);

	interface Context
	{
		CompletableFuture<Void> waitExit();

		CompletableFuture<Void> cancel();

		CompletableFuture<Void> reload(Specification specification);
	}
}
