package cz.znj.kvr.sw.exp.java.process.jobrunner.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.List;


/**
 * Machine specification.
 */
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = MachineGroup.Builder.class)
public class MachineGroup
{
	List<String> machines;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
	}
}
