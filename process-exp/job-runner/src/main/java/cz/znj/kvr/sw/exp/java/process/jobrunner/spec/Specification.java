package cz.znj.kvr.sw.exp.java.process.jobrunner.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Main specification.
 */
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Specification.Builder.class)
public class Specification
{
	@NonNull
	Map<String, JobTask> tasks;

	@NonNull
	Map<String, MachineGroup> machineGroups;

	@NonNull
	Map<String, Machine> machines;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
		@JsonDeserialize(as = LinkedHashMap.class)
		Map<String, JobTask> tasks = new LinkedHashMap<>();

		@JsonDeserialize(as = LinkedHashMap.class)
		Map<String, MachineGroup> machineGroups = new LinkedHashMap<>();

		@JsonDeserialize(as = LinkedHashMap.class)
		Map<String, Machine> machines = new LinkedHashMap<>();
	}
}
