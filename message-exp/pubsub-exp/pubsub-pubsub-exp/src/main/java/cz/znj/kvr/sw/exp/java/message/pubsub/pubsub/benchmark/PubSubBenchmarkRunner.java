package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.ServerUrlInject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractParentCommand;
import net.dryuf.cmdline.command.Command;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Jedis pub-sub benchmark runner.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class PubSubBenchmarkRunner extends AbstractParentCommand
{
	private final Options options;

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(PubSubBenchmarkRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "pubsub-jedis-benchmark", null),
				Arrays.asList(args0)
			);
		});
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (options.redisUrl == null) {
			return usage(context, "-r must be specified");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "-r":
			this.options.redisUrl = needArgsParam(null, args);
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	protected CommandContext createChildContext(CommandContext commandContext, String name, boolean isHelp)
	{
		return commandContext.createChild(this, name, ImmutableMap.of(
		));
	}

	@Override
	protected Map<String, Class<? extends Command>> configSubCommands(CommandContext context)
	{
		return ImmutableMap.of(
			"jedis-subscribe", JedisSubscribeCommand.class,
			"jedis-publish-singleswork", JedisSinglesWorkPoolPublishReceiveBenchmark.class,
			"jedis-publish-workpool", JedisWorkPoolPublishReceiveBenchmark.class,
			"jedis-publish-pooled", JedisPooledPublishReceiveBenchmark.class,
			"lettuce-subscribe", LettuceSubscribeCommand.class,
			"lettuce-publish", LettucePublishReceiveBenchmark.class,
			"rabbitmq-reactor-publish", RabbitMqReactorPublishReceiveBenchmark.class,
			"rabbitmq-amqp-publish", RabbitMqAmqpPublishReceiveBenchmark.class,
			"rabbitmq-amqp-subscribe", RabbitMqAmqpSubscribeCommand.class
		);
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"-r", "Redis server address"
		);
	}

	@Override
	protected Map<String, String> configCommandsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"jedis-subscribe", "Run subscriber Jedis",
			"jedis-publish-singleswork", "Run publisher singleswork",
			"jedis-publish-workpool", "Run publisher workpool",
			"jedis-publish-pooled", "Run publisher pooled",
			"lettuce-subscribe", "Run subscriber Lettuce",
			"lettuce-publish", "Run publisher Lettuce",
			"rabbitmq-reactor-publish", "Run RabbitMq-reactor publish",
			"rabbitmq-amqp-publish", "Run RabbitMq-amqp publish",
			"rabbitmq-amqp-subscribe", "Run RabbitMq-amqp subscribe"
		);
	}

	public static class Options
	{
		String redisUrl = "redis://localhost:7207";
	}

	public static class GuiceModule extends AbstractModule
	{
		private Options options = new Options();

		@Override
		public void configure()
		{
		}

		@Provides
		@Singleton
		public BeanFactory beanFactory(Injector injector)
		{
			return new GuiceBeanFactory(injector);
		}

		@Provides
		@Singleton
		public ExecutorService blockingExecutor()
		{
			return Executors.newCachedThreadPool((runnable) -> {
				Thread t = new Thread(runnable);
				t.setDaemon(true);
				return t;
			});
		}

		@Provides
		@Singleton
		public Options options()
		{
			return options;
		}

		@Provides
		@Singleton
		@ServerUrlInject
		public String redisUrl()
		{
			return options.redisUrl;
		}
	}
}
