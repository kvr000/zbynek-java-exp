package cz.znj.kvr.sw.exp.java.process.processwatcher.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Process specification.
 */
@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Process.Builder.class)
public class Process
{
	/**
	 * command to execute, as array:
	 */
	@Nullable
	List<String> command;

	/**
	 * command to execute, as shell command:
	 */
	@Nullable
	String shellCommand;

	/**
	 * Start time, the process is supposed to be started after the period of time.
	 */
	long startTimeMs;

	/**
	 * Restart delay, applies from the last start time.
	 */
	long restartDelayMs;

	/**
	 * Terminate time, until kill is sent, default 10000 ms.
	 */
	long terminateTimeMs;

	/**
	 * Indicates the process is disabled.
	 */
	boolean disabled;

	/**
	 * Indicates the process is disabled by external property.
	 */
	String disableProperty;

	/**
	 * Indicates the process is disabled by operating system.
	 */
	Set<String> disableOs;

	/**
	 * Indicates the process is disabled by existence of file.
	 */
	String disableFile;

	/**
	 * Dependencies list, the process should wait until those dependencies are up.
	 */
	List<String> dependencies;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
		long startTimeMs = 4_000;

		long restartDelayMs = 10_000;

		long terminateTimeMs = 10_000;

		List<String> dependencies = Collections.emptyList();
	}
}
