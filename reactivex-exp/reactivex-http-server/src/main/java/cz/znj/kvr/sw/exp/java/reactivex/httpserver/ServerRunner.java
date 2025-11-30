package cz.znj.kvr.sw.exp.java.reactivex.httpserver;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;
import rx.Observable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Proxy runner.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ServerRunner extends AbstractCommand
{

	private List<SocketAddress> httpServers = new ArrayList<>();

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(ServerRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "reactivex-http-server", null),
				Arrays.asList(args0)
			);
		});
	}

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "--http-server":
			this.httpServers.add(parseServerAddress(needArgsParam(null, args)));
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (this.httpServers.isEmpty()) {
			return usage(context, "nothing to run, type -h for help");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return context.getCommandPath() + "- runs http server";
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"-http-server host:port", "runs simple HTTP server on specified host and port"
		);
	}

	@Override
	public int execute() throws Exception
	{
		List<CompletableFuture<HttpServer<ByteBuf, ByteBuf>>> tasks = new ArrayList<>();
		httpServers.forEach(config -> tasks.add(runServer(config)));
		tasks.forEach(task -> {
			task.join().awaitShutdown();
		});
		return EXIT_SUCCESS;
	}

	private SocketAddress parseServerAddress(String spec)
	{
		HostAndPort hp = HostAndPort.fromString(spec);
		return new InetSocketAddress(hp.getHost(), hp.getPort());
	}

	private CompletableFuture<HttpServer<ByteBuf, ByteBuf>> runServer(SocketAddress address)
	{
		log.info("Listening on: {}", address);
		return CompletableFuture.completedFuture(
			HttpServer.newServer(address)
				.start((req, resp) -> {
					return resp.writeStringAndFlushOnEach(Observable.just("Hello World\n"));
				})
		);
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
