package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor;

import cz.znj.kvr.sw.exp.java.process.jobrunner.agent.WrappingExecutionAgent;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.JobTask;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.MachineGroup;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Specification;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.queue.SingleConsumerQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


/**
 * Runtime based JobExecutor, maintaining all information in runtime.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
@Singleton
public class RuntimeJobExecutor implements JobExecutor
{
	private final WrappingExecutionAgent executionAgent;

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

	private void doNext(Context context)
	{
		try (SingleConsumerQueue<TaskState>.Consumer consumer = context.pendingTasks.consume()) {
			TaskState lastFailed = null;
			for (TaskState taskState; (taskState = consumer.next()) != null; ) {
				log.trace("Processing: {}", taskState.id);
				taskState.machine.availableCpus += taskState.usedCpus;
				taskState.machine.availableMemory += taskState.usedMemory;
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
				if (taskState.exitCode != 0) {
					log.fatal("Command failed: id={} command={} exit={}", taskState.id, taskState.task.getCommand(), taskState.exitCode);
					if (lastFailed == null) {
						lastFailed = taskState;
					}
				}
			}

			if (lastFailed != null) {
				context.result.complete(lastFailed.exitCode);
				return;
			}

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
				TaskState taskState = findMachine(context, entry);
				if (taskState != null) {
					taskState.process = runTask(context, entry, taskState);
					taskState.process.whenComplete((exit, ex) -> {
							if (ex != null)
								log.error("Error running: {}", entry.getKey(), ex);
							finished(context, taskState, exit == null ? 255 : exit);
						});
					readyIterator.remove();
					context.running.put(entry.getKey(), taskState);
				}
			}
			if (context.running.isEmpty()) {
				throw new IllegalStateException("No running processes but none can be executed, currently ready: ready="+context.ready);
			}
		}
		catch (Throwable ex) {
			context.result.completeExceptionally(ex);
		}
	}

	private void finished(Context context, TaskState taskState, int exitCode)
	{
		taskState.exitCode = exitCode;
		log.debug("Finishing: {}", taskState.id);
		context.pendingTasks.add(taskState);
	}

	private void scheduleDoNext(Context context)
	{
		log.trace("Scheduling doNext");
		CompletableFuture.runAsync(() -> doNext(context))
			.whenComplete((v, ex) -> {
				if (ex != null) {
					log.fatal(ex);
					context.result.completeExceptionally(ex);
				}
			});
	}

	private TaskState findMachine(Context context, Map.Entry<String, JobTask> taskEntry)
	{
		JobTask task = taskEntry.getValue();
		Map<String, MachineState> machines = Optional.ofNullable(task.getMachineGroup())
			.map(group -> context.machineGroups.get(group))
			.map(group -> (Collection<String>) group.getMachines())
			.map(groupMachines -> (Map<String, MachineState>) groupMachines.stream()
					.map(machine -> new AbstractMap.SimpleImmutableEntry<>(machine, context.machines.get(machine)))
					.filter(entry -> entry.getValue() != null)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
				)
			.orElseGet(() -> context.machines);
		for (Map.Entry<String, MachineState> machineEntry: machines.entrySet()) {
			MachineState machine = machineEntry.getValue();
			int cpus;
			if (task.getCpu() < 1) {
				cpus = (int) Math.ceil(task.getCpu()*machine.machine.getCpus());
			}
			else {
				cpus = (int) task.getCpu();
			}
			if (cpus > machine.availableCpus)
				continue;
			int memory;
			if (task.getMemory() < 1) {
				memory = (int) Math.ceil(task.getMemory()*machine.machine.getMemory());
			}
			else {
				memory = (int) task.getMemory();
			}
			if (memory > machine.availableMemory)
				continue;

			machine.availableCpus -= cpus;
			machine.availableMemory -= memory;
			return TaskState.builder()
				.id(taskEntry.getKey())
				.task(task)
				.machine(machine)
				.usedCpus(cpus)
				.usedMemory(memory)
				.build();
		}
		return null;
	}

	private CompletableFuture<Integer> runTask(Context context, Map.Entry<String, JobTask> entry, TaskState taskState)
	{
		log.info("Running: "+entry.getKey());
		return executionAgent.execute(taskState.machine.machine, entry.getValue().getCommand());
	}

	private Context populateContext(Specification specification)
	{
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
					.machine(entry.getValue())
					.availableCpus(entry.getValue().getCpus())
					.availableMemory(entry.getValue().getMemory())
					.build()
			))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

		context.pendingTasks = new SingleConsumerQueue<>(() -> scheduleDoNext(context));
		return context;
	}

	private static class Context
	{
		Map<String, JobTask> tasks;

		Map<String, MachineGroup> machineGroups;

		Map<String, MachineState> machines;

		Map<String, JobTask> finished;

		Map<String, TaskState> running;

		Map<String, JobTask> ready;

		Map<String, JobTask> blocked;

		Map<String, Map<String, JobTask>> reverseDependent;

		CompletableFuture<Integer> result;

		SingleConsumerQueue<TaskState> pendingTasks;
	}

	@Builder
	private static class MachineState
	{
		Machine machine;
		int availableCpus;
		int availableMemory;
	}

	@Builder
	private static class TaskState
	{
		String id;
		JobTask task;
		MachineState machine;
		int usedCpus;
		int usedMemory;
		int exitCode;
		CompletableFuture<Integer> process;
	}
}
