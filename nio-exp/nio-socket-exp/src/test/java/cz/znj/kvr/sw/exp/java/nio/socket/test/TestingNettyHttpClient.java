package cz.znj.kvr.sw.exp.java.nio.socket.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;


/**
 * Simple HTTP server for testing.
 */
public class TestingNettyHttpClient
{
	InetSocketAddress serverAddress = new InetSocketAddress("localhost", 4444);

	EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

	Bootstrap bootstrap;

	AtomicLong nextStats = new AtomicLong(System.currentTimeMillis());

	long lastPerfCount = 0;
	AtomicLong perfCount = new AtomicLong();

	long endTime = nextStats.get()+60_000;


	public static void main(String[] args) throws Exception
	{
		new TestingNettyHttpClient().run(args);
	}

	private int run(String[] args) throws Exception
	{
		return execute();
	}

	private int execute() throws Exception
	{
		bootstrap = new Bootstrap();
		bootstrap.group(eventLoopGroup)
			.channel(NioSocketChannel.class)
			.handler(new ClientInitializer());

		IntStream.range(0, 1024)
			.mapToObj((index) -> runStream(index))
			.toArray(CompletableFuture[]::new);

		return 0;
	}

	private CompletableFuture<Void> runStream(int index)
	{
		return runOneRequest()
			.thenCompose(r -> finishedRequest(index));
	}

	private CompletableFuture<Void> finishedRequest(int index)
	{
		perfCount.incrementAndGet();
		long time = System.currentTimeMillis();
		long nextStatsValue = nextStats.get();
		if (time >= nextStatsValue && nextStats.compareAndSet(nextStatsValue, nextStatsValue+1_000)) {
			// well, we should have some better synchronization including above, but conflict very unlikely
			synchronized (this) {
				System.err.println("Request/s: "+(perfCount.get()-lastPerfCount));
				lastPerfCount = perfCount.get();
			}
		}
		if (time >= endTime) {
			return CompletableFuture.completedFuture(null);
		}
		return runStream(index);
	}

	private CompletableFuture<Void> runOneRequest()
	{
		CompletableFuture<Void> result = new CompletableFuture<>();
		bootstrap.connect(serverAddress)
			.addListener(
				new GenericFutureListener<ChannelFuture>()
				{
					@Override
					public void operationComplete(ChannelFuture future) throws Exception
					{
						try {
							future.get();
							HttpRequest request = new DefaultFullHttpRequest(
								HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
							request.headers().set(HttpHeaders.Names.HOST, "localhost:9999");
							request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
							request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
							future.channel().writeAndFlush(request);
							future.channel().closeFuture()
								.addListener((v) -> {
									try {
										result.complete((Void)v.get());
									}
									catch (Throwable ex) {
										result.completeExceptionally(ex);
									}
								});
						}
						catch (Throwable ex) {
							result.completeExceptionally(ex);
						}
					}
				}
		);
		return result;
	}

	public class ClientHandler extends SimpleChannelInboundHandler<HttpObject>
	{
		@Override
		public void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
		{
			if (msg instanceof HttpContent) {
				HttpContent content = (HttpContent)msg;

				if (content instanceof LastHttpContent) {
					ctx.close();
				}
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
		{
			cause.printStackTrace();
			ctx.close();
		}
	}

	public class ClientInitializer extends ChannelInitializer<SocketChannel>
	{
		@Override
		public void initChannel(SocketChannel ch)
		{
			ChannelPipeline p = ch.pipeline();
			p.addLast(new HttpClientCodec());
			p.addLast(new HttpContentDecompressor());
			p.addLast(new ClientHandler());
		}
	}
}
