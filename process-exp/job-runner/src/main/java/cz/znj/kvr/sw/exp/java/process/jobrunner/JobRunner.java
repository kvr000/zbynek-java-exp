package cz.znj.kvr.sw.exp.java.process.jobrunner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.ExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.FunctionExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.LocalExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.SshExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor.JobExecutor;
import cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor.RuntimeJobExecutor;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.JobTask;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.MachineGroup;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;
import org.apache.commons.io.IOUtils;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Main class of JobRunner.
 *
 * Simple job runner, running the processes on local machine or via ssh on remote machine.  No persistence is supported,
 * therefore the JobRunner must be kept alive all the time during execution.
 *
 * Also, no validation of data is supported, eventually resulting into fatal failure during execution.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
public class JobRunner extends AbstractCommand
{
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
		.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
		.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

	private final JobExecutor jobExecutor;

	private int printDoc;
	private String tasksFile;
	private String machinesFile;
	private String machinesGroupsFile;
	private String fullFile;

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(JobRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "job-runner", null),
				Arrays.asList(args0)
			);
		});
	}

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "--doc":
			this.printDoc = 1;
			return true;

		case "--tasks":
		case "-t":
			this.tasksFile = needArgsParam(this.tasksFile, args);
			return true;

		case "-m":
		case "--machines":
			this.machinesFile = needArgsParam(this.machinesFile, args);
			return true;

		case "-g":
		case "--machine-groups":
			this.machinesGroupsFile = needArgsParam(this.machinesGroupsFile, args);
			return true;

		case "-s":
		case "--spec":
			this.fullFile = needArgsParam(this.fullFile, args);
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (this.printDoc != 0) {
			IOUtils.copy(JobRunner.class.getResourceAsStream("README.md"), System.out);
			return EXIT_SUCCESS;
		}
		if ((this.fullFile != null) == (this.tasksFile != null || this.machinesFile != null || this.machinesGroupsFile != null)) {
			return usage(context, "Either --spec must be specified or all of --tasks, --machines, --machine-groups but not both set at the same time");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return "JobRunner - runs jobs required by definition file";
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"--doc", "prints concise documentation",
			"-s,--spec", "definition file of tasks",
			"-t,--tasks", "definition file of tasks",
			"-m,--machines", "definition file of machines",
			"-g,--machine-groups", "definition file of machines"
		);
	}

	@Override
	public int execute() throws Exception
	{
		Specification specification;
		if (fullFile != null) {
			specification = OBJECT_MAPPER.readValue(new File(fullFile), Specification.class);
		}
		else {
			specification = Specification.builder()
				.tasks(readSingleElement(tasksFile, "tasks", new TypeReference<Map<String, JobTask>>()
				{
				}))
				.machines(readSingleElement(machinesFile, "machines", new TypeReference<Map<String, Machine>>()
				{
				}))
				.machineGroups(readSingleElement(machinesGroupsFile, "machineGroups", new TypeReference<Map<String, MachineGroup>>()
				{
				}))
				.build();
		}
		CompletableFuture<Integer> future = jobExecutor.execute(specification);

		Signal.handle(new Signal("TERM"), new SignalHandler()
			{
				@Override
				public void handle(Signal sig)
				{
					log.info("Got TERM signal");
					future.cancel(true);
				}
			}
		);
		Signal.handle(new Signal("INT"), new SignalHandler()
			{
				@Override
				public void handle(Signal sig)
				{
					log.info("Got INT signal");
					future.cancel(true);
				}
			}
		);

		return Optional.ofNullable(future.get())
			.orElse(EXIT_FAILURE);
	}

	@SuppressWarnings("unchecked")
	private <T> Map<String, T> readSingleElement(String file, String element, TypeReference<Map<String, T>> type) throws IOException
	{
		return Optional.ofNullable(OBJECT_MAPPER.readValue(new File(file), Map.class))
			.map(content -> content.get(element))
			.map(content -> (Map<String, T>) OBJECT_MAPPER.convertValue(content, type))
			.orElseThrow(() -> new IllegalArgumentException("Expected map with field '"+element+"' in "+file));
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
