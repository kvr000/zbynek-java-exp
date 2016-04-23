package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;

import java.util.concurrent.CompletableFuture;


/**
 * JobExecutor component, the one which actually does all the task running.
 */
public interface JobExecutor
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
	CompletableFuture<Integer> execute(Specification specification);
}
