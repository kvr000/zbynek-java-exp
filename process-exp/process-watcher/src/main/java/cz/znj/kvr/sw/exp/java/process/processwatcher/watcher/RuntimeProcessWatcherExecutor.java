package cz.znj.kvr.sw.exp.java.process.processwatcher.watcher;

import com.google.common.base.Preconditions;
import cz.znj.kvr.sw.exp.java.process.processwatcher.process.ProcessExecutor;
import cz.znj.kvr.sw.exp.java.process.processwatcher.process.TaskHandle;
import cz.znj.kvr.sw.exp.java.process.processwatcher.spec.Process;
import cz.znj.kvr.sw.exp.java.process.processwatcher.spec.Specification;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.queue.SingleConsumerQueue;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;


/**
 * Runtime based ProcessWatcherExecutor, maintaining all information in runtime.
 */
@Log4j2
@Singleton
public class RuntimeProcessWatcherExecutor implements ProcessWatcherExecutor
{
	public static final long TERMINATE_TIMEOUT_MS = 10_000;

	@Named("runtimeClock")
	private final Clock runtimeClock;
	private final ScheduledExecutorService scheduledExecutorService;
	private final ProcessExecutor processExecutor;

	@Inject
	public RuntimeProcessWatcherExecutor(@Named("runtimeClock") Clock runtimeClock, ScheduledExecutorService scheduledExecutorService, ProcessExecutor processExecutor)
	{
		this.runtimeClock = runtimeClock;
		this.scheduledExecutorService = scheduledExecutorService;
		this.processExecutor = processExecutor;
	}

	public RuntimeContext execute(Specification specification)
	{
		RuntimeContext context = populateContext(specification);
		scheduleReview(context);
		return context;
	}

	private void doNext(RuntimeContext context)
	{
		try (SingleConsumerQueue<Runnable>.Consumer reader = context.pendingTasks.consume()) {
			Runnable runnable;
			while ((runnable = reader.next()) != null) {
				runnable.run();
			}

			if (context.needReview) {
				context.needReview = false;
				handleReview(context);
			}
		}
	}

	private void needReview(RuntimeContext context)
	{
		context.needReview = true;
	}

	private void scheduleReview(RuntimeContext context)
	{
		scheduleStep(context, () -> needReview(context));
	}

	private void scheduleStep(RuntimeContext context, Runnable step)
	{
		context.pendingTasks.add(step);
	}

	private void runProcess(RuntimeContext context, ProcessState state)
	{
		TaskHandle taskHandle = state.process.getCommand() == null ?
			processExecutor.execute(state.process.getShellCommand()) :
			processExecutor.execute(state.process.getCommand());
		log.info("Process started: process={}", state.id);
		state.taskHandle = taskHandle;
		taskHandle.waitExit().whenComplete((v, ex) ->
			scheduleStep(context, () -> handleExited(context, state, v, ex))
		);
		state.status = ProcessState.Status.STARTING;
		state.lastStart = runtimeClock.millis();
		scheduledExecutorService.schedule(() -> scheduleStep(context, () -> {
				if (state.taskHandle == taskHandle) {
					handleRunning(context, state);
				}
			}),
			state.process.getStartTimeMs(),
			TimeUnit.MILLISECONDS
		);
	}

	private void terminateProcess(RuntimeContext context, ProcessState state)
	{
		TaskHandle oldHandle = state.taskHandle;
		state.taskHandle.terminate();
		scheduledExecutorService.schedule(() -> scheduleStep(context, () -> {
			if (state.taskHandle == oldHandle) {
				state.taskHandle.kill();
			}
		}), TERMINATE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	private void handleReview(RuntimeContext context)
	{
		for (ProcessState processState: context.processes.values()) {
			switch (processState.desired) {
			case RUNNING:
				if (processState.taskHandle == null) {
					if (!processState.process.getDependencies().stream()
						.allMatch(dependency -> Optional.ofNullable(context.processes.get(dependency))
							.map(ProcessState::isUp)
							.orElse(true))) {
						continue;
					}
					long delay = processState.lastStart+processState.process.getRestartDelayMs()-runtimeClock.millis();
					if (delay > 0) {
						scheduledExecutorService.schedule(() -> scheduleReview(context), delay, TimeUnit.MILLISECONDS);
						continue;
					}
					runProcess(context, processState);
				}
				break;

			case DISABLED:
				if (processState.taskHandle == null) {
					processState.status = ProcessState.Status.DISABLED;
					break;
				}
				// fall through
			case STOPPED:
			case REMOVE:
			case RESTART:
				if (processState.taskHandle != null && processState.status != ProcessState.Status.TERMINATING) {
					terminateProcess(context, processState);
				}
				break;
			}
		}

		if (context.cancelling) {
			if (context.processes.values().stream()
				.allMatch(ProcessState::isDown)) {
				context.exitFuture.complete(null);
			}
		}

		if (context.reloadRequest != null) {
			Pair<Specification, CompletableFuture<Void>> localRequest = context.reloadRequest;
			if (context.processes.values().stream()
				.allMatch(ProcessState::isUp)) {
				if (RELOAD_REQUEST_UPDATER.compareAndSet(context, localRequest, null)) {
					localRequest.getRight().complete(null);
				}
			}
		}
	}

	private void handleReload(RuntimeContext context)
	{
		Pair<Specification, CompletableFuture<Void>> request = RELOAD_REQUEST_UPDATER.getAndSet(context, null);
		Specification specification = request.getLeft();
		if (context.cancelling) {
			request.getRight().completeExceptionally(new IllegalStateException("Terminate in progress"));
		}
		try {
			validateSpec(specification);
			for (ProcessState state: context.processes.values()) {
				Process process = specification.getProcesses().get(state.id);
				if (process == null) {
					state.desired = ProcessState.Status.REMOVE;
					continue;
				}
				else if (!process.equals(state.process)) {
					state.process = process;
					state.desired = checkDisabled(context, process) ? ProcessState.Status.DISABLED : ProcessState.Status.RESTART;
				}
				else {
					state.desired = checkDisabled(context, process) ? ProcessState.Status.DISABLED : ProcessState.Status.RUNNING;
				}
			}
			for (Map.Entry<String, Process> entry: specification.getProcesses().entrySet()) {
				context.processes.computeIfAbsent(entry.getKey(), (key) -> populateProcessState(context, entry));
			}
			request.getValue().complete(null);
		}
		catch (Throwable ex) {
			request.getValue().completeExceptionally(ex);
		}
		finally {
			needReview(context);
		}
	}

	private void handleRunning(RuntimeContext context, ProcessState state)
	{
		log.info("Process running: process={}", state.id);
		state.status = ProcessState.Status.RUNNING;
		needReview(context);
	}

	private void handleExited(RuntimeContext context, ProcessState state, Integer exit, Throwable ex)
	{
		log.error("Process exited: process={} exit={}", state.id, exit, ex);
		state.status = ProcessState.Status.STOPPED;
		state.taskHandle = null;
		switch (state.desired) {
		case REMOVE:
			context.processes.remove(state.id);
			break;

		case RESTART:
			state.desired = ProcessState.Status.RUNNING;
			break;
		}
		needReview(context);
	}

	private RuntimeContext populateContext(Specification specification)
	{
		validateSpec(specification);
		RuntimeContext context = new RuntimeContext();
		context.properties = specification.getProperties();
		context.processes = specification.getProcesses().entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				(entry) -> populateProcessState(context, entry),
				(a, b) -> { throw new IllegalArgumentException(); },
				LinkedHashMap::new
			));

		return context;
	}

	private ProcessState populateProcessState(RuntimeContext context, Map.Entry<String, Process> entry)
	{
		ProcessState state = ProcessState.builder()
			.id(entry.getKey())
			.desired(ProcessState.Status.RUNNING)
			.process(entry.getValue())
			.build();
		if (checkDisabled(context, state.process))
			state.desired = ProcessState.Status.DISABLED;
		return state;
	}

	private boolean checkDisabled(RuntimeContext context, Process process)
	{
		if (process.isDisabled())
			return true;
		if (Optional.ofNullable(process.getDisableProperty())
			.filter(property -> Boolean.parseBoolean(context.properties.get(property)))
			.isPresent()) {
			return true;
		}
		if (Optional.ofNullable(process.getDisableFile())
			.filter(file -> Files.exists(Paths.get(file)))
			.isPresent()) {
			return true;
		}
		if (Optional.ofNullable(process.getDisableOs())
			.filter(osList -> osList.stream()
				.anyMatch(os -> os.equals(SystemUtils.OS_NAME)))
			.isPresent()) {
			return true;
		}
		return false;
	}

	private void validateSpec(Specification specification)
	{
		for (Process process: specification.getProcesses().values()) {
			Preconditions.checkArgument((process.getCommand() == null) != (process.getShellCommand() == null), "Only one of command and shellCommand may be specified");
			Preconditions.checkArgument(process.getStartTimeMs() >= 0, "startTimeMs must be non-negative");
			Preconditions.checkArgument(process.getRestartDelayMs() >= 0, "restartDelayMs must be non-negative");
		}
	}

	private class RuntimeContext implements Context
	{
		Map<String, String> properties;

		Map<String, ProcessState> processes;

		/** Pending finished tasks to process.  In case it's null, doNext can be scheduled, otherwise it already is. */
		final SingleConsumerQueue<Runnable> pendingTasks = new SingleConsumerQueue<>(() ->
			CompletableFuture.runAsync(() -> doNext(RuntimeContext.this))
				.whenComplete((v, ex) -> {
					if (ex != null) {
						log.fatal(ex);
					}
				})
		);

		boolean needReview = true;

		boolean cancelling = false;

		CompletableFuture<Void> exitFuture = new CompletableFuture<>();

		volatile Pair<Specification, CompletableFuture<Void>> reloadRequest;

		@Override
		public CompletableFuture<Void> waitExit()
		{
			return exitFuture;
		}

		@Override
		public CompletableFuture<Void> cancel()
		{
			scheduleStep(this, () -> {
				if (!cancelling) {
					cancelling = true;
					for (ProcessState state : processes.values()) {
						if (!state.isDesiredInactive())
							state.desired = ProcessState.Status.STOPPED;
					}
					needReview(this);
				}
			});
			return exitFuture;
		}

		@Override
		public CompletableFuture<Void> reload(Specification specification)
		{
			CompletableFuture<Void> future;
			for (;;) {
				Pair<Specification, CompletableFuture<Void>> localRequest = reloadRequest;
				future = Optional.ofNullable(localRequest)
					.map(Pair::getRight)
					.orElseGet(CompletableFuture::new);
				Pair<Specification, CompletableFuture<Void>> newRequest = new ImmutablePair<>(
					specification, future
				);
				if (RELOAD_REQUEST_UPDATER.compareAndSet(this, localRequest, newRequest))
					break;
			}
			scheduleStep(this, () -> {
				handleReload(RuntimeContext.this);
			});
			return future;
		}
	}

	@SuppressWarnings("unchecked")
	public static final AtomicReferenceFieldUpdater<RuntimeContext, Pair<Specification, CompletableFuture<Void>>> RELOAD_REQUEST_UPDATER =
		(AtomicReferenceFieldUpdater<RuntimeContext, Pair<Specification, CompletableFuture<Void>>>)(Object)
			AtomicReferenceFieldUpdater.newUpdater(RuntimeContext.class, Pair.class, "reloadRequest");

	@Builder(builderClassName = "Builder")
	private static class ProcessState
	{
		public boolean isDesiredInactive()
		{
			return desired == Status.REMOVE || desired == Status.DISABLED;
		}

		enum Status
		{
			STOPPED,
			STARTING,
			RUNNING,
			DISABLED,
			TERMINATING,
			RESTART,
			REMOVE,
		};

		String id;
		Status status;
		Status desired;
		Process process;
		TaskHandle taskHandle;

		long lastStart;

		boolean isUp()
		{
			return status == Status.RUNNING || status == Status.DISABLED;
		}

		boolean isDown()
		{
			return status == Status.STOPPED || status == Status.DISABLED;
		}

		public static class Builder
		{
			Status status = Status.STOPPED;
		}
	}
}
