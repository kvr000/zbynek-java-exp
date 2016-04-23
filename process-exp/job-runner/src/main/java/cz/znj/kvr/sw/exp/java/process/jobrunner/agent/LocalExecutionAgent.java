package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


/**
 * Local execution Agent.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LocalExecutionAgent implements ExecutionAgent
{
	private final ExecutorService executorService;

	@Override
	public CompletableFuture<Integer> execute(Machine machine, List<String> command)
	{
		CompletableFuture<Integer> future = new CompletableFuture<Integer>() {
			Process process;

			public CompletableFuture<Integer> initialize()
			{
				try {
					this.process = new ProcessBuilder()
						.command(command)
						.redirectOutput(ProcessBuilder.Redirect.INHERIT)
						.redirectError(ProcessBuilder.Redirect.INHERIT)
						.start();
					executorService.execute(() -> {
						try {
							complete(process.waitFor());
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					});
				}
				catch (Throwable ex) {
					completeExceptionally(ex);
				}
				return this;
			}
		}.initialize();
		return future;
	}
}
