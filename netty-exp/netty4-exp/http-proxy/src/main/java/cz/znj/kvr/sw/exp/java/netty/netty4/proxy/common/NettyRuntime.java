package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.pipeline.ForwarderHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;
import org.apache.commons.lang3.SystemUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * Netty runtime core.
 */
@Log4j2
public class NettyRuntime implements Closeable
{
	public static Map<String, Class<? extends InetAddress>> PROTO_TO_ADDRESS_CLASS = ImmutableMap.<String, Class<? extends InetAddress>>builder()
		.put("udp4", Inet4Address.class)
		.put("tcp4", Inet4Address.class)
		.put("ip4", Inet4Address.class)
		.put("udp6", Inet6Address.class)
		.put("tcp6", Inet6Address.class)
		.put("ip6", Inet6Address.class)
		.build();

	private ConfigAdapter configAdapter = createConfigAdapter();

	@Getter
	private EventLoopGroup bossGroup = configAdapter.createEventLoopGroup();
	@Getter
	private EventLoopGroup workerGroup = configAdapter.createEventLoopGroup();

	@Getter
	private final InetNameResolver inetNameResolver = new DnsNameResolverBuilder()
		.eventLoop(workerGroup.next())
		.channelFactory(configAdapter.getDatagramChannel(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)))
		.build();

	public CompletableFuture<SocketAddress> resolve(SocketAddress address)
	{
		if (address instanceof InetSocketAddress && ((InetSocketAddress) address).isUnresolved()) {
			InetSocketAddress address1 = (InetSocketAddress) address;
			if (address1.getHostName().equals("*")) {
				return CompletableFuture.completedFuture(new InetSocketAddress(address1.getPort()));
			}
			Future<InetAddress> future =
				getInetNameResolver().resolve(((InetSocketAddress)address).getHostName());
			return new CompletableFuture<SocketAddress>() {
				{
					future.addListener((f) -> {
						try {
							complete(new InetSocketAddress((InetAddress) f.get(), ((InetSocketAddress)address).getPort()));
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					});
				}
			};
		}
		else {
			return CompletableFuture.completedFuture(address);
		}
	}

	private CompletableFuture<SocketAddress> resolve(String proto, SocketAddress address)
	{
		if (address instanceof InetSocketAddress && ((InetSocketAddress) address).isUnresolved()) {
			InetSocketAddress address1 = (InetSocketAddress) address;
			String hostname = address1.getHostName();
			try {
				if (hostname.equals("*")) {
					switch (proto) {
					case "ip4":
					case "tcp4":
					case "udp4":
						return CompletableFuture.completedFuture(new InetSocketAddress(InetAddress.getByAddress(new byte[4]), address1.getPort()));

					case "ip6":
					case "tcp6":
					case "udp6":
						return CompletableFuture.completedFuture(new InetSocketAddress(InetAddress.getByAddress(new byte[16]), address1.getPort()));

					default:
						throw new IllegalArgumentException("Unknown protocol: proto="+proto);
					}
				}
			}
			catch (UnknownHostException ex) {
				return FutureUtil.exception(new UnknownHostException("Failed to resolve "+hostname+" : "+ex.getMessage()));
			}
			Future<List<InetAddress>> future =
				getInetNameResolver().resolveAll(hostname);
			return new CompletableFuture<SocketAddress>() {
				{
					future.addListener((f) -> {
						try {
							Class<? extends InetAddress> clazz = proto == null ? InetAddress.class : PROTO_TO_ADDRESS_CLASS.get(proto);
							if (clazz == null) {
								throw new IllegalArgumentException("Unrecognized proto: "+proto);
							}
							Optional<InetAddress> resolved = future.get().stream()
								.filter(clazz::isInstance)
								.findFirst();
							if (!resolved.isPresent())
								throw new UnknownHostException("Unknown host for proto="+proto+": "+hostname);
							complete(new InetSocketAddress(resolved.get(), ((InetSocketAddress)address).getPort()));
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					});
				}
			};
		}
		else {
			return CompletableFuture.completedFuture(address);
		}
	}

	public CompletableFuture<ServerChannel> listen(String proto, SocketAddress listen, ChannelInitializer<DuplexChannel> channelInitializer) throws InterruptedException
	{
		try {
			return new CompletableFuture<ServerChannel>() {
				ChannelFuture bindFuture;

				private synchronized void stepBind(SocketAddress address)
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group(bossGroup, workerGroup)
							.channelFactory(configAdapter.getServerChannel(address))
							.childHandler(channelInitializer).option(ChannelOption.SO_BACKLOG, Integer.MAX_VALUE)
							.childOption(ChannelOption.SO_KEEPALIVE, true);

					bindFuture = b.bind(address);

					bindFuture.addListener((f) -> {
						try {
							try {
								f.get();
							}
							catch (ExecutionException ex) {
								throw ex.getCause();
							}
							if (!complete((ServerChannel) bindFuture.channel())) {
								bindFuture.channel().close();
							}
						}
						catch (IOException ex) {
							completeExceptionally(new UncheckedIOException("Failed to bind to: "+address+" : "+ex.getMessage(), ex));
						}
						catch (Throwable ex) {
							completeExceptionally(new IOException("Failed to bind to: "+address, ex));
						}
					});
				}

				{
					resolve(proto, listen)
						.whenComplete((v, ex) -> {
							if (ex != null) {
								completeExceptionally(ex);
							}
							else {
								stepBind(v);
							}
						});
				}

				@Override
				public synchronized boolean cancel(boolean interrupt)
				{
					return bindFuture.cancel(interrupt);
				}
			};
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	public CompletableFuture<DuplexChannel> connect(String proto, SocketAddress address, ChannelHandler channelInitializer)
	{
		return new CompletableFuture<DuplexChannel>() {
			private ChannelFuture future;

			{
				resolve(proto, address)
					.whenComplete((v, ex) -> {
						if (ex != null) {
							completeExceptionally(ex);
						}
						else {
							stepConnect(v);
						}
					});
			}

			private synchronized void stepConnect(SocketAddress resolved)
			{
				if (isDone())
					return;
				try {
					Bootstrap b = new Bootstrap();
					future = b.group(workerGroup)
						.channelFactory(configAdapter.getStreamChannel(resolved))
						.handler(channelInitializer)
						.connect(resolved);

					future.addListener((f) -> {
						try {
							try {
								f.get();
							}
							catch (ExecutionException ex) {
								throw ex.getCause();
							}
							complete((DuplexChannel)future.channel());
						}
						catch (IOException ex) {
							completeExceptionally(new UncheckedIOException("Failed to connect to: "+address+" : "+ex.getMessage(), ex));
						}
						catch (Throwable ex) {
							completeExceptionally(new IOException("Failed to connect to: "+address, ex));
						}
					});
				}
				catch (Throwable ex) {
					completeExceptionally(ex);
				}
			}

			@Override
			public synchronized boolean cancel(boolean interrupt)
			{
				if (future != null)
					return future.cancel(interrupt);
				return super.cancel(interrupt);
			}
		};
	}

	public CompletableFuture<Void> writeAndShutdown(DuplexChannel channel, ByteBuf buf)
	{
		return new CompletableFuture<Void>() {
			{
				channel.writeAndFlush(buf)
					.addListener(f -> {
						try {
							NettyFutures.completeOrFail(channel.shutdownOutput(), this);
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					});
			}
		};
	}

	public CompletableFuture<Void> writeAndClose(DuplexChannel channel, ByteBuf buf)
	{
		return writeAndShutdown(channel, buf)
			.thenCompose(v -> NettyFutures.toCompletable(channel.close()));
	}

	public CompletableFuture<Void> forwardUni(DuplexChannel client, DuplexChannel server)
	{
		ChannelPromise clientPromise = client.newPromise();
		client.pipeline().addLast(new ForwarderHandler(server, clientPromise));
		client.config().setAutoRead(true);
		return NettyFutures.toCompletable(clientPromise);
	}

	public CompletableFuture<Void> forwardDuplex(DuplexChannel client, DuplexChannel server)
	{
		return NettyFutures.join(forwardUni(client, server), forwardUni(server, client));
	}

	@Override
	public void close() throws IOException
	{
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	public static ProtocolFamily getProtocolByAddress(InetAddress address)
	{
		return address instanceof Inet6Address ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
	}

	public static InternetProtocolFamily getNettyProtocolByAddress(InetAddress address)
	{
		return address instanceof Inet6Address ? InternetProtocolFamily.IPv6 : InternetProtocolFamily.IPv4;
	}

	private ConfigAdapter createConfigAdapter()
	{
		try {
			if (SystemUtils.IS_OS_LINUX) {
				return new EpollConfigAdapter();
			}
			else if (SystemUtils.IS_OS_MAC_OSX) {
				return new KqueueConfigAdapter();
			}
		}
		catch (Throwable ex) {
			log.error("Cannot create expected ConfigAdapter, falling back to Nio", ex);
		}
		return new NioConfigAdapter();
	}

	public interface ConfigAdapter
	{
		EventLoopGroup createEventLoopGroup();

		ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address);

		ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address);

		ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address);
	}

	public static class EpollConfigAdapter implements ConfigAdapter
	{
		static {
			EpollEventLoopGroup events = new EpollEventLoopGroup();
			events.shutdownGracefully(0, 0, TimeUnit.SECONDS);
		}

		@Override
		public EventLoopGroup createEventLoopGroup()
		{
			return new EpollEventLoopGroup();
		}

		@Override
		public ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return () -> new EpollServerSocketChannel(getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
			}
			else if (address instanceof DomainSocketAddress) {
				return EpollServerDomainSocketChannel::new;
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return () -> new EpollSocketChannel(getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
			}
			else if (address instanceof DomainSocketAddress) {
				return EpollDomainSocketChannel::new;
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return () -> new EpollDatagramChannel(getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
			}
			else {
				return EpollDatagramChannel::new;
			}
		}
	}

	public static class KqueueConfigAdapter implements ConfigAdapter
	{
		static {
			KQueueEventLoopGroup events = new KQueueEventLoopGroup();
			events.shutdownGracefully(0, 0, TimeUnit.SECONDS);
		}

		@Override
		public EventLoopGroup createEventLoopGroup()
		{
			return new KQueueEventLoopGroup();
		}

		@Override
		public ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return KQueueServerSocketChannel::new;
			}
			else if (address instanceof DomainSocketAddress) {
				return KQueueServerDomainSocketChannel::new;
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return KQueueSocketChannel::new;
			}
			else if (address instanceof DomainSocketAddress) {
				return KQueueDomainSocketChannel::new;
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
		{
			return KQueueDatagramChannel::new;
		}
	}

	public static class NioConfigAdapter implements ConfigAdapter
	{
		@Override
		public EventLoopGroup createEventLoopGroup()
		{
			return new NioEventLoopGroup();
		}

		@Override
		public ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return () -> new NioServerSocketChannel(
						SelectorProvider.provider(),
						getNettyProtocolByAddress(((InetSocketAddress) address).getAddress())
				);
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
		{

			if (address instanceof InetSocketAddress) {
				return () -> new NioSocketChannel(SelectorProvider.provider(), getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
			}
			else {
				throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
			}
		}

		@Override
		public ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
		{
			if (address instanceof InetSocketAddress) {
				return () -> new NioDatagramChannel(SelectorProvider.provider(), getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
			}
			else {
				return NioDatagramChannel::new;
			}
		}
	}

	@RequiredArgsConstructor
	public static class DelegatedSelectorProvider extends SelectorProvider
	{
		@Delegate
		private final SelectorProvider delegate;
	}
}
