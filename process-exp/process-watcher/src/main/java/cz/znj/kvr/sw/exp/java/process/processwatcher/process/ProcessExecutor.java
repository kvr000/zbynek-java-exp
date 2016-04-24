package cz.znj.kvr.sw.exp.java.process.processwatcher.process;

import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


/**
 * Process executor.
 */
public class ProcessExecutor
{
	@Named("blockingExecutor")
	private final ExecutorService executorService;

	@Inject
	public ProcessExecutor(@Named("blockingExecutor") ExecutorService executorService)
	{
		this.executorService = executorService;
	}

	public TaskHandle execute(List<String> command)
	{
		return executeBuilder(new ProcessBuilder()
			.command(command)
		);
	}

	public TaskHandle execute(String command)
	{
		if (SystemUtils.IS_OS_WINDOWS) {
			return executeBuilder(new ProcessBuilder()
				.command("cmd", "/c", command)
			);
		}
		else {
			return executeBuilder(new ProcessBuilder()
				.command("sh", "-c", command)
			);
		}
	}

	private TaskHandle executeBuilder(ProcessBuilder builder)
	{
		TaskHandle handle = new TaskHandle()
		{
			Process process;

			CompletableFuture<Integer> exitFuture = new CompletableFuture<>();

			@Override
			public CompletableFuture<Integer> waitExit()
			{
				return exitFuture;
			}

			@Override
			public void terminate()
			{
				if (this.process != null)
					this.process.destroy();
			}

			@Override
			public void kill()
			{
				if (this.process != null)
					this.process.destroyForcibly();
			}

			public TaskHandle initialize()
			{
				try {
					this.process = builder
						.redirectOutput(ProcessBuilder.Redirect.INHERIT)
						.redirectError(ProcessBuilder.Redirect.INHERIT)
						.start();
					executorService.execute(() -> {
						try {
							exitFuture.complete(process.waitFor());
						}
						catch (Throwable ex) {
							exitFuture.completeExceptionally(ex);
						}
					});
				}
				catch (Throwable ex) {
					exitFuture.completeExceptionally(ex);
				}
				return this;
			}
		}.initialize();
		return handle;
	}
}
