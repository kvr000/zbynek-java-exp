package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.test;

import com.google.common.base.Stopwatch;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.NettyRuntime;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * Simple Echo server and client running in high parallelism.
 */
@Log4j2
public class EchoTester
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new EchoTester().run(args));
	}

	public int run(String[] args) throws Exception
	{
		return execute();
	}

	public int execute() throws Exception
	{
		try (NettyRuntime nettyRuntime = new NettyRuntime()) {
			Stopwatch stopwatch = Stopwatch.createStarted();
			CompletableFuture<ServerChannel> serviceFuture = nettyRuntime.listen(
				"tcp4",
				InetSocketAddress.createUnresolved("localhost", 3302),
				new ChannelInitializer<DuplexChannel>()
				{
					@Override
					protected void initChannel(DuplexChannel ch) throws Exception
					{
						ch.pipeline().addLast(new EchoHandler());
					}
				}
			);
			AtomicInteger counter = new AtomicInteger(10_000);
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (int i = 0; i < 1000; ++i) {
				Function<Void, CompletableFuture<Void>> code = new Function<Void, CompletableFuture<Void>>()
				{
					@Override
					public CompletableFuture<Void> apply(Void v)
					{
						if (counter.decrementAndGet() <= 0) {
							return CompletableFuture.completedFuture(null);
						}
						else {
							return runClient(nettyRuntime)
								.thenComposeAsync(this);
						}
					}
				};
				CompletableFuture<Void> future = CompletableFuture.completedFuture((Void) null)
					.thenComposeAsync(code);
				futures.add(future);
			}
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
			serviceFuture.cancel(true);
			log.error("Time taken: "+stopwatch.toString());
		}
		return 0;
	}

	private CompletableFuture<Void> runClient(NettyRuntime nettyRuntime)
	{
		return new CompletableFuture<Void>()
		{
			private CompletableFuture<Void> future = this;

			{
				nettyRuntime.connect("tcp4",
						InetSocketAddress.createUnresolved("localhost", 3302),
						new ChannelInitializer<DuplexChannel>()
						{
							@Override
							protected void initChannel(DuplexChannel ch) throws Exception
							{
								ch.pipeline().addLast(new ThrowingHandler(future));
							}
						}
					)
					.thenCompose(channel -> {
						AtomicInteger counter = new AtomicInteger(1000);
						return CompletableFuture.completedFuture((Void) null)
							.thenComposeAsync(
								new Function<Void, CompletableFuture<Void>>()
								{
									@Override
									public CompletableFuture<Void> apply(Void arg)
									{
										if (counter.decrementAndGet() <= 0)
											return NettyFutures.toCompletable(channel.close());
										return NettyFutures.toCompletable(channel.writeAndFlush(Unpooled.wrappedBuffer("Hello world\n".getBytes(StandardCharsets.UTF_8))))
											.thenComposeAsync(this::apply);
									}
								}
							);
					});
			}
		};
	}

	private static class EchoHandler extends ChannelInboundHandlerAdapter
	{
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			ctx.writeAndFlush(msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx)
		{
			ctx.close();
		}
	}

	@RequiredArgsConstructor
	private static class ThrowingHandler extends ChannelInboundHandlerAdapter
	{
		private final CompletableFuture<Void> closedFuture;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			ReferenceCountUtil.release(msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx)
		{
			NettyFutures.completeOrFail(ctx.close(), closedFuture);
		}
	}
}
