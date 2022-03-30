package cz.znj.kvr.sw.exp.java.netty.netty4.proxy;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.NettyHttpProxyFactory;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.NettyPortForwarder;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.PortForwarder;
import lombok.RequiredArgsConstructor;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactory;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import net.dryuf.cmdline.command.RootCommandContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
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

	private final PortForwarder portForwarder;
	private final HttpProxyFactory httpProxyFactory;

	private Map<String, String> proxyRemap = new LinkedHashMap<>();
	private Map<String, String> proxyHeaders = new LinkedHashMap<>();

	private List<PortForwarder.ForwardConfig> forwards = new ArrayList<>();

	private List<HttpProxyFactory.Config> httpProxies = new ArrayList<>();

	public static void main(String[] args)
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(ProxyRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "httpproxy", null),
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
			PortForwarder.ForwardConfig config = PortForwarder.ForwardConfig.builder()
				.bind(parseAddressSpec(needArgsParam(null, args), false))
				.connect(parseAddressSpec(needArgsParam(null, args), true))
				.build();
			this.forwards.add(config);
			return true;

		case "--proxy-remap":
			String value = needArgsParam(null, args);
			String[] lr = value.split("=", 2);
			if (lr.length != 2) {
				throw new IllegalArgumentException("Need oldhost=newhost mapping");
			}
			proxyRemap.put(lr[0], lr[1]);
			return true;

		case "--proxy-header":
			String header = needArgsParam(null, args);
			String[] headerSplit = header.split(":", 2);
			if (headerSplit.length != 2) {
				throw new IllegalArgumentException("Need key: value mapping");
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

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (this.forwards.isEmpty() && this.httpProxies.isEmpty()) {
			return usage(context, "nothing to run, type -h for help");
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected String configHelpTitle(CommandContext context)
	{
		return "ProxyRunner - runs port forwards or http proxy";
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"-f,--forward proto:host:port proto:host:port", "forwards first argument to second argument, proto can be one of tcp4, tcp6",
			"--proxy-remap oldhost[:port]=newhost[:port]", "remaps request from oldhost to newhost",
			"--proxy-header name:value", "adds header to HTTP requests",
			"--proxy-reset", "reset proxy settings for next instance",
			"-p,--proxy [host:]port", "runs proxy on specified host and port"
		);
	}

	@Override
	public int execute() throws Exception
	{
		List<CompletableFuture<CompletableFuture<Void>>> tasks = new ArrayList<>();
		forwards.forEach(config -> tasks.add(portForwarder.runForward(config)));
		httpProxies.forEach(config -> tasks.add(httpProxyFactory.runProxy(config)));
		NettyFutures.nestedAllOrCancel(tasks).get().get();
		return EXIT_SUCCESS;
	}

	private PortForwarder.ForwardConfig.AddressSpec parseAddressSpec(String spec, boolean isConnect)
	{
		Matcher m = ADDRESS_SPEC_PATTERN.matcher(spec);
		if (!m.matches()) {
			throw new IllegalArgumentException("Failed to parse address specification, it must be in form {tcp4|tcp6}:[host:]port or {unix|domain}:path "+spec);
		}
		PortForwarder.ForwardConfig.AddressSpec.Builder builder =
			PortForwarder.ForwardConfig.AddressSpec.builder();
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
		PortForwarder.ForwardConfig.AddressSpec addressSpec = parseAddressSpec(spec, false);
		return HttpProxyFactory.Config.builder()
			.proto(addressSpec.getProto())
			.listenAddress(InetSocketAddress.createUnresolved(Optional.ofNullable(addressSpec.getHost()).orElse("*"), addressSpec.getPort()))
			.remapHosts(proxyRemap)
			.addedHeaders(proxyHeaders)
			.build();
	}

	public static class GuiceModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(NettyRuntime.class).in(Singleton.class);
			bind(PortForwarder.class).to(NettyPortForwarder.class).in(Singleton.class);
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
