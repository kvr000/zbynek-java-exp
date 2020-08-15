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
import java.nio.file.Paths;
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
	private final AsynchronousChannelGroup channelGroup;

	private AtomicLong channelCount = new AtomicLong();

	@Inject
	public PortForwarder()
	{
		try {
			channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void createdChannel(Channel channel)
	{
		channelCount.incrementAndGet();
	}

	public <T> void close(CompletableFuture<T> future, Channel channel)
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
		catch (Exception ex) {
			return CompletableFuture.failedFuture(ex);
		}
		return runListener(forward);
	}

	public CompletableFuture<Void> runBothForward(AsynchronousSocketChannel one, AsynchronousSocketChannel two)
	{
		var future = new CompletableFuture<Void>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				close(this, one);
				close(this, two);
				return true;
			}
		};
		CompletableFuture<Void> there = runOneForward(one, two);
		CompletableFuture<Void> back = runOneForward(two, one);
		CompletableFuture.allOf(there, back)
			.whenComplete((v, ex) -> {
				close(null, one);
				close(null, two);
				FutureUtil.completeOrFail(future, v, ex);
			});
		return future;
	}

	public void connect(SocketAddress address, Consumer<AsynchronousSocketChannel> initializer, CompletionHandler<Void, Integer> handler)
	{
		AsynchronousSocketChannel socket = null;
		try {
			socket = AsynchronousSocketChannel.open(channelGroup);
			createdChannel(socket);
			initializer.accept(socket);
			socket.connect(address, 0, handler);
		}
		catch (Throwable e) {
			close(null, socket);
			handler.failed(new IOException("Failed to connect to: "+address, e), 0);
			return;
		}
	}

	public CompletableFuture<Void> writeFully(AsynchronousSocketChannel socket, ByteBuffer buffer)
	{
		if (!buffer.hasRemaining())
			return CompletableFuture.completedFuture(null);

		CompletableFuture<Void> future = new CompletableFuture<>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				close(this, socket);
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
			listener = AsynchronousServerSocketChannel.open(channelGroup);
			createdChannel(listener);
			return initializer.apply(listener);
		}
		catch (Throwable ex) {
			close(null, listener);
			return CompletableFuture.failedFuture(ex);
		}
	}

	public CompletableFuture<Void> runServerClient(
		Socket server,
		InputStream serverInput,
		OutputStream serverOutput,
		Socket client,
		InputStream clientInput,
		OutputStream clientOutput
	)
	{
		CompletableFuture<Void> one = runOneForward(server, client, serverInput, clientOutput);
		CompletableFuture<Void> two = runOneForward(client, server, clientInput, serverOutput);
		return CompletableFuture.allOf(one, two)
			.whenComplete((v, ex) -> {
				IOUtils.closeQuietly(client, server);
			});
	}

	@Override
	public void close() throws InterruptedException
	{
		channelGroup.shutdown();
		channelGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	private CompletableFuture<Void> runListener(ForwardConfig config)
	{
		if (!config.bindProto.equals("unix") && !config.connectProto.equals("unix")) {
			return runListenerAsync(config);
		}
		// For unix sockets which don't support asynchronous interface, use old-school accept and fork pattern:
		var result = new CompletableFuture<Void>() {
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

			public synchronized void setListener(ServerSocket listener) {
				this.listener = listener;
			}
		};
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
					result.setListener(new ServerSocket(
						address.getPort(),
						0,
						address.getAddress()
					));
				}
				catch (IOException ex) {
					throw new IOException("Failed to create listener on "+address, ex);
				}
				break;

			case "unix":
				try {
					result.setListener(AFUNIXServerSocket.bindOn(Paths.get(config.bindPath), true));
				}
				catch (IOException ex) {
					throw new IOException("Failed to create listener on "+config.bindPath, ex);
				}
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
			new Thread(() -> loopListener(result.listener, config)).start();
		}
		catch (IOException ex) {
			result.completeExceptionally(ex);
		}
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
					client = AFUNIXSocket.connectTo(AFUNIXSocketAddress.of(Paths.get(config.connectPath)));
				}
				catch (IOException ex) {
					throw new IOException("Failed to connect to: "+config.connectPath, ex);
				}
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
			return runServerClient(server, server.getInputStream(), server.getOutputStream(), client, client.getInputStream(), client.getOutputStream());
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
			return CompletableFuture.failedFuture(e);
		}
	}

	private CompletableFuture<Void> runOneForward(Socket one, Socket two, InputStream oneInput, OutputStream twoOutput)
	{
		CompletableFuture<Void> result = new CompletableFuture<>();
		new Thread(() -> {
			try {
				int size;
				byte[] buf = new byte[4096];
				while ((size = oneInput.read(buf)) > 0) {
					try {
						twoOutput.write(buf, 0, size);
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
		var future = new CompletableFuture<Void>() {
			private AsynchronousServerSocketChannel listener;

			@Override
			public synchronized boolean cancel(boolean interrupt) {
				if (listener != null) {
					close(this, listener);
				}
				return true;
			}

			public synchronized void setListener(AsynchronousServerSocketChannel listener) {
				this.listener = listener;
			}
		};
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
					AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(channelGroup);
					createdChannel(listener);
					listener.bind(address);
					future.setListener(listener);
				}
				catch (IOException ex) {
					throw new IOException("Failed to create listener on: "+address, ex);
				}
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
			future.listener.accept(
				0,
				new CompletionHandler<AsynchronousSocketChannel, Integer>()
				{
					@Override
					public void completed(AsynchronousSocketChannel result, Integer attachment)
					{
						createdChannel(result);
						future.listener.accept(0, this);
						runConnectionForward(result, config)
							.whenComplete(FutureUtil.whenException(Throwable::printStackTrace));
					}

					@Override
					public void failed(Throwable exc, Integer attachment)
					{
						future.completeExceptionally(exc);
					}
				}
			);
		}
		catch (Throwable ex) {
			future.completeExceptionally(ex);
		}
		return future;
	}

	private CompletableFuture<Void> runConnectionForward(AsynchronousSocketChannel client, ForwardConfig config)
	{
		var future = new CompletableFuture<Void>() {
			AsynchronousSocketChannel server;

			@Override
			public synchronized boolean cancel(boolean interrupt)
			{
				if (!super.cancel(interrupt))
					return false;
				close(this, client);
				close(this, server);
				return true;
			}
		};
		try {
			switch (config.connectProto) {
			case "tcp4":
			case "tcp6":
				future.server = AsynchronousSocketChannel.open(channelGroup);
				createdChannel(future.server);
				InetSocketAddress address = new InetSocketAddress(getHost(config.connectProto, config.connectHost), config.connectPort);
				future.server.connect(
					address,
					0,
					new CompletionHandler<Void, Integer>()
					{
						@Override
						public void completed(Void result, Integer attachment)
						{
							runBothForward(client, future.server)
								.whenComplete((v, ex) -> FutureUtil.completeOrFail(future, v, ex));
						}

						@Override
						public void failed(Throwable exc, Integer attachment)
						{
							future.completeExceptionally(new IOException("Failed to connect to: "+address, exc));
						}
					}
				);
				break;

			default:
				throw new IllegalStateException("Improperly validated config: "+config);
			}
		}
		catch (Throwable e) {
			future.completeExceptionally(e);
		}
		CompletableFuture<Void> last = new CompletableFuture<>();
		future.whenComplete((v, ex) -> {
			if (ex != null)
				last.completeExceptionally(ex);
			close(last, client);
			close(last, future.server);
			last.complete(v);
		});
		return last;
	}

	private CompletableFuture<Void> runOneForward(AsynchronousSocketChannel reader, AsynchronousSocketChannel writer)
	{
		var future = new CompletableFuture<Void>();
		try {
			ByteBuffer buf = ByteBuffer.allocate(4096);
			reader.read(buf, 0, new CompletionHandler<Integer, Integer>()
			{
				@Override
				public void completed(Integer result, Integer attachment)
				{
					var readerCompleter = this;

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
