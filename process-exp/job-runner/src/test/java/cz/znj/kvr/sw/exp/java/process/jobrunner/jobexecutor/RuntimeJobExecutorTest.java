package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.process.jobrunner.JobRunner;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.ExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.FunctionExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.LocalExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.SshExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import org.apache.commons.lang3.SystemUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Parallel runs of JobExecutor to discover any concurrency issues.
 */
@Log4j2
public class RuntimeJobExecutorTest
{
	private BeanFactory beanFactory;
	private JobExecutor jobExecutor;

	@BeforeClass
	public void setup()
	{
		beanFactory = Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class);

		jobExecutor = beanFactory.getBean(JobExecutor.class);
	}

	@Test(timeOut = 15000L)
	public void runFunction() throws Exception
	{
		Specification specification = JobRunner.OBJECT_MAPPER.readValue(this.getClass().getResourceAsStream(
			"functiontest.json"), Specification.class);

		CompletableFuture<Integer> future = null;
		future = jobExecutor.execute(specification);
		Assert.assertEquals(future.get(), (Integer) 0);
	}

	@Test(timeOut = 15000L)
	public void runExternal() throws Exception
	{
		if (SystemUtils.IS_OS_WINDOWS) {
			// sleep utility may not be available on windows
			return;
		}
		Specification specification = JobRunner.OBJECT_MAPPER.readValue(this.getClass().getResourceAsStream(
			"external.json"), Specification.class);

		CompletableFuture<Integer> future = null;
		future = jobExecutor.execute(specification);
		Assert.assertEquals(future.get(), (Integer) 0);
	}

	public static class GuiceModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(JobExecutor.class).to(RuntimeJobExecutor.class).in(Singleton.class);
		}

		@Provides
		public Map<String, ExecutionAgent> executionAgents(
			FunctionExecutionAgent functionExecutionAgent,
			LocalExecutionAgent localExecutionAgent,
			SshExecutionAgent sshExecutionAgent
		) {
			return ImmutableMap.of(
				"function", functionExecutionAgent,
				"local", localExecutionAgent,
				"ssh", sshExecutionAgent
			);
		}

		@Provides
		public ExecutorService executor()
		{
			return Executors.newCachedThreadPool();
		}

		@Provides
		public BeanFactory beanFactory(Injector injector)
		{
			return new GuiceBeanFactory(injector);
		}
	}
}
