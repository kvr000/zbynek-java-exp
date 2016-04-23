package cz.znj.kvr.sw.exp.java.process.jobrunner.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import cz.znj.kvr.sw.exp.java.process.jobrunner.JobRunner;
import lombok.Builder;
import lombok.Value;


/**
 * Machine specification.
 */
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Machine.Builder.class)
public class Machine
{
	/** Agent connection.  Only "ssh" is supported. */
	String agent;

	/** Address, like user@host for SSH. */
	String address;

	/** Number of CPUs. */
	int cpus;

	/** Memory size in MB. */
	int memory;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
	}
}
