package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.process.jobrunner.JobRunner;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.LocalExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.WrappingExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Parallel runs of JobExecutor to discover any concurrency issues.
 */
@Log4j2
public class ExternalRuntimeJobExecutorConcurrentTest
{
	private JobExecutor jobExecutor = new RuntimeJobExecutor(
		new WrappingExecutionAgent(
			ImmutableMap.of("local", new LocalExecutionAgent())
		)
	);

	public static void main(String[] args) throws Exception
	{
		System.exit(new ExternalRuntimeJobExecutorConcurrentTest().run(args));
	}

	public int run(String[] args) throws Exception
	{
		Specification specification = JobRunner.OBJECT_MAPPER.readValue(this.getClass().getResourceAsStream(
			"external.json"), Specification.class);

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
