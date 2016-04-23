package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import com.google.common.collect.ImmutableList;
import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * SSH execution Agent.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SshExecutionAgent implements ExecutionAgent
{
	private final LocalExecutionAgent localExecutionAgent;

	@Override
	public CompletableFuture<Integer> execute(Machine machine, List<String> command)
	{
		List<String> fullCommand = ImmutableList.<String>builder()
			.add("ssh", "-o", "ServerAliveInterval=270", machine.getAddress())
			.addAll(command)
			.build();
		return localExecutionAgent.execute(machine, fullCommand);
	}
}
