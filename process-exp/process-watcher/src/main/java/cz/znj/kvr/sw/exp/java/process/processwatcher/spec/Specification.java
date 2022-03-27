package cz.znj.kvr.sw.exp.java.process.processwatcher.spec;

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
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Specification.Builder.class)
public class Specification
{
	@NonNull
	Map<String, Process> processes;

	@NonNull
	Map<String, String> properties;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
		@JsonDeserialize(as = LinkedHashMap.class)
		Map<String, Process> processes = new LinkedHashMap<>();

		@JsonDeserialize(as = LinkedHashMap.class)
		Map<String, String> properties = new LinkedHashMap<>();
	}
}
