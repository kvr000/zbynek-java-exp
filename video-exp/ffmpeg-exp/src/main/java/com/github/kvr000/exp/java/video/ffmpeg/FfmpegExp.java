package com.github.kvr000.exp.java.video.ffmpeg;

import com.github.kvr000.exp.java.video.ffmpeg.command.MakeOverlayCommand;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractParentCommand;
import net.dryuf.cmdline.command.Command;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;


@Log4j2
public class FfmpegExp extends AbstractParentCommand
{
	private MainOptions mainOptions;

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(FfmpegExp.class).run(
				new RootCommandContext(appContext).createChild(null, "ffmpeg-exp", null),
				Arrays.asList(args0)
			);
		});
	}

	protected CommandContext createChildContext(CommandContext commandContext, String name, boolean isHelp)
	{
		return commandContext.createChild(this, name, Map.of(MainOptions.class, mainOptions));
	}

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "--vo":
			mainOptions.videoOutput = needArgsParam(mainOptions.videoOutput, args);
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	public void createOptions(CommandContext context)
	{
		this.mainOptions = new MainOptions();
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return "ffmpeg-exp - various Ffmpeg experiments";
	}

	@Override
	protected Map<String, Class<? extends Command>> configSubCommands(CommandContext context)
	{
		return ImmutableMap.of(
			"make-overlay", MakeOverlayCommand.class
		);
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"--vo", "video output file"
		);
	}

	@Override
	protected Map<String, String> configCommandsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"make-overlay", "Creates overlay video",
			"help [command]", "Prints help"
		);
	}

	@Data
	public static class MainOptions
	{
		String videoOutput;
	}

	public static class GuiceModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
		}

		@Provides
		@Singleton
		public BeanFactory beanFactory(Injector injector)
		{
			return new GuiceBeanFactory(injector);
		}
	}
}
