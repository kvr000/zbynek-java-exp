package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.pipeline.ForwarderHandler;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider.ChannelProvider;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider.EpollChannelProvider;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider.KqueueChannelProvider;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider.NioChannelProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
import lombok.Getter;
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
import java.net.UnixDomainSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * Netty runtime core.
 */
@Log4j2
public class NettyEngine implements Closeable
{
	public static Map<String, Class<? extends InetAddress>> PROTO_TO_ADDRESS_CLASS = ImmutableMap.<String, Class<? extends InetAddress>>builder()
		.put("udp4", Inet4Address.class)
		.put("tcp4", Inet4Address.class)
		.put("ip4", Inet4Address.class)
		.put("udp6", Inet6Address.class)
		.put("tcp6", Inet6Address.class)
		.put("ip6", Inet6Address.class)
		.build();

	private ChannelProvider channelProvider = createChannelProvider();

	@Getter
	private EventLoopGroup bossGroup = channelProvider.createBossEventLoopGroup();
	@Getter
	private EventLoopGroup workerGroup = channelProvider.createWorkerEventLoopGroup();

	@Getter
	private final InetNameResolver inetNameResolver = new DnsNameResolverBuilder()
		.eventLoop(workerGroup.next())
		.channelFactory(channelProvider.getDatagramChannel(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)))
		.build();

	/**
	 * Resolves DNS address.
	 *
	 * @param address
	 * 	potentially unresolved address
	 *
	 * @return
	 * 	future with eventually resolved address or exception.
	 */
	public CompletableFuture<SocketAddress> resolve(SocketAddress address)
	{
		if (address instanceof InetSocketAddress && ((InetSocketAddress) address).isUnresolved()) {
			InetSocketAddress address1 = (InetSocketAddress) address;
			if (address1.getHostString().equals("*")) {
				return CompletableFuture.completedFuture(new InetSocketAddress(address1.getPort()));
			}
			Future<InetAddress> future =
				getInetNameResolver().resolve(((InetSocketAddress)address).getHostString());
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

	/**
	 * Resolves DNS address for specific protocol.
	 *
	 * @param proto
	 * 	protocol required for address
	 * @param address
	 * 	potentially unresolved address
	 *
	 * @return
	 * 	future with eventually resolved address or exception.
	 */
	private CompletableFuture<SocketAddress> resolve(String proto, SocketAddress address)
	{
		if (address instanceof InetSocketAddress && ((InetSocketAddress) address).isUnresolved()) {
			InetSocketAddress address1 = (InetSocketAddress) address;
			String hostname = address1.getHostString();
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
				return CompletableFuture.failedFuture(new UnknownHostException("Failed to resolve "+hostname+" : "+ex.getMessage()));
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

	/**
	 * Listens on specified address.
	 *
	 * @param addressSpec
	 * 	address to listen on
	 * @param channelInitializer
	 * 	child channel initializer
	 *
	 * @return
	 * 	future with server channel.
	 */
	public CompletableFuture<ServerChannel> listen(AddressSpec addressSpec, ChannelInitializer<DuplexChannel> channelInitializer)
	{
		try {
			return listen(addressSpec.getProto(), getProtoAddress(addressSpec), channelInitializer);
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	/**
	 * Listens on specified address.
	 *
	 * @param proto
	 * 	protocol to bind to
	 * @param listen
	 * 	address to listen on
	 * @param channelInitializer
	 * 	child channel initializer
	 *
	 * @return
	 * 	future with server channel.
	 */
	public CompletableFuture<ServerChannel> listen(String proto, SocketAddress listen, ChannelInitializer<DuplexChannel> channelInitializer)
	{
		try {
			return new CompletableFuture<ServerChannel>() {
				ChannelFuture bindFuture;

				private synchronized void stepBind(SocketAddress address)
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group(bossGroup, workerGroup)
						.channelFactory(channelProvider.getServerChannel(address))
						.option(ChannelOption.SO_BACKLOG, Integer.MAX_VALUE)
						.childHandler(channelInitializer)
						.childOption(ChannelOption.AUTO_READ, false)
						.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true);
					if (!SystemUtils.IS_OS_MAC_OSX) {
						b.childOption(ChannelOption.SO_KEEPALIVE, true);
					}

					bindFuture = b.bind(channelProvider.convertAddress(address));

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
						.thenAccept(this::stepBind)
						.whenComplete((v, ex) -> {
							if (ex != null) {
								completeExceptionally(ex);
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

	/**
	 * Connects to specified address.
	 *
	 * @param addressSpec
	 * 	address to connect to
	 * @param channelInitializer
	 * 	child channel initializer
	 *
	 * @return
	 * 	future with client channel.
	 */
	public CompletableFuture<DuplexChannel> connect(AddressSpec addressSpec, ChannelHandler channelInitializer)
	{
		try {
			return connect(addressSpec.getProto(), getProtoAddress(addressSpec), channelInitializer);
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	/**
	 * Connects to specified address.
	 *
	 * @param proto
	 * 	protocol to connect
	 * @param address
	 * 	address to connect to
	 * @param channelInitializer
	 * 	child channel initializer
	 *
	 * @return
	 * 	future with client channel.
	 */
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
				SocketAddress converted = channelProvider.convertAddress(resolved);
				if (isDone())
					return;
				try {
					Bootstrap b = new Bootstrap();
					future = b.group(workerGroup)
						.channelFactory(channelProvider.getStreamChannel(converted))
						.option(ChannelOption.AUTO_READ, false)
						.option(ChannelOption.ALLOW_HALF_CLOSURE, true)
						.handler(channelInitializer)
						.connect(converted);

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

	/**
	 * Shutdown the channel output.
	 *
	 * @param channel
	 * 	channel to shutdown
	 *
	 * @return
	 * 	future completed at shutdown.
	 */
	public CompletableFuture<Void> shutdownOutput(DuplexChannel channel)
	{
		return writeAndShutdown(channel, Unpooled.EMPTY_BUFFER);
	}

	/**
	 * Writes data and shuts down channel for output.
	 *
	 * @param channel
	 * 	channel to write data to
	 * @param buf
	 * 	buffer to write
	 *
	 * @return
	 * 	future completing once operation is done.
	 */
	public CompletableFuture<Void> writeAndShutdown(DuplexChannel channel, ByteBuf buf)
	{
		return new CompletableFuture<Void>() {
			{
				CompletableFuture<Void> this0 = this;

				channel.writeAndFlush(buf)
					.addListener(f -> {
						try {
							if (f.isSuccess()) {
								NettyFutures.copy(channel.shutdownOutput(), this0);
							}
							else {
								completeExceptionally(f.cause());
							}
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					});
			}
		};
	}

	/**
	 * Writes data and closes channel.
	 *
	 * @param channel
	 * 	channel to write data to
	 * @param buf
	 * 	buffer to write
	 *
	 * @return
	 * 	future completing once operation is done.
	 */
	public CompletableFuture<Void> writeAndClose(DuplexChannel channel, ByteBuf buf)
	{
		return NettyFutures.composeAlways(
			writeAndShutdown(channel, buf),
			() -> NettyFutures.toCompletable(channel.close())
		);
	}

	/**
	 * Forwards traffic from one channel to another.
	 *
	 * @param source
	 * 	source channel
	 * @param destination
	 * 	destination channel
	 *
	 * @return
	 * 	future completing once source channel is closed.
	 */
	public CompletableFuture<Void> forwardUni(DuplexChannel source, DuplexChannel destination)
	{
		CompletableFuture<Void> clientPromise = new CompletableFuture<>();
		source.pipeline().addLast(new ForwarderHandler(this, source, destination, clientPromise));
		return clientPromise;
	}

	/**
	 * Forwards traffic between channels.
	 *
	 * @param source
	 * 	source channel
	 * @param destination
	 * 	destination channel
	 *
	 * @return
	 * 	future completing once both channels are closed.
	 */
	public CompletableFuture<Void> forwardDuplex(DuplexChannel source, DuplexChannel destination)
	{
		return NettyFutures.join(forwardUni(source, destination), forwardUni(destination, source));
	}

	@Override
	public void close() throws IOException
	{
		workerGroup.shutdownGracefully().syncUninterruptibly();
		bossGroup.shutdownGracefully().syncUninterruptibly();
	}

	public static ProtocolFamily getProtocolByAddress(InetAddress address)
	{
		return address instanceof Inet6Address ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
	}

	public static InternetProtocolFamily getNettyProtocolByAddress(InetAddress address)
	{
		return address instanceof Inet6Address ? InternetProtocolFamily.IPv6 : InternetProtocolFamily.IPv4;
	}

	private ChannelProvider createChannelProvider()
	{
		try {
			if (SystemUtils.IS_OS_LINUX) {
				return new EpollChannelProvider();
			}
			else if (SystemUtils.IS_OS_MAC_OSX) {
				return new KqueueChannelProvider();
			}
		}
		catch (Throwable ex) {
			log.error("Cannot create expected ConfigAdapter, falling back to Nio", ex);
		}
		return new NioChannelProvider();
	}

	public SocketAddress getProtoAddress(AddressSpec addressSpec)
	{
		switch (addressSpec.getProto()) {
		case "tcp4":
		case "tcp6":
			return InetSocketAddress.createUnresolved(Optional.ofNullable(addressSpec.getHost()).orElse("*"), addressSpec.getPort());

		case "domain":
		case "unix":
			return UnixDomainSocketAddress.of(addressSpec.getPath());

		default:
			throw new IllegalArgumentException("Unsupported proto: " + addressSpec.getProto());
		}
	}
}
