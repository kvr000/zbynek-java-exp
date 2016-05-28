package cz.znj.kvr.sw.exp.java.netty.netty4.server.forward;

import com.google.common.base.Preconditions;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.common.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.common.NettyRuntime;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DuplexChannel;

import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * PortForwarder based on Netty.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class NettyPortForwarder implements PortForwarder
{
	private final NettyRuntime nettyRuntime;

	@Override
	public CompletableFuture<CompletableFuture<Void>> runForwards(List<ForwardConfig> forwards)
	{
		List<CompletableFuture<CompletableFuture<Void>>> futures = forwards.stream().map(this::runForward).collect(Collectors.toList());
		return allOrCancelNestedFutures(futures);
	}

	@Override
	public CompletableFuture<CompletableFuture<Void>> runForward(ForwardConfig forward)
	{
		try {
			Preconditions.checkArgument(forward.getBind() != null, "bind must be specified");
			Preconditions.checkArgument(forward.getConnect() != null, "connect must be specified");
			switch (Optional.ofNullable(forward.getBind().getProto()).orElse("")) {
			case "tcp4":
			case "tcp6":
				Preconditions.checkArgument(forward.getBind().getPort() != 0, "port not specified");
				break;

			case "unix":
				Preconditions.checkArgument(forward.getBind().getPath() != null, "path not specified");
				break;

			default:
				throw new IllegalArgumentException("Unknown bind.proto: "+forward.getBind().getProto());
			}
			switch (Optional.ofNullable(forward.getConnect().getProto()).orElse("")) {
			case "tcp4":
			case "tcp6":
				Preconditions.checkArgument(forward.getConnect().getPort() != 0, "port not specified");
				Preconditions.checkArgument(forward.getConnect().getHost() != null, "host not specified");
				break;

			case "unix":
			case "domain":
				Preconditions.checkArgument(forward.getConnect().getPath() != null, "path not specified");
				break;

			default:
				throw new IllegalArgumentException("Unknown connect.proto: "+forward.getConnect().getProto());
			}
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
		return runListener(forward);

	}

	private CompletableFuture<CompletableFuture<Void>> runListener(ForwardConfig config)
	{
		return new CompletableFuture<CompletableFuture<Void>>() {
			private final CompletableFuture<CompletableFuture<Void>> this0 = this;
			private CompletableFuture<ServerChannel> initFuture;
			private ServerChannel listener;

			@Override
			public synchronized boolean cancel(boolean interrupt) {
				if (listener != null)
					listener.close();
				initFuture.cancel(interrupt);
				return super.cancel(interrupt);
			}

			public synchronized boolean setListener(ServerChannel listener) {
				if (isDone()) {
					return false;
				}
				this.listener = listener;
				return true;
			}

			private void fail(Throwable ex)
			{
				if (listener != null)
					listener.close();
				completeExceptionally(ex);
			}

			private void connectForward(String proto, DuplexChannel client, SocketAddress remote)
			{
				nettyRuntime.connect(
					proto,
						remote,
						new ChannelInitializer<DuplexChannel>()
						{
							@Override
							public void initChannel(DuplexChannel server) throws Exception
							{
								server.config().setAutoRead(false);
							}
						}
					)
					.whenComplete((server, ex) -> {
						if (ex == null) {
							nettyRuntime.forwardDuplex(client, server)
								.whenComplete((v, ex2) ->
									NettyFutures.join(client.close(), server.close())
								);
						}
						else {
							log.error("Failed to connect", ex);
							client.close();
						}
					});
			}

			private void createListener(String proto, SocketAddress address) throws InterruptedException
			{
				initFuture = nettyRuntime.listen(
					proto,
					address,
					new ChannelInitializer<DuplexChannel>() {
						@Override
						public void initChannel(DuplexChannel client) throws Exception {
							client.config().setAutoRead(false);
							SocketAddress remote = createAddress(config.getConnect());
							connectForward(config.getConnect().getProto(), client, remote);
						}
					}
				);
				initFuture.whenComplete((channel, ex) -> {
					if (ex != null) {
						fail(ex);
					}
					else {
						if (!setListener(channel)) {
							channel.close();
						}
						else {
							CompletableFuture<Void> close = new CompletableFuture<Void>() {
								public synchronized boolean cancel(boolean interrupt) {
									channel.close();
									return super.cancel(interrupt);
								}
							};
							NettyFutures.completeOrFail(channel.closeFuture(), close);
							complete(close);
						}
					}
				});
			}

			{
				try {
					SocketAddress address = createAddress(config.getBind());
					createListener(config.getBind().getProto(), address);
				}
				catch (Throwable ex) {
					fail(ex);
				}
			}
		};
	}

	private SocketAddress createAddress(ForwardConfig.AddressSpec addressSpec)
	{
		switch (addressSpec.getProto()) {
		case "tcp4":
		case "tcp6":
			return InetSocketAddress.createUnresolved(addressSpec.getHost(), addressSpec.getPort());

		case "domain":
		case "unix":
			return new DomainSocketAddress(addressSpec.getPath());

		default:
			throw new IllegalStateException("Improperly validated config, expected tcp4 or tcp6 or domain or unix for proto: " + addressSpec);
		}
	}

	@Override
	public void close()
	{
	}

	public static <T> CompletableFuture<CompletableFuture<T>> allOrCancelNestedFutures(List<CompletableFuture<CompletableFuture<T>>> futures)
	{
		AtomicInteger remaining = new AtomicInteger(futures.size());

		return new CompletableFuture<CompletableFuture<T>>() {
			{
				futures.forEach(f -> {
					f.whenComplete((v, ex) -> {
						if (ex != null) {
							completeExceptionally(ex);
						}
						if (remaining.decrementAndGet() == 0) {
							stepInner();
						}
					});
				});
				whenComplete((v, ex) -> {
					if (ex != null) {
						futures.forEach(f -> {
							f.cancel(true);
							CompletableFuture<T> inner = f.getNow(null);
							if (inner != null)
								inner.cancel(true);
						});
					}
				});
			}

			private void stepInner()
			{
				complete(new CompletableFuture<T>() {
					{
						futures.forEach(f -> {
							CompletableFuture<T> inner = f.getNow(null);
							inner.whenComplete((v, ex) -> {
								if (ex != null) {
									completeExceptionally(ex);
								}
								else {
									complete(v);
								}
							});
						});
						whenComplete((v, ex) -> {
							futures.forEach(f -> {
								CompletableFuture<T> inner = f.getNow(null);
								inner.cancel(true);
							});
						});
					}

					public synchronized boolean cancel(boolean interrupt)
					{
						futures.forEach(f -> {
							CompletableFuture<T> inner = f.getNow(null);
							inner.cancel(true);
						});
						return super.cancel(interrupt);
					}
				});
			}
		};
	}
}
