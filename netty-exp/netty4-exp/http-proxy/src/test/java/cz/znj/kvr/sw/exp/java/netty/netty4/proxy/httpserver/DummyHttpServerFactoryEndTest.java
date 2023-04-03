package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyEngine;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.test.ClientServerTester;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;


@Log4j2
public class DummyHttpServerFactoryEndTest
{
	@Test(timeOut = 30_000L)
	public void testHttpServer() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = runHttpServer(tester);

			runHttpClient(tester, serverAddress.getHostString(), 100, serverAddress);
		}
	}

	public static InetSocketAddress runHttpServer(ClientServerTester tester)
	{
		return runHttpServer(tester, InetSocketAddress.createUnresolved("localhost", 0));
	}

	public static <T extends SocketAddress> T runHttpServer(ClientServerTester tester, T listenAddress)
	{
		Server server = new DummyHttpServerFactory(tester.nettyEngine()).runServer(
			DummyHttpServerFactory.Config.builder()
				.listenAddress(AddressSpec.fromSocketAddress(listenAddress))
				.build(),
			(method, path) -> "Hello World\n"
		).join();
		tester.addServer(server);
		@SuppressWarnings("unchecked")
		T address = (T) server.listenAddress();
		log.info("HttpServer listening: {}", address);
		return address;
	}

	public static double runHttpClient(ClientServerTester tester, String hostname, int attempts, SocketAddress serverAddress)
	{
		AtomicInteger pending = new AtomicInteger();
		return tester.runNettyClientLoop(
			ClientServerTester.TestConfig.builder().batchSize(attempts).build(),
			serverAddress,
			(future) -> new ChannelInitializer<DuplexChannel>()
			{
				@Override
				protected void initChannel(DuplexChannel ch) throws Exception
				{
					ch.pipeline().addLast(
						new HttpRequestEncoder(),
						new HttpResponseDecoder(),
						new ClientHandler(tester.nettyEngine(), hostname, attempts, future, pending)
					);
					ch.config().setAutoRead(true);
				}
			},
			(channel) -> {
				return channel.pipeline().get(ClientHandler.class).closedPromise;
			}
		);
	}

	@RequiredArgsConstructor
	private static class ClientHandler extends ChannelInboundHandlerAdapter
	{
		private final NettyEngine nettyEngine;

		private final String hostname;

		private final int attempts;

		private final CompletableFuture<Void> closedPromise;

		private final AtomicInteger pending;

		private final AtomicInteger counter = new AtomicInteger();

		ByteBuf fullContent;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			try {
				if (msg instanceof HttpContent msg0) {
					fullContent.writeBytes(msg0.content());
					if (msg instanceof LastHttpContent) {
						try {
							assertEquals(fullContent.toString(StandardCharsets.UTF_8), "Hello World\n");
						}
						catch (Throwable ex) {
							closedPromise.completeExceptionally(ex);
						}
						finally {
							log.info("Pending: {}", pending.decrementAndGet());
						}
						nextRequest(ctx);
						return;
					}
				}
				ctx.read();
			}
			finally {
				ReferenceCountUtil.release(msg);
			}
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx)
		{
			counter.set(attempts);
			nextRequest(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx)
		{
			ctx.close().addListener(f ->
				closedPromise.completeExceptionally(new IOException("Channel unexpectedly closed"))
			);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
			log.error("Exception in client:", cause);
			ctx.close();
			closedPromise.completeExceptionally(cause);
		}

		private void nextRequest(ChannelHandlerContext ctx)
		{
			fullContent = Unpooled.buffer();
			if (counter.decrementAndGet() < 0) {
				nettyEngine.writeAndClose((DuplexChannel) ctx.channel(), Unpooled.EMPTY_BUFFER);
				closedPromise.complete(null);
			}
			else {
				ctx.writeAndFlush(new DefaultFullHttpRequest(
						HttpVersion.HTTP_1_1,
						HttpMethod.POST,
						"/hello",
						Unpooled.wrappedBuffer("content\n".getBytes(StandardCharsets.UTF_8)),
						new DefaultHttpHeaders()
							.add(HttpHeaderNames.HOST, hostname)
							.add(HttpHeaderNames.CONTENT_LENGTH, 8),
						EmptyHttpHeaders.INSTANCE
					))
					.addListener(f -> {
						if (f.isSuccess()) {
							pending.incrementAndGet();
							ctx.read();
						}
						else {
							closedPromise.completeExceptionally(f.cause());
						}
					});
			}
		}
	}
}
