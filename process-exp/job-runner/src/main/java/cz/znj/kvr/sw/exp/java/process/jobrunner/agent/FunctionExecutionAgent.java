package cz.znj.kvr.sw.exp.java.process.jobrunner.agent;

import cz.znj.kvr.sw.exp.java.process.jobrunner.spec.Machine;
import lombok.RequiredArgsConstructor;
import net.dryuf.cmdline.app.BeanFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Local execution Agent.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class FunctionExecutionAgent implements ExecutionAgent
{
	private final BeanFactory beanFactory;
	private final ExecutorService executor;

	@Override
	public CompletableFuture<Integer> execute(Machine machine, List<String> command)
	{
		CompletableFuture<Integer> future = new CompletableFuture<Integer>()
		{
			public CompletableFuture<Integer> initialize()
			{
				try {
					String clazzName = command.get(0);
					@SuppressWarnings("unchecked")
					Class<Function<String[], ?>> clazz = (Class<Function<String[], ?>>) this.getClass().getClassLoader().loadClass(clazzName);
					Function<String[], ?> function = beanFactory.getBean(clazz);
					Consumer<Object> completorFinal = (result) -> {
						if (result instanceof Integer) {
							complete((Integer) result);
						}
						else if (result == null) {
							completeExceptionally(new UnsupportedOperationException("Unexpected return type from function, only CompletableFuture<Integer> or Integer are supported: function="+clazz.getName()+" result=null"));
						}
						else {
							completeExceptionally(new UnsupportedOperationException("Unexpected return type from function, only CompletableFuture<Integer> or Integer are supported: function="+clazz.getName()+" type="+result.getClass().getName()));
						}
					};
					executor.execute(() -> {
						Object result = function.apply(command.subList(1, command.size()).toArray(new String[0]));
						if (result instanceof CompletableFuture) {
							((CompletableFuture<?>) result).whenComplete((v, ex) -> {
								if (ex != null) {
									completeExceptionally(ex);
								}
								else {
									completorFinal.accept(result);
								}
							});
						}
						else {
							completorFinal.accept(result);
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
