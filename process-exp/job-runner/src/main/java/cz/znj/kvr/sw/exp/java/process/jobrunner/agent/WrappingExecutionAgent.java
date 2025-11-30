package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Wrapping proxy execution Agent.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class WrappingExecutionAgent implements ExecutionAgent
{
	private final Map<String, ExecutionAgent> agents;

	@Override
	public CompletableFuture<Integer> execute(Machine machine, List<String> command)
	{
		return Optional.ofNullable(agents.get(machine.getAgent()))
			.orElseThrow(() -> new UnsupportedOperationException("Unsupported agent: "+machine.getAgent()))
			.execute(machine, command);

	}
}
