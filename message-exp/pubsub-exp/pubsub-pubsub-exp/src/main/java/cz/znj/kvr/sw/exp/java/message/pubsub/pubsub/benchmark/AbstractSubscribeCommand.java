package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.ScheduledUtil;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;

import jakarta.inject.Inject;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public abstract class AbstractSubscribeCommand extends AbstractCommand
{
	private static final AtomicLongFieldUpdater<AbstractSubscribeCommand> RECEIVED_COUNT_UPDATER = AtomicLongFieldUpdater.newUpdater(
		AbstractSubscribeCommand.class, "receivedCount"
	);

	private static final AtomicIntegerFieldUpdater<AbstractSubscribeCommand> RECEIVED_UPDATE_SCHEDULED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(
		AbstractSubscribeCommand.class, "receivedUpdateScheduled"
	);

	protected final Options options = new Options();

	protected volatile long receivedCount = 0;

	protected volatile int receivedUpdateScheduled = 0;

	protected long lastReceivedCount = 0;

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "-c":
			this.options.count = Integer.parseInt(needArgsParam(this.options.count == 0 ? null : 1, args));
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (options.count <= 0) {
			return usage(context, "-c must be positive");
		}
		return EXIT_CONTINUE;
	}

	protected void onReceived(String message)
	{
		RECEIVED_COUNT_UPDATER.incrementAndGet(this);
		if (RECEIVED_UPDATE_SCHEDULED_UPDATER.compareAndSet(this, 0, 1)) {
			ScheduledUtil.sharedExecutor().schedule(
				() -> {
					RECEIVED_UPDATE_SCHEDULED_UPDATER.set(this, 0);
					log.info("Received messages: {}", RECEIVED_COUNT_UPDATER.get(this));
					return null;
				},
				1, TimeUnit.SECONDS
			);
		}
	}

	public static class Options
	{
		int count;
	}
}
