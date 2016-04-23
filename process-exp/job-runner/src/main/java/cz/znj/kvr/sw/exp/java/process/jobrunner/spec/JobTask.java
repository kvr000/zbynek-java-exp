package cz.znj.kvr.sw.exp.java.process.jobrunner.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import cz.znj.kvr.sw.exp.java.process.jobrunner.JobRunner;
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
	 * CPU utilization: < 1 - fraction of all CPUs, 1 - one CPU, 1000000 - all CPUs
	 */
	float cpu;
	/**
	 * Memory utilization: <= 1 - fraction of memory taken, > 1 - memory utilization in MB.
	 */
	float memory;

	/**
	 * Machines to run on, can be empty to allow any.
	 */
	String machineGroup;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
		List<String> dependencies = Collections.emptyList();
	}
}
