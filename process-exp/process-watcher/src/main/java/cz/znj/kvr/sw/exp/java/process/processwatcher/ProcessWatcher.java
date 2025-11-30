package cz.znj.kvr.sw.exp.java.process.processwatcher;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.process.processwatcher.watcher.ProcessWatcherExecutor;
import cz.znj.kvr.sw.exp.java.process.processwatcher.watcher.RuntimeProcessWatcherExecutor;
import cz.znj.kvr.sw.exp.java.process.processwatcher.spec.Specification;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.ScheduledUtil;
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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Main class of ProcessRunner.
 *
 * Process runner, running processes and restarting if needed.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
public class ProcessWatcher extends AbstractCommand
{
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
		.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
		.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

	private final ProcessWatcherExecutor processExecutor;

	private int printDoc;
	private String properties;
	private String spec;

	private ProcessWatcherExecutor.Context processContext;

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(ProcessWatcher.class).run(
				new RootCommandContext(appContext).createChild(null, "process-watcher", null),
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

		case "--properties":
		case "-p":
			this.properties = needArgsParam(this.properties, args);
			return true;

		case "-s":
		case "--spec":
			this.spec = needArgsParam(this.spec, args);
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (this.printDoc != 0) {
			IOUtils.copy(ProcessWatcher.class.getResourceAsStream("README.md"), System.out);
			return EXIT_SUCCESS;
		}
		if (this.spec == null) {
			return usage(context, "--spec must be specified");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return "ProcessRunner - runs and controls processes";
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"--doc", "prints concise documentation",
			"-s,--spec", "definition file of processes",
			"-p,--properties", "definition file of tasks"
		);
	}

	@Override
	public int execute() throws Exception
	{
		Specification specification = readSpecification();

		Object lock = new Object();
		synchronized (lock) {
			Signal.handle(new Signal("TERM"), new SignalHandler() {
					@Override
					public void handle(Signal sig)
					{
						synchronized (lock) {
							log.info("Got TERM signal");
							processContext.cancel();
						}
					}
				}
			);
			Signal.handle(new Signal("INT"), new SignalHandler() {
					@Override
					public void handle(Signal sig)
					{
						synchronized (lock) {
							log.info("Got INT signal");
							processContext.cancel();
						}
					}
				}
			);
			Signal.handle(new Signal("HUP"), new SignalHandler() {
					@Override
					public void handle(Signal sig)
					{
						synchronized (lock) {
							Specification specification = readSpecification();
							log.info("Got HUP signal");
							processContext.reload(specification)
								.whenComplete((v, ex) -> {
									log.info("Reload", ex);
								});
						}
					}
				}
			);
			this.processContext = processExecutor.execute(specification);
		}

		processContext.waitExit().get();
		return EXIT_SUCCESS;
	}

	@SneakyThrows
	private Specification readSpecification()
	{
		Specification specification = OBJECT_MAPPER.readValue(new File(spec), Specification.class);
		if (properties != null) {
			try (Reader reader = Files.newBufferedReader(Paths.get(properties))) {
				Properties propertiesFile = new Properties();
				propertiesFile.load(reader);
				@SuppressWarnings("unchecked")
				Set<Map.Entry<String, String>> entries = (Set) propertiesFile.entrySet();
				specification = specification.toBuilder()
					.properties(ImmutableMap.copyOf(entries))
					.build();
			}
		}
		return specification;
	}

	public static class GuiceModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(ProcessWatcherExecutor.class).to(RuntimeProcessWatcherExecutor.class).in(Singleton.class);
		}

		@Provides
		public BeanFactory beanFactory(Injector injector)
		{
			return new GuiceBeanFactory(injector);
		}

		@Provides
		@Singleton
		@Named("runtimeClock")
		public Clock runtimeClock()
		{
			return Clock.systemUTC();
		}

		@Provides
		@Singleton
		public ScheduledExecutorService scheduledExecutorService()
		{
			return ScheduledUtil.sharedExecutor();
		}

		@Provides
		@Singleton
		@Named("blockingExecutor")
		public ExecutorService executor()
		{
			return Executors.newCachedThreadPool();
		}
	}
}
