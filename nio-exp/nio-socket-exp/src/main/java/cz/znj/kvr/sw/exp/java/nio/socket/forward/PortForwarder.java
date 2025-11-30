package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.base.function.ThrowingBiConsumer;
import net.dryuf.base.function.ThrowingFunction;
import org.apache.commons.io.IOUtils;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Port forwarding component.
 */
public class PortForwarder implements AutoCloseable
{
	private final AtomicLong channelCount = new AtomicLong();

	private final ExecutorService blockingExecutor;

	@Inject
	public PortForwarder(@Named("blockingExecutor") ExecutorService blockingExecutor)
	{
		this.blockingExecutor = blockingExecutor;
	}

	public void createdChannel(Channel channel)
	{
		channelCount.incrementAndGet();
	}

	public <T> void closeChannel(CompletableFuture<T> future, Channel channel)
	{
		if (channel != null) {
			synchronized (channel) {
				if (channel.isOpen()) {
					try {
						@SuppressWarnings("unused") // just for debugging
						long count = channelCount.decrementAndGet();
						channel.close();
					}
					catch (Throwable ex) {
						if (future != null)
							future.completeExceptionally(ex);
					}
				}
			}
		}
	}

	public CompletableFuture<Void> runForwards(List<ForwardConfig> forwards)
	{
		List<CompletableFuture<Void>> futures = forwards.stream().map(this::runForward).collect(Collectors.toList());
		return FutureUtil.anyAndCancel(futures);
	}

	public CompletableFuture<Void> runForward(ForwardConfig forward)
	{
		try {
			switch (Optional.ofNullable(forward.bindProto).orElse("")) {
			case "tcp4":
			case "tcp6":
				Preconditions.checkArgument(forward.bindPort != 0, "bindPort not specified");
				break;

			case "unix":
				Preconditions.checkArgument(forward.bindPath != null, "bindPath not specified");
				break;

			default:
				throw new IllegalArgumentException("Unknown bindProto: "+forward.bindProto);
			}
			switch (Optional.ofNullable(forward.connectProto).orElse("")) {
			case "tcp4":
			case "tcp6":
				Preconditions.checkArgument(forward.connectPort != 0, "connectPort not specified");
				Preconditions.checkArgument(forward.connectHost != null, "connectHost not specified");
				break;

			case "unix":
				Preconditions.checkArgument(forward.connectPath != null, "connectPath not specified");
				break;

			default:
				throw new IllegalArgumentException("Unknown connectProto: "+forward.connectProto);
			}
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
		return runListener(forward);
	}

	public CompletableFuture<Void> runBothForward(AsynchronousSocketChannel one, AsynchronousSocketChannel two)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				closeChannel(this, one);
				closeChannel(this, two);
				return true;
			}
		};
		CompletableFuture<Void> there = runOneForward(one, two);
		CompletableFuture<Void> back = runOneForward(two, one);
		CompletableFuture.allOf(there, back)
			.whenComplete((v, ex) -> {
				closeChannel(null, one);
				closeChannel(null, two);
				FutureUtil.completeOrFail(future, v, ex);
			});
		return future;
	}

	public CompletableFuture<SocketAddress> resolve(SocketAddress address)
	{
		if (address instanceof InetSocketAddress && ((InetSocketAddress) address).isUnresolved()) {
			return FutureUtil.submitAsync(() -> {
					InetSocketAddress address1 = (InetSocketAddress) address;
					return new InetSocketAddress(InetAddress.getByName(address1.getHostName()), address1.getPort());
				},
				blockingExecutor
			);
		}
		else {
			return CompletableFuture.completedFuture(address);
		}
	}

	private CompletableFuture<SocketAddress> resolve(String proto, SocketAddress address)
	{
		if (!(address instanceof InetSocketAddress))
			return CompletableFuture.completedFuture(address);

		InetSocketAddress address1 = (InetSocketAddress) address;
		if (!address1.isUnresolved())
			return CompletableFuture.completedFuture(address1);
		return FutureUtil.submitAsync(() ->
				new InetSocketAddress(
					Stream.of(Inet4Address.getAllByName(address1.getHostName()))
						.filter(proto.equals("tcp4") ?
							Inet4Address.class::isInstance :
							Inet6Address.class::isInstance
						)
						.findFirst()
						.orElseThrow(() -> new UnknownHostException("Cannot find "+proto+" " +
							"host for: "+address)),
					address1.getPort()
				),
			blockingExecutor
		);
	}

	public void connect(SocketAddress address, Consumer<AsynchronousSocketChannel> initializer, CompletionHandler<Void, Integer> handler)
	{
		connect(this::resolve, address, initializer, handler);
	}

	public void connect(Function<SocketAddress, CompletableFuture<SocketAddress>> resolver, SocketAddress address, Consumer<AsynchronousSocketChannel> initializer, CompletionHandler<Void, Integer> handler)
	{
		resolver.apply(address)
			.thenApply(ThrowingFunction.sneaky(address1 -> {
				AsynchronousSocketChannel socket = null;
				try {
					socket = AsynchronousSocketChannel.open();
					createdChannel(socket);
					initializer.accept(socket);
					socket.connect(address1, 0, handler);
					return socket;
				}
				catch (Throwable ex) {
					if (socket != null)
						closeChannel(null, socket);
					throw ex;
				}
			}))
			.whenComplete((socket, ex) -> {
				if (ex != null) {
					if (ex instanceof IOException) {
						handler.failed(new UncheckedIOException("Failed to connect to: "+address, (IOException)ex), 0);
					}
					else {
						handler.failed(ex, 0);
					}
				}
			});
	}

	public CompletableFuture<Void> writeFully(AsynchronousSocketChannel socket, ByteBuffer buffer)
	{
		if (!buffer.hasRemaining())
			return CompletableFuture.completedFuture(null);

		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				closeChannel(this, socket);
				return true;
			}
		};
		try {
			socket.write(buffer, 0, new CompletionHandler<Integer, Integer>()
			{
				@Override
				public void completed(Integer result, Integer attachment)
				{
					if (!buffer.hasRemaining()) {
						future.complete(null);
					}
					else {
						try {
							socket.write(buffer, 0, this);
						}
						catch (Throwable ex) {
							future.completeExceptionally(ex);
						}
					}
				}

				@Override
				public void failed(Throwable exc, Integer attachment)
				{
					future.completeExceptionally(new IOException("Failed to write to: "+getRemoteAddressSafe(socket), exc));
				}
			});
		}
		catch (Throwable ex) {
			future.completeExceptionally(ex);
		}
		return future;
	}

	public CompletableFuture<Void> writeAndShutdown(AsynchronousSocketChannel socket, ByteBuffer buffer)
	{
		return writeFully(socket, buffer)
			.whenComplete(ThrowingBiConsumer.sneaky((v, ex) -> {
				if (ex == null) {
					socket.shutdownOutput();
				}
			}));
	}

	public CompletableFuture<Void> runListener(Function<AsynchronousServerSocketChannel, CompletableFuture<Void>> initializer)
	{
		AsynchronousServerSocketChannel listener = null;
		try {
			listener = AsynchronousServerSocketChannel.open();
			createdChannel(listener);
			return initializer.apply(listener);
		}
		catch (Throwable ex) {
			closeChannel(null, listener);
			return FutureUtil.exception(ex);
		}
	}

	public CompletableFuture<Void> runServerClient(Socket server, Socket client)
	{
		CompletableFuture<Void> one = runOneForward(server, client);
		CompletableFuture<Void> two = runOneForward(client, server);
		return CompletableFuture.allOf(one, two)
			.whenComplete((v, ex) -> {
				IOUtils.closeQuietly(client);
				IOUtils.closeQuietly(server);
			});
	}

	public void close()
	{
	}

	private CompletableFuture<Void> runListener(ForwardConfig config)
	{
		if (!config.bindProto.equals("unix") && !config.connectProto.equals("unix")) {
			return runListenerAsync(config);
		}
		// For unix sockets which don't support asynchronous interface, use old-school accept and fork pattern:
		CompletableFuture<Void> result = new CompletableFuture<Void>() {
			private ServerSocket listener;
			private CompletableFuture<Void> this0 = this;

			@Override
			public synchronized boolean cancel(boolean interrupt) {
				if (!super.cancel(interrupt))
					return false;
				if (listener != null) {
					try {
						listener.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			}

			private synchronized boolean setListener(ServerSocket listener) {
				if (isDone()) {
					return false;
				}
				this.listener = listener;
				return true;
			}

			public CompletableFuture<Void> initialize()
			{
				try {
					CompletableFuture<ServerSocket> listenerFuture;
					switch (config.bindProto) {
					case "tcp4":
					case "tcp6":
						InetSocketAddress address0 = Optional.ofNullable(config.bindHost)
							.map(name -> InetSocketAddress.createUnresolved(name, config.bindPort))
							.orElseGet(() -> new InetSocketAddress(config.bindPort));
						listenerFuture = resolve(config.bindProto, address0)
							.thenApplyAsync(address -> {
									try {
										return new ServerSocket(
											((InetSocketAddress)address).getPort(),
											0,
											((InetSocketAddress)address).getAddress()
										);
									}
									catch (IOException e) {
										throw new UncheckedIOException("Failed to create listener on: "+address, e);
									}
								},
								blockingExecutor
							);
						break;

					case "unix":
						listenerFuture = FutureUtil.submitAsync(() -> {
								try {
									return AFUNIXServerSocket.bindOn(new AFUNIXSocketAddress(new File(config.bindPath)));
								}
								catch (IOException ex) {
									throw new IOException("Failed to create listener on "+config.bindPath, ex);
								}
							},
							blockingExecutor
						);
						break;

					default:
						throw new IllegalStateException("Improperly validated config, expected tcp4, tcp6 or unix on bindProto: "+config);
					}
					listenerFuture.thenAcceptAsync((listener1) -> {
								if (!setListener(listener1)) {
									IOUtils.closeQuietly(listener1);
									return;
								}
								loopListener(listener1, config);
							},
							blockingExecutor
						)
						.whenComplete((v, ex) -> FutureUtil.completeOrFail(this0, v, ex));
				}
				catch (Throwable ex) {
					completeExceptionally(ex);
				}
				return this;
			}
		}.initialize();
		return result;
	}

	@SneakyThrows
	private void loopListener(ServerSocket listener, ForwardConfig config)
	{
		for (;;) {
			Socket server;
			server = listener.accept();
			runConnectionForward(server, config)
				.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
		}
	}

	private CompletableFuture<Void> runConnectionForward(Socket server, ForwardConfig config)
	{
		CompletableFuture<Socket> clientFuture;
		try {
			switch (config.connectProto) {
			case "tcp4":
			case "tcp6":
				InetSocketAddress address0 = InetSocketAddress.createUnresolved(config.connectHost, config.connectPort);
				clientFuture = resolve(config.connectProto, InetSocketAddress.createUnresolved(config.connectHost, config.connectPort))
					.thenApplyAsync(address -> {
							try {
								return new Socket(((InetSocketAddress)address).getAddress(), ((InetSocketAddress)address).getPort());
							}
							catch (IOException e) {
								throw new UncheckedIOException("Failed to connect to: "+address0, e);
							}
						},
						blockingExecutor
					);
				break;

			case "unix":
				clientFuture = FutureUtil.submitAsync(() -> {
						try {
							return AFUNIXSocket.connectTo(new AFUNIXSocketAddress(new File(config.connectPath)));
						}
						catch (IOException ex) {
							throw new UncheckedIOException("Failed to connect to: "+config.connectPath, ex);
						}
					},
					blockingExecutor
				);
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
			return clientFuture
				.whenComplete(FutureUtil.whenException(ex -> IOUtils.closeQuietly(server)))
				.thenCompose(client -> runServerClient(server, client));
		}
		catch (Throwable e) {
			IOUtils.closeQuietly(server);
			return FutureUtil.exception(e);
		}
	}

	private CompletableFuture<Void> runOneForward(Socket one, Socket two)
	{
		return FutureUtil.submitAsync(() -> {
				InputStream oneInput = one.getInputStream();
				OutputStream twoOutput = two.getOutputStream();
				int size;
				byte[] buf = new byte[4096];
				while ((size = oneInput.read(buf)) > 0) {
					try {
						twoOutput.write(buf, 0, size);
						twoOutput.flush();
					}
					catch (IOException ex) {
						throw new IOException("Failed to write to: "+two.getRemoteSocketAddress(), ex);
					}
				}
				two.shutdownOutput();
				one.shutdownInput();
				return (Void)null;
			},
			blockingExecutor
		);
	}

	private CompletableFuture<Void> runListenerAsync(ForwardConfig config)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			private CompletableFuture<Void> this0 = this;
			private AsynchronousServerSocketChannel listener;

			@Override
			public synchronized boolean cancel(boolean interrupt) {
				if (listener != null) {
					closeChannel(this, listener);
				}
				return true;
			}

			public synchronized boolean setListener(AsynchronousServerSocketChannel listener) {
				if (isDone()) {
					return false;
				}
				this.listener = listener;
				return true;
			}

			private void fail(Throwable ex)
			{
				closeChannel(this, listener);
				completeExceptionally(ex);
			}

			public CompletableFuture<Void> initialize()
			{
				try {
					Preconditions.checkArgument(!config.bindProto.equals("unix") && !config.connectProto.equals("unix"));
					CompletableFuture<AsynchronousServerSocketChannel> listenerFuture;
					switch (config.bindProto) {
					case "tcp4":
					case "tcp6":
						listenerFuture = createListenerTcp(config);
						break;

					default:
						throw new IllegalStateException("Improperly validated config, expected tcp4 or tcp6 for bindProto: "+config);
					}
					listenerFuture.thenAccept(listener0 -> {
							if (!setListener(listener0)) {
								closeChannel(this0, listener0);
								return;
							}
							listener0.accept(
								0,
								new CompletionHandler<AsynchronousSocketChannel, Integer>()
								{
									@Override
									public void completed(AsynchronousSocketChannel result, Integer attachment)
									{
										createdChannel(result);
										listener.accept(0, this);
										runConnectionForward(result, config)
											.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
									}

									@Override
									public void failed(Throwable exc, Integer attachment)
									{
										this0.completeExceptionally(exc);
									}
								}
							);
						})
						.whenComplete(FutureUtil.whenException(this::fail));
				}
				catch (Throwable ex) {
					fail(ex);
				}
				return this;
			}

			private CompletableFuture<AsynchronousServerSocketChannel> createListenerTcp(ForwardConfig config)
			{
				SocketAddress address0 = Optional.ofNullable(config.bindHost)
					.map(name -> InetSocketAddress.createUnresolved(name, config.bindPort))
					.orElseGet(() -> new InetSocketAddress(config.bindPort));
				return resolve(config.bindProto, address0)
					.thenApply(address -> {
						AsynchronousServerSocketChannel listener = null;
						try {
							listener = AsynchronousServerSocketChannel.open();
							createdChannel(listener);
							listener.bind(address);
							return listener;
						}
						catch (IOException ex) {
							closeChannel(null, listener);
							throw new UncheckedIOException("Failed to create listener on: "+address, ex);
						}
					});
			}
		}.initialize();
		return future;
	}

	private CompletableFuture<Void> runConnectionForward(AsynchronousSocketChannel client, ForwardConfig config)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			AsynchronousSocketChannel server;

			@Override
			public synchronized boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				closeChannel(this, client);
				closeChannel(this, server);
				return true;
			}

			public CompletableFuture<Void> initialize()
			{
				CompletableFuture<Void> this0 = this;
				try {
					switch (config.connectProto) {
					case "tcp4":
					case "tcp6":
						InetSocketAddress address0 = InetSocketAddress.createUnresolved(config.connectHost, config.connectPort);
						connect(
							(address) -> resolve(config.connectProto, address),
							address0,
							(socket) -> server = socket,
							new CompletionHandler<Void, Integer>()
							{
								@Override
								public void completed(Void result, Integer attachment)
								{
									runBothForward(client, server)
										.whenComplete((v, ex) -> FutureUtil.completeOrFail(this0, v, ex));
								}

								@Override
								public void failed(Throwable exc, Integer attachment)
								{
									this0.completeExceptionally(new IOException("Failed to connect to: "+address0, exc));
								}
							}
						);
						break;

					default:
						throw new IllegalStateException("Improperly validated config: "+config);
					}
				}
				catch (Throwable e) {
					this.completeExceptionally(e);
				}
				CompletableFuture<Void> last = new CompletableFuture<>();
				this.whenComplete((v, ex) -> {
					if (ex != null)
						last.completeExceptionally(ex);
					closeChannel(last, client);
					closeChannel(last, server);
					last.complete(v);
				});
				return last;
			}
		}.initialize();
		return future;
	}

	private CompletableFuture<Void> runOneForward(AsynchronousSocketChannel reader, AsynchronousSocketChannel writer)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>();
		try {
			ByteBuffer buf = ByteBuffer.allocateDirect(4096);
			reader.read(buf, 0, new CompletionHandler<Integer, Integer>()
			{
				@Override
				public void completed(Integer result, Integer attachment)
				{
					CompletionHandler<Integer, Integer> readerCompleter = this;

					if (result <= 0) {
						try {
							writer.shutdownOutput();
							future.complete(null);
						}
						catch (Throwable e) {
							future.completeExceptionally(e);
						}
						return;
					}
					buf.flip();
					writer.write(buf, 0, new CompletionHandler<Integer, Integer>()
					{
						@Override
						public void completed(Integer result, Integer attachment)
						{
							if (buf.hasRemaining()) {
								writer.write(buf, 0, this);
							}
							else {
								buf.compact();
								reader.read(buf, 0, readerCompleter);
							}
						}

						@Override
						public void failed(Throwable exc, Integer attachment)
						{
							try {
								reader.shutdownInput();
							}
							catch (IOException e) {
								exc.addSuppressed(e);
							}
							finally {
								future.completeExceptionally(new IOException("Failed to write to: "+getRemoteAddressSafe(writer), exc));
							}
						}
					});
				}

				@Override
				public void failed(Throwable exc, Integer attachment)
				{
					future.completeExceptionally(exc);
				}
			});
		}
		catch (Throwable ex) {
			future.completeExceptionally(ex);
		}
		return future;
	}

	private SocketAddress getRemoteAddressSafe(AsynchronousSocketChannel socket)
	{
		try {
			return socket.getRemoteAddress();
		}
		catch (IOException e) {
			return null;
		}
	}

	@Builder(builderClassName = "Builder")
	@Value
	public static class ForwardConfig
	{
		/** One of tcp4, tcp6, unix: */
		String bindProto;
		String bindHost;
		String bindPath;
		int bindPort;

		/** One of tcp4, tcp6, unix: */
		String connectProto;
		String connectHost;
		String connectPath;
		int connectPort;
	}
}
