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
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;


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
		.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
		.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

	private final ProcessWatcherExecutor processExecutor;

	private int printDoc;
	private String properties;
	private static final int AUTO_RELOAD_ATTEMPTS = 5;
	private static final long RELOAD_RETRY_DELAY_MS = 500;

	private String spec;
	private String pidFile;
	private boolean autoReload;

	private final Object lock = new Object();

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

		case "--pid-file":
			this.pidFile = needArgsParam(this.pidFile, args);
			return true;

		case "--auto-reload":
			this.autoReload = true;
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
			"-p,--properties", "definition file of tasks",
			"--auto-reload", "reload automatically when the spec or properties file changes",
			"--pid-file", "file to write the process-watcher PID to at start, removed on exit"
		);
	}

	@Override
	public int execute() throws Exception
	{
		Specification specification = readSpecification();

		Path pidPath = pidFile == null ? null : writePidFile(Paths.get(pidFile));
		try {
			return executeWatcher(specification);
		}
		finally {
			if (pidPath != null) {
				deletePidFile(pidPath);
			}
		}
	}

	private Path writePidFile(Path path) throws IOException
	{
		Files.write(path, (ProcessHandle.current().pid() + "\n").getBytes(StandardCharsets.UTF_8));
		path.toFile().deleteOnExit();
		return path;
	}

	private void deletePidFile(Path path)
	{
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ex) {
			log.warn("Failed to delete PID file {}", path, ex);
		}
	}

	private int executeWatcher(Specification specification) throws Exception
	{
		synchronized (lock) {
			try {
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
			}
			catch (RuntimeException ex) {
				// HUP is not supported on all platforms (e.g. Windows)
				log.warn("TERM signal handler not installed: {}", ex.toString());
			}
			try {
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
			}
			catch (RuntimeException ex) {
				// HUP is not supported on all platforms (e.g. Windows)
				log.warn("INT signal handler not installed: {}", ex.toString());
			}
			try {
				Signal.handle(new Signal("HUP"), new SignalHandler() {
						@Override
						public void handle(Signal sig)
						{
							log.info("Got HUP signal");
							reloadSpecification(1);
						}
					}
				);
			}
			catch (RuntimeException ex) {
				// HUP is not supported on all platforms (e.g. Windows)
				log.warn("HUP signal handler not installed: {}", ex.toString());
			}
			this.processContext = processExecutor.execute(specification);
		}

		Thread fileWatcher = autoReload ? startFileWatcher() : null;
		try {
			processContext.waitExit().get();
		}
		finally {
			if (fileWatcher != null) {
				fileWatcher.interrupt();
			}
		}
		return EXIT_SUCCESS;
	}

	private void reloadSpecification(int attempts)
	{
		// read outside the lock, so retries do not block signal handling
		Specification specification;
		for (int attempt = 1; ; ++attempt) {
			try {
				specification = readSpecification();
				break;
			}
			catch (Exception ex) {
				if (attempt >= attempts) {
					log.warn("Failed to read specification, keeping the current one", ex);
					return;
				}
				log.info("Failed to read specification, retrying: attempt={}/{} error={}", attempt, attempts, ex.toString());
				try {
					Thread.sleep(RELOAD_RETRY_DELAY_MS);
				}
				catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		synchronized (lock) {
			if (processContext == null) {
				return;
			}
			processContext.reload(specification)
				.whenComplete((v, ex) -> {
					if (ex != null) {
						log.warn("Reload failed", ex);
					}
					else {
						log.info("Reload completed");
					}
				});
		}
	}

	private Thread startFileWatcher() throws IOException
	{
		Set<Path> files = new HashSet<>();
		files.add(Paths.get(spec).toAbsolutePath().normalize());
		if (properties != null) {
			files.add(Paths.get(properties).toAbsolutePath().normalize());
		}
		WatchService watchService = FileSystems.getDefault().newWatchService();
		try {
			for (Path dir: files.stream().map(Path::getParent).collect(Collectors.toSet())) {
				dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
			}
		}
		catch (IOException | RuntimeException ex) {
			watchService.close();
			throw ex;
		}
		Thread thread = new Thread(() -> {
			try (WatchService ws = watchService) {
				for (;;) {
					WatchKey key = ws.take();
					boolean changed = false;
					for (;;) {
						Path dir = (Path) key.watchable();
						for (WatchEvent<?> event: key.pollEvents()) {
							if (event.context() instanceof Path && files.contains(dir.resolve((Path) event.context()).normalize())) {
								changed = true;
							}
						}
						key.reset();
						if (!changed) {
							break;
						}
						// debounce: wait for editors writing the file in several steps
						Thread.sleep(300);
						key = ws.poll();
						if (key == null) {
							break;
						}
					}
					if (changed) {
						log.info("Configuration file changed, reloading");
						reloadSpecification(AUTO_RELOAD_ATTEMPTS);
					}
				}
			}
			catch (InterruptedException | ClosedWatchServiceException ex) {
				// exiting
			}
			catch (IOException ex) {
				log.warn("Configuration file watcher failed", ex);
			}
		}, "config-watcher");
		thread.setDaemon(true);
		thread.start();
		return thread;
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
