package cz.znj.kvr.sw.exp.java.netty.netty4.proxy;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy.NettyHttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpserver.DummyHttpServerFactory;
import lombok.RequiredArgsConstructor;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;
import net.dryuf.concurrent.FutureUtil;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyEngine;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.forward.NettyPortForwarderFactory;
import net.dryuf.netty.forward.PortForwarderFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Proxy runner.
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ProxyRunner extends AbstractCommand
{
	public static final Pattern ADDRESS_SPEC_PATTERN = Pattern.compile("^(?:(tcp4|tcp6):(?:(.+):)?(\\d+)$|(unix|domain):(.+))$");

	private final NettyEngine nettyEngine;
	private final PortForwarderFactory portForwarderFactory;
	private final HttpProxyFactory httpProxyFactory;
	private final DummyHttpServerFactory dummyHttpServerFactory;

	private Map<String, String> proxyRemap = new LinkedHashMap<>();
	private Map<String, String> proxyHeaders = new LinkedHashMap<>();

	private List<PortForwarderFactory.ForwardConfig> forwards = new ArrayList<>();

	private List<HttpProxyFactory.Config> httpProxies = new ArrayList<>();

	private List<DummyHttpServerFactory.Config> httpServers = new ArrayList<>();

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(ProxyRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "netty-http-proxy", null),
				Arrays.asList(args0)
			);
		});
	}


	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "--forward":
		case "-f":
			PortForwarderFactory.ForwardConfig config = PortForwarderFactory.ForwardConfig.builder()
				.bind(parseAddressSpec(needArgsParam(null, args), false))
				.connect(parseAddressSpec(needArgsParam(null, args), true))
				.build();
			this.forwards.add(config);
			return true;

		case "--proxy-remap":
			String value = needArgsParam(null, args);
			String[] lr = value.split("=", 2);
			if (lr.length != 2) {
				throw new IllegalArgumentException("Need oldhost=newhost mapping: " + value);
			}
			proxyRemap.put(lr[0], lr[1]);
			return true;

		case "--proxy-header":
			String header = needArgsParam(null, args);
			String[] headerSplit = header.split(":", 2);
			if (headerSplit.length != 2) {
				throw new IllegalArgumentException("Need key:value mapping: " + header);
			}
			proxyHeaders.put(headerSplit[0], headerSplit[1]);
			return true;

		case "--proxy-reset":
			proxyRemap.clear();
			proxyHeaders.clear();
			return true;

		case "--proxy":
		case "-p":
			this.httpProxies.add(parseProxySpec(needArgsParam(null, args)));
			return true;

		case "--http-server":
			this.httpServers.add(DummyHttpServerFactory.Config.builder()
				.listenAddress(parseAddressSpec(needArgsParam(null, args), false))
				.build()
			);
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (this.forwards.isEmpty() && this.httpProxies.isEmpty() && this.httpServers.isEmpty()) {
			return usage(context, "nothing to run, type -h for help");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return context.getCommandPath() + "- runs port forwards or http proxy";
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"-f,--forward proto:host:port proto:host:port", "forwards first argument to second argument, proto can be one of tcp4, tcp6",
			"--proxy-remap oldhost[:port]=newhost[:port]", "remaps request from oldhost to newhost",
			"--proxy-header name:value", "adds header to HTTP requests",
			"--proxy-reset", "reset proxy settings for next instance",
			"-p,--proxy proto:[host:]port", "runs proxy on specified host and port",
			"-http-server proto:[host:]port", "runs simple HTTP server on specified host and port"
		);
	}

	@Override
	public int execute() throws Exception
	{
		List<CompletableFuture<Server>> tasks = new ArrayList<>();
		forwards.forEach(config -> tasks.add(portForwarderFactory.runForward(config)));
		httpProxies.forEach(config -> tasks.add(httpProxyFactory.runProxy(config)));
		httpServers.forEach(config -> tasks.add(dummyHttpServerFactory.runServer(config, (m, p) -> "Hello World\n")));
		Server.waitOneAndClose(FutureUtil.nestedAllOrCancel(tasks).get()).get();
		return EXIT_SUCCESS;
	}

	private AddressSpec parseAddressSpec(String spec, boolean isConnect)
	{
		Matcher m = ADDRESS_SPEC_PATTERN.matcher(spec);
		if (!m.matches()) {
			throw new IllegalArgumentException("Failed to parse address specification, it must be in form {tcp4|tcp6}:[host:]port or {unix|domain}:path : "+spec);
		}
		AddressSpec.Builder builder =
			AddressSpec.builder();
		if (m.group(1) != null) {
			String host = m.group(2);
			int port = Integer.parseInt(m.group(3));
			builder.proto(m.group(1));
			builder.host(host);
			builder.port(port);
			if (isConnect && (port <= 0 || host == null)) {
				throw new IllegalArgumentException("Connect specification has invalid host or port: "+spec);
			}
		}
		else {
			builder.proto(m.group(4));
			builder.path(m.group(5));
		}
		return builder.build();
	}

	private HttpProxyFactory.Config parseProxySpec(String spec)
	{
		AddressSpec addressSpec = parseAddressSpec(spec, false);
		return HttpProxyFactory.Config.builder()
			.listenAddress(addressSpec)
			.remapHosts(proxyRemap)
			.addedHeaders(proxyHeaders)
			.build();
	}

	public static class GuiceModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(NettyEngine.class).in(Singleton.class);
			bind(PortForwarderFactory.class).to(NettyPortForwarderFactory.class).in(Singleton.class);
			bind(HttpProxyFactory.class).to(NettyHttpProxyFactory.class).in(Singleton.class);
		}

		@Provides
		@Singleton
		public BeanFactory beanFactory(Injector injector)
		{
			return new GuiceBeanFactory(injector);
		}
	}
}
