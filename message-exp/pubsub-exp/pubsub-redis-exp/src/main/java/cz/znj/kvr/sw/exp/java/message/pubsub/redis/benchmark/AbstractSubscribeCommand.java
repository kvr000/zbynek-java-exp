package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;

import javax.inject.Inject;
import java.util.ListIterator;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public abstract class AbstractSubscribeCommand extends AbstractCommand
{
	protected final Options options = new Options();

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

	public static class Options
	{
		int count;
	}
}
