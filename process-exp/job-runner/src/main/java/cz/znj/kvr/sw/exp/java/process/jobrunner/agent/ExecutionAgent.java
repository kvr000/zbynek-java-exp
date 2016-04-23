package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Execution agent.
 */
public interface ExecutionAgent
{
	/**
	 * Executes the command.
	 *
	 * @param machine
	 * 	machine to execute on.
	 * @param command
	 * 	command to execute.
	 *
	 * @return
	 * 	future with process result.
	 */
	CompletableFuture<Integer> execute(Machine machine, List<String> command);
}
