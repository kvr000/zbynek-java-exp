package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import com.google.inject.Guice;
import cz.znj.kvr.sw.exp.java.process.jobrunner.JobRunner;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Parallel runs of JobExecutor to discover any concurrency issues.
 *
 * Faster responses with internal function execution should lead to quicker stress on JVM thread pools.
 */
@Log4j2
public class FunctionRuntimeJobExecutorConcurrentTest
{
	private JobExecutor jobExecutor = Guice.createInjector(new RuntimeJobExecutorTest.GuiceModule())
		.getInstance(JobExecutor.class);

	public static void main(String[] args) throws Exception
	{
		System.exit(new FunctionRuntimeJobExecutorConcurrentTest().run(args));
	}

	public int run(String[] args) throws Exception
	{
		Specification specification = JobRunner.OBJECT_MAPPER.readValue(this.getClass().getResourceAsStream(
			"functiontest.json"), Specification.class);

		for (;;) {
			CompletableFuture<Integer> future = null;
			try {
				future = jobExecutor.execute(specification);
				Assert.assertEquals(future.get(15, TimeUnit.SECONDS), (Integer) 0);
			}
			catch (TimeoutException ex) {
				log.fatal("Test case timed out", ex);
				log.fatal("Future: {}", future);
				throw ex;
			}
		}
	}
}
