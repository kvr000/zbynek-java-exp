package cz.znj.kvr.sw.exp.java.process.jobrunner.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;


/**
 * Job task.
 */
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = JobTask.Builder.class)
public class JobTask
{
	/**
	 * command to execute
	 */
	@NonNull
	List<String> command;

	/**
	 * Task dependencies.
	 */
	List<String> dependencies;

	/**
	 * CPU utilization, minimum absolute amount available.
	 */
	float cpuMinimum;

	/**
	 * CPU utilization, portion of all CPUs.
	 */
	float cpuPortion;

	/**
	 * Memory utilization, minimum absolute amount available, in MB
	 */
	int memoryMinimum;

	/**
	 * Memory utilization, portion of all memory.
	 */
	float memoryPortion;

	/**
	 * Machine group to run on, can be empty to allow any.
	 */
	String machineGroup;

	/**
	 * Machine groups to run on, can be empty to allow any.
	 */
	List<String> machineGroups;

	/**
	 * Indicates whether to run job on all hosts within the machine group.
	 */
	boolean runAllHosts;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
		List<String> dependencies = Collections.emptyList();

		List<String> machineGroups;
	}
}
