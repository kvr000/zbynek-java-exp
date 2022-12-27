package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import com.google.common.base.Preconditions;
import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.WrappingExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.JobTask;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.MachineGroup;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.executor.FinishingSequencingExecutor;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Runtime based JobExecutor, maintaining all information in runtime.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
@Singleton
public class RuntimeJobExecutor implements JobExecutor
{
	static final int CPU_MULTIPLY = 1000;

	private final WrappingExecutionAgent executionAgent;

	@Override
	public CompletableFuture<Integer> execute(Specification specification)
	{
		Context context1 = populateContext(specification);
		context1.result = new CompletableFuture<Integer>() {
			@SuppressWarnings("unused") // debugging
			private final Context context = context1;
		};
		scheduleDoNext(context1);
		return context1.result;
	}

	private void doReview(Context context)
	{
		if (context.result.isDone())
			return;
		if (context.firstFailed != null) {
			context.result.complete(context.firstFailed.exitCode);
			return;
		}

		try {
			processReady(context);
		}
		catch (Throwable ex) {
			context.result.completeExceptionally(ex);
		}
	}

	private void doFinished(Context context, TaskState taskState)
	{
		log.trace("Processing: {}", taskState.id);
		taskState.machine.availableCpusMultiplied += taskState.usedCpusMultiplied;
		taskState.machine.availableMemory += taskState.usedMemory;
		if (taskState.statesCounter.decrementAndGet() == 0) {
			context.running.remove(taskState.id);
			context.finished.put(taskState.id, taskState.task);
			Optional.ofNullable(context.reverseDependent.get(taskState.id))
					.ifPresent(map -> map.forEach((depId, depTask) -> {
						if (context.blocked.containsKey(depId) &&
								depTask.getDependencies().stream().allMatch(context.finished::containsKey)) {
							context.blocked.remove(depId);
							context.ready.put(depId, depTask);
						}
					}));
		}
		if (taskState.exitCode != 0) {
			log.fatal("Command failed: id={} machine={} command={} exit={}", taskState.id, taskState.machine.id, taskState.task.getCommand(), taskState.exitCode);
			if (context.firstFailed == null) {
				context.firstFailed = taskState;
			}
		}
	}

	private void processReady(Context context)
	{
		if (context.ready.isEmpty()) {
			if (context.running.isEmpty()) {
				if (context.blocked.isEmpty()) {
					context.result.complete(0);
					return;
				}
				throw new IllegalStateException("No running processes but none can be scheduled, currently blocked: finished="+context.finished+" blocked="+context.blocked);
			}
		}
		for (Iterator<Map.Entry<String, JobTask>> readyIterator = context.ready.entrySet().iterator(); readyIterator.hasNext(); ) {
			Map.Entry<String, JobTask> entry = readyIterator.next();
			Collection<TaskState> taskStates = allocateMachine(context, entry);
			if (taskStates != null) {
				taskStates.forEach(taskState -> {
					taskState.process = runTask(context, entry, taskState);
					taskState.process.whenComplete((exit, ex) -> {
						if (ex != null)
							log.error("Error running: task={} machine={}", entry.getKey(), taskState.machine.id, ex);
						finished(context, taskState, exit == null ? 255 : exit);
					});

				});
				readyIterator.remove();
				context.running.put(entry.getKey(), taskStates);
			}
		}
		if (context.running.isEmpty()) {
			throw new IllegalStateException("No running processes but none can be executed, currently ready: ready="+context.ready);
		}
	}

	private void finished(Context context, TaskState taskState, int exitCode)
	{
		taskState.exitCode = exitCode;
		log.debug("Finishing: {}", taskState.id);
		context.taskExecutor.execute(() -> doFinished(context, taskState));
	}

	private void scheduleDoNext(Context context)
	{
		log.trace("Scheduling doNext");
		CompletableFuture.runAsync(() -> doReview(context));
	}

	private Collection<TaskState> allocateMachine(Context context, Map.Entry<String, JobTask> taskEntry)
	{
		Map<String, TaskState> tasks = findMachine(context, taskEntry);
		if (tasks == null || tasks.values().stream().anyMatch(Objects::isNull))
			return null;

		tasks.values().forEach((task) -> {
			task.machine.availableCpusMultiplied -= task.usedCpusMultiplied;
			task.machine.availableMemory -= task.usedMemory;
		});

		return tasks.values();
	}

	/**
	 * Finds machine for the task.
	 *
	 * @param context
	 * 	execution context
	 * @param taskEntry
	 * 	task
	 *
	 * @return
	 * 	map of machine entries to taskState, taskState can be null in case the machine is not eligible
	 */
	private Map<String, TaskState> findMachine(Context context, Map.Entry<String, JobTask> taskEntry)
	{
		JobTask task = taskEntry.getValue();
		Map<String, MachineState> machines = task.getMachineGroup() == null && task.getMachineGroups() == null ?
			context.machines :
			Optional.ofNullable(task.getMachineGroup()).map(Collections::singletonList)
				.orElseGet(task::getMachineGroups).stream()
				.map(group -> context.machineGroups.get(group))
				.flatMap(group -> group.getMachines().stream())
				.collect(Collectors.toMap(
					Function.identity(),
					machine -> context.machines.get(machine),
					(a, b) -> a,
					LinkedHashMap::new
				));
		if (task.isRunAllHosts()) {
			Map<String, TaskState> taskStates = new LinkedHashMap<>();
			AtomicInteger statesCounter = new AtomicInteger(machines.size());
			for (Map.Entry<String, MachineState> machineEntry : machines.entrySet()) {
				TaskState taskState = checkRunningEligibility(taskEntry, machineEntry.getValue());
				if (taskState == null) {
					taskStates.put(machineEntry.getKey(), null);
				}
				else {
					taskState.statesCounter = statesCounter;
					taskStates.put(machineEntry.getKey(), taskState);
				}
			}
			return taskStates;
		}
		else {
			for (Map.Entry<String, MachineState> machineEntry : machines.entrySet()) {
				TaskState taskState = checkRunningEligibility(taskEntry, machineEntry.getValue());
				if (taskState != null) {
					taskState.statesCounter = new AtomicInteger(1);
					return Collections.singletonMap(taskState.machine.id, taskState);
				}
			}
		}
		return null;
	}

	private TaskState checkRunningEligibility(Map.Entry<String, JobTask> taskEntry, MachineState machine)
	{
		JobTask task = taskEntry.getValue();
		int cpusMultiplied = (int)(Math.max(task.getCpuMinimum(), task.getCpuPortion()*machine.machine.getCpus())*CPU_MULTIPLY);
		if (cpusMultiplied > machine.availableCpusMultiplied)
			return null;
		int memory = (int)Math.max(task.getMemoryMinimum(), task.getMemoryPortion()*machine.machine.getMemory());
		if (memory > machine.availableMemory)
			return null;

		return TaskState.builder()
			.id(taskEntry.getKey())
			.task(task)
			.machine(machine)
			.usedCpusMultiplied(cpusMultiplied)
			.usedMemory(memory)
			.build();
	}

	private CompletableFuture<Integer> runTask(Context context, Map.Entry<String, JobTask> entry, TaskState taskState)
	{
		log.info("Running: task={} machine={}", entry.getKey(), taskState.machine.id);
		return executionAgent.execute(taskState.machine.machine, entry.getValue().getCommand());
	}

	private Context populateContext(Specification specification)
	{
		Preconditions.checkArgument(specification.getTasks() != null, "Must not be null: tasks");
		Preconditions.checkArgument(specification.getMachines() != null, "Must not be null: machines");
		Preconditions.checkArgument(specification.getMachineGroups() != null, "Must not be null: machineGroups");

		Context context = new Context();
		context.tasks = specification.getTasks();
		context.finished = new LinkedHashMap<>();
		context.running = new LinkedHashMap<>();
		context.ready = new LinkedHashMap<>();
		context.blocked = new LinkedHashMap<>(context.tasks);
		context.machineGroups = specification.getMachineGroups();
		context.machines = specification.getMachines().entrySet().stream()
			.map((Map.Entry<String, Machine> entry) -> new AbstractMap.SimpleImmutableEntry<String, MachineState>(
				entry.getKey(),
				MachineState.builder()
					.id(entry.getKey())
					.machine(entry.getValue())
					.availableCpusMultiplied(entry.getValue().getCpus()*CPU_MULTIPLY)
					.availableMemory(entry.getValue().getMemory())
					.build()
			))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		validateContext(context);

		context.reverseDependent = new LinkedHashMap<>();
		for (Iterator<Map.Entry<String, JobTask>> taskIterator = context.blocked.entrySet().iterator(); taskIterator.hasNext(); ) {
			Map.Entry<String, JobTask> taskEntry = taskIterator.next();
			if (taskEntry.getValue().getDependencies().isEmpty()) {
				context.ready.put(taskEntry.getKey(), taskEntry.getValue());
				taskIterator.remove();
			}
			else {
				taskEntry.getValue().getDependencies().forEach((String dependency) -> {
					context.reverseDependent.computeIfAbsent(dependency, (key) -> new LinkedHashMap<>())
						.put(taskEntry.getKey(), taskEntry.getValue());
				});
			}
		}

		context.taskExecutor = FinishingSequencingExecutor.createFromFinisher(() -> doReview(context));

		return context;
	}

	private void validateContext(Context context)
	{
		context.machineGroups.forEach((id, group) ->
			group.getMachines().forEach(machine -> {
				if (!context.machines.containsKey(machine)) {
					throw new IllegalArgumentException("Machine group references non-existing machine: machineGroup="+id+" machine="+machine);
				}
			})
		);
		context.blocked.forEach((id, task) -> {
			if (task.getMachineGroup() != null && task.getMachineGroups() != null) {
				throw new IllegalArgumentException("Task references both machineGroup and machineGroups, only one allowed: task="+id);
			}
			Optional.ofNullable(task.getMachineGroups()).ifPresent((groups) -> groups.forEach((group) -> {
				if (!context.machineGroups.containsKey(group)) {
					throw new IllegalArgumentException("Task references non-existing group: task="+id+" machineGroup="+group);
				}
			}));
			Optional.ofNullable(task.getMachineGroup()).ifPresent((group) -> {
				if (!context.machineGroups.containsKey(group)) {
					throw new IllegalArgumentException("Task references non-existing group: task="+id+" machineGroup="+group);
				}
			});
		});
		for (Map.Entry<String, JobTask> jobEntry: context.blocked.entrySet()) {
			Map<String, TaskState> tasks = findMachine(context, jobEntry);
			if (tasks == null || tasks.values().stream().anyMatch(Objects::isNull)) {
				JobTask task = jobEntry.getValue();
				if (task.isRunAllHosts()) {
					throw new IllegalArgumentException("Cannot eventually find all machines for task: task="+jobEntry.getKey()+" ineligible="+tasks.entrySet().stream().filter(e -> e.getValue() == null).map(Map.Entry::getKey).collect(Collectors.toList()));
				}
				else {
					throw new IllegalArgumentException("Cannot eventually find machine for task: task="+jobEntry.getKey());
				}
			}
		}
		Set<String> ready = new HashSet<>();
		for (;;) {
			boolean found = false;
			Map.Entry<String, List<String>> failed = null;
			for (Map.Entry<String, JobTask> jobEntry: context.blocked.entrySet()) {
				String jobId = jobEntry.getKey();
				JobTask task = jobEntry.getValue();
				if (ready.contains(jobId))
					continue;
				if (ready.containsAll(task.getDependencies())) {
					ready.add(jobId);
					found = true;
				}
				else {
					if (failed == null) {
						List<String> unsatisfied = new LinkedList<>(task.getDependencies());
						unsatisfied.removeAll(ready);
						failed = new ImmutablePair<>(jobId, unsatisfied);
					}
				}
			}
			if (!found) {
				if (failed != null) {
					throw new IllegalArgumentException("Task has unsatisfiable dependencies: task="+failed.getKey()+ " unsatisfied="+failed.getValue());
				}
				break;
			}
		}
	}

	private static class Context
	{
		Map<String, JobTask> tasks;

		Map<String, MachineGroup> machineGroups;

		Map<String, MachineState> machines;

		Map<String, JobTask> finished;

		Map<String, Collection<TaskState>> running;

		Map<String, JobTask> ready;

		Map<String, JobTask> blocked;

		Map<String, Map<String, JobTask>> reverseDependent;

		CompletableFuture<Integer> result;

		Executor taskExecutor;

		TaskState firstFailed;
	}

	@Builder
	private static class MachineState
	{
		String id;
		Machine machine;
		/** Available number of CPUs, multiplied by CPU_MULTIPLY */
		int availableCpusMultiplied;
		/** Available memory, in MB */
		int availableMemory;
	}

	@Builder
	private static class TaskState
	{
		String id;
		JobTask task;
		MachineState machine;
		int usedCpusMultiplied;
		int usedMemory;
		/** Number of pending TaskState instances for the same JobTask */
		AtomicInteger statesCounter;
		int exitCode;
		CompletableFuture<Integer> process;
	}
}
