package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Local execution Agent.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LocalExecutionAgent implements ExecutionAgent
{
	@Override
	public CompletableFuture<Integer> execute(Machine machine, List<String> command)
	{
		CompletableFuture<Integer> future = new CompletableFuture<Integer>() {
			Process process;

			{
				try {
					this.process = new ProcessBuilder()
						.command(command)
						.redirectOutput(ProcessBuilder.Redirect.INHERIT)
						.redirectError(ProcessBuilder.Redirect.INHERIT)
						.start();
					process.onExit()
						.whenComplete((process1, ex) -> {
							if (ex != null) {
								completeExceptionally(ex);
							}
							else {
								complete(process1.exitValue());
							}
						});
				}
				catch (Throwable ex) {
					completeExceptionally(ex);
				}
			}
		};
		return future;
	}
}
