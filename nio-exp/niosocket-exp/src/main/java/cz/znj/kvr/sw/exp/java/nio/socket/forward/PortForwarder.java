package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import cz.znj.kvr.sw.exp.java.nio.socket.util.FutureUtil;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.io.IOUtils;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import javax.inject.Inject;
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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Port forwarding component.  Simple implementation using threads instead of selectors.
 */
public class PortForwarder implements AutoCloseable
{
	private AtomicLong channelCount = new AtomicLong();

	@Inject
	public PortForwarder()
	{
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

	public void connect(SocketAddress address, Consumer<AsynchronousSocketChannel> initializer, CompletionHandler<Void, Integer> handler)
	{
		AsynchronousSocketChannel socket = null;
		try {
			socket = AsynchronousSocketChannel.open();
			createdChannel(socket);
			initializer.accept(socket);
			socket.connect(address, 0, handler);
		}
		catch (Throwable e) {
			closeChannel(null, socket);
			handler.failed(new IOException("Failed to connect to: "+address, e), 0);
			return;
		}
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
					future.completeExceptionally(exc);
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
			.whenComplete((v, ex) -> {
				if (ex == null) {
					try {
						socket.shutdownOutput();
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			});
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

	@Override
	public void close() throws InterruptedException
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

			public CompletableFuture<Void> initialize()
			{
				try {
					switch (config.bindProto) {
					case "tcp4":
					case "tcp6":
						InetSocketAddress address = null;
						try {
							address = new InetSocketAddress(
								Optional.ofNullable(config.bindHost)
									.map(name -> {
											try {
												return getHost(config.bindProto, name);
											}
											catch (UnknownHostException e) {
												throw new UncheckedIOException(e);
											}
										}
									)
									.orElse(null),
								config.bindPort
							);
							listener = new ServerSocket(
								address.getPort(),
								0,
								address.getAddress()
							);
						}
						catch (IOException ex) {
							throw new IOException("Failed to create listener on "+address, ex);
						}
						break;

					case "unix":
						try {
							listener = AFUNIXServerSocket.bindOn(new AFUNIXSocketAddress(new File(config.bindPath)));
						}
						catch (IOException ex) {
							throw new IOException("Failed to create listener on "+config.bindPath, ex);
						}
						break;

					default:
						throw new IllegalStateException("Improperly validated config: "+config);
					}
					new Thread(() -> {
						try {
							loopListener(listener, config);
							complete(null);
						}
						catch (Throwable ex) {
							completeExceptionally(ex);
						}
					}).start();
				}
				catch (Throwable ex) {
					completeExceptionally(ex);
				}
				return this;
			}
		}.initialize();
		return result;
	}

	private void loopListener(ServerSocket listener, ForwardConfig config)
	{
		for (;;) {
			Socket server;
			try {
				server = listener.accept();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new UncheckedIOException(e);
			}
			runConnectionForward(server, config)
				.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
		}
	}

	private CompletableFuture<Void> runConnectionForward(Socket server, ForwardConfig config)
	{
		Socket client = null;
		try {
			switch (config.connectProto) {
			case "tcp4":
			case "tcp6":
				try {
					client = new Socket(getHost(config.connectProto, config.connectHost), config.connectPort);
				}
				catch (IOException ex) {
					throw new IOException("Failed to connect to: "+config.connectHost+":"+config.connectPort, ex);
				}
				break;

			case "unix":
				try {
					client = AFUNIXSocket.connectTo(new AFUNIXSocketAddress(new File(config.connectPath)));
				}
				catch (IOException ex) {
					throw new IOException("Failed to connect to: "+config.connectPath, ex);
				}
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
			return runServerClient(server, client);
		}
		catch (Throwable e) {
			try {
				try {
					server.shutdownInput();
					server.shutdownOutput();
				}
				finally {
					Closeables.close(client, true);
					Closeables.close(server, true);
				}
			}
			catch (IOException ex) {
				// ignore inner exception
			}
			return FutureUtil.exception(e);
		}
	}

	private CompletableFuture<Void> runOneForward(Socket one, Socket two)
	{
		CompletableFuture<Void> result = new CompletableFuture<>();
		new Thread(() -> {
			try {
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
			}
			catch (Throwable ex) {
				IOUtils.closeQuietly(two);
				result.completeExceptionally(ex);
			}
			result.complete(null);
		}).start();
		return result;
	}

	private CompletableFuture<Void> runListenerAsync(ForwardConfig config)
	{
		CompletableFuture<Void> future = new CompletableFuture<Void>() {
			private AsynchronousServerSocketChannel listener;

			@Override
			public synchronized boolean cancel(boolean interrupt) {
				if (listener != null) {
					closeChannel(this, listener);
				}
				return true;
			}

			public synchronized void setListener(AsynchronousServerSocketChannel listener) {
				this.listener = listener;
			}

			public CompletableFuture<Void> initialize()
			{
				try {
					Preconditions.checkArgument(!config.bindProto.equals("unix") && !config.connectProto.equals("unix"));
					switch (config.bindProto) {
					case "tcp4":
					case "tcp6":
						InetSocketAddress address = null;
						try {
							address = new InetSocketAddress(
								Optional.ofNullable(config.bindHost)
									.map(name -> {
											try {
												return getHost(config.bindProto, config.bindHost);
											}
											catch (UnknownHostException e) {
												throw new RuntimeException(e);
											}
										}
									)
									.orElse(null),
								config.bindPort
							);
							AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open();
							createdChannel(listener);
							listener.bind(address);
							this.setListener(listener);
						}
						catch (IOException ex) {
							throw new IOException("Failed to create listener on: "+address, ex);
						}
						break;

					default:
						throw new IllegalStateException("Improperly validated config: "+config);
					}
					listener.accept(
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
								completeExceptionally(exc);
							}
						}
					);
				}
				catch (Throwable ex) {
					this.completeExceptionally(ex);
				}
				return this;
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
						server = AsynchronousSocketChannel.open();
						createdChannel(server);
						InetSocketAddress address = new InetSocketAddress(getHost(config.connectProto, config.connectHost), config.connectPort);
						server.connect(
							address,
							0,
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
									this0.completeExceptionally(new IOException("Failed to connect to: "+address, exc));
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
								future.completeExceptionally(exc);
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

	private InetAddress getHost(String proto, String name) throws UnknownHostException
	{
		return Stream.of(Inet4Address.getAllByName(name))
			.filter(proto.equals("tcp4") ?
				Inet4Address.class::isInstance :
				Inet6Address.class::isInstance
			)
			.findFirst()
			.orElseThrow(() -> new UnknownHostException("Cannot find "+proto+" host for: "+name));
	}

	@Builder(builderClassName = "Builder")
	@Value
	public static class ForwardConfig
	{
		String bindProto;
		String bindHost;
		String bindPath;
		int bindPort;
		String connectProto;
		String connectHost;
		String connectPath;
		int connectPort;
	}
}
