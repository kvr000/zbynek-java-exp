package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.test;

import com.google.common.base.Stopwatch;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


@Log4j2
public class ClientServerTester implements AutoCloseable
{
	public static final long RUN_LENGTH = 2000;

	@Getter
	@Accessors(fluent = true)
	private final NettyEngine nettyEngine;

	private final List<Server> servers = new ArrayList<>();

	public ClientServerTester()
	{
		nettyEngine = new NettyEngine();
	}

	public void addServer(Server server)
	{
		servers.add(server);
	}

	public double runClientLoop(int batchSize, Function<NettyEngine, CompletableFuture<Void>> runClient)
	{
		long started = System.currentTimeMillis();
		Stopwatch stopwatch = Stopwatch.createStarted();
		AtomicInteger counter = new AtomicInteger(0);
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (int i = 0; i < Runtime.getRuntime().availableProcessors()*2*1+1; ++i) {
			Function<Void, CompletableFuture<Void>> code = new Function<Void, CompletableFuture<Void>>()
			{
				@Override
				public CompletableFuture<Void> apply(Void v)
				{
					if (System.currentTimeMillis()-started >= RUN_LENGTH) {
						return CompletableFuture.completedFuture(null);
					}
					else {
						counter.incrementAndGet();
						return runClient.apply(nettyEngine)
							.thenComposeAsync(this);
					}
				}
			};
			CompletableFuture<Void> future = CompletableFuture.completedFuture((Void) null)
				.thenComposeAsync(code);
			futures.add(future);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		double performance = counter.get() * 1_000_000_000.0 * batchSize / stopwatch.elapsed(TimeUnit.NANOSECONDS);
		log.info("Performance: time={} count={} ops/s={}", stopwatch.toString(), counter.get() * batchSize, performance);
		return performance;
	}

	public <T extends DuplexChannel> double runNettyClientLoop(
		int batchSize,
		SocketAddress connectAddress,
		Function<CompletableFuture<Void>, ChannelInitializer<T>> clientInitializer,
		Function<DuplexChannel, ? extends CompletionStage<Void>> runner
	)
	{
		return runClientLoop(
			batchSize,
			(runtime) -> new CompletableFuture<Void>()
			{
				{
					nettyEngine.connect("tcp4",
							connectAddress,
							clientInitializer.apply(this)
						)
						.thenCompose(runner);
				}
			}
		);
	}

	@Override
	public void close()
	{
		servers.forEach(s -> s.close());
	}
}
