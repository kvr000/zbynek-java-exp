package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward;

import com.google.common.base.Preconditions;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyServer;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.pipeline.FullFlowControlHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DuplexChannel;

import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.flow.FlowControlHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


/**
 * PortForwarder based on Netty.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class NettyPortForwarderFactory implements PortForwarderFactory
{
	private final NettyEngine nettyEngine;

	@Override
	public List<CompletableFuture<Server>> runForwards(List<ForwardConfig> forwards)
	{
		List<CompletableFuture<Server>> futures = forwards.stream().map(this::runForward).collect(Collectors.toList());
		return futures;
	}

	@Override
	public CompletableFuture<Server> runForward(ForwardConfig forward)
	{
		try {
			Preconditions.checkArgument(forward.getBind() != null, "bind must be specified");
			switch (Optional.ofNullable(forward.getBind().getProto()).orElse("")) {
			case "tcp4":
			case "tcp6":
				break;

			case "unix":
			case "domain":
				Preconditions.checkArgument(forward.getBind().getPath() != null, "path not specified");
				break;

			default:
				throw new IllegalArgumentException("Unknown bind.proto: "+forward.getBind().getProto());
			}

			Preconditions.checkArgument(forward.getConnect() != null, "connect must be specified");
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
		return runForwarder(forward);

	}

	private CompletableFuture<Server> runForwarder(ForwardConfig config)
	{
		return new CompletableFuture<Server>() {
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

			private void connectForward(DuplexChannel client, AddressSpec connect)
			{
				nettyEngine.connect(
						connect,
						new ChannelInitializer<DuplexChannel>()
						{
							@Override
							public void initChannel(DuplexChannel server) throws Exception
							{
								server.config().setAutoRead(false);
								server.pipeline().addLast(new FullFlowControlHandler());
							}
						}
					)
					.whenComplete((server, ex) -> {
						if (ex == null) {
							nettyEngine.forwardDuplex(client, server)
								.whenComplete((v, ex2) -> {
									NettyFutures.join(client.close(), server.close());
								});
						}
						else {
							log.error("Failed to connect to: {}", connect, ex);
							client.close();
						}
					});
			}

			private void createListener(AddressSpec address) throws InterruptedException
			{
				initFuture = nettyEngine.listen(
					address,
					new ChannelInitializer<DuplexChannel>() {
						@Override
						public void initChannel(DuplexChannel client) throws Exception {
							client.config().setAutoRead(false);
							client.pipeline().addFirst(new FullFlowControlHandler());
							connectForward(client, config.getConnect());
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
							complete(new NettyServer(
								channel
							));
						}
					}
				});
			}

			{
				try {
					createListener(config.getBind());
				}
				catch (Throwable ex) {
					fail(ex);
				}
			}
		};
	}

	private SocketAddress createAddress(AddressSpec addressSpec)
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
}
