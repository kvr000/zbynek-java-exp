package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpserver;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyServer;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.kqueue.AbstractKQueueStreamChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DummyHttpServerFactory
{
	private final NettyEngine nettyEngine;

	public CompletableFuture<Server> runServer(Config config, BiFunction<String, String, String> requestHandler)
	{
		try {
			return new CompletableFuture<Server>() {
				CompletableFuture<ServerChannel> listenFuture;

				{
					listenFuture = nettyEngine.listen(
							config.getListenAddress(),
							new ChannelInitializer<DuplexChannel>()
							{
								@Override
								protected void initChannel(DuplexChannel ch) throws Exception
								{
									runServer(ch, requestHandler);
								}
							}
						)
						.whenComplete((v, ex) -> {
							if (ex != null) {
								completeExceptionally(ex);
							}
							else {
								complete(new NettyServer(
									v
								));
							}
						});
				}

				@Override
				public boolean cancel(boolean interrupt)
				{
					listenFuture.cancel(true);
					return super.cancel(interrupt);
				}
			};
		}
		catch (Throwable ex) {
			return FutureUtil.exception(ex);
		}
	}

	private CompletableFuture<Void> runServer(Channel client, BiFunction<String, String, String> requestHandler)
	{
		return new CompletableFuture<Void>() {
			{
				client.pipeline().addLast(
					new HttpRequestDecoder(),
					new HttpResponseEncoder(),
					new RequestHandler(this, requestHandler)
				);
				whenComplete((v, ex) -> {
					if (ex != null) {
						client.close();
						log.error("Error processing HTTP request", ex);
					}
				});
			}
		};
	}

	@Value
	@Builder(builderClassName = "Builder")
	public static class Config
	{
		AddressSpec listenAddress;
	}

	@RequiredArgsConstructor
	private class RequestHandler extends SimpleChannelInboundHandler<HttpObject>
	{
		private final CompletableFuture<Void> connectionFuture;

		private final BiFunction<String, String, String> requestHandler;

		HttpRequest request;

		@Override
		public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception
		{
			if (msg instanceof HttpRequest) {
				this.request = (HttpRequest) msg;
				ctx.read();
			}
			else if (msg instanceof LastHttpContent) {
				String result;
				try {
					result = requestHandler.apply(request.method().toString(), request.uri());
				}
				catch (Exception ex) {
					writeResponse(ctx, new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1,
						HttpResponseStatus.INTERNAL_SERVER_ERROR,
						Unpooled.wrappedBuffer(ex.toString().getBytes(StandardCharsets.UTF_8))
					));
					return;
				}
				if (result == null) {
					writeResponse(ctx, new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1,
						HttpResponseStatus.NOT_FOUND
					));
				}
				else {
					writeResponse(ctx, new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1,
						HttpResponseStatus.OK,
						Unpooled.wrappedBuffer(result.getBytes(StandardCharsets.UTF_8))
					));
				}
			}
		}

		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ctx.read();
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			connectionFuture.complete(null);
			ctx.fireChannelInactive();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
			connectionFuture.completeExceptionally(cause);
			ctx.close();
		}

		private void writeResponse(ChannelHandlerContext ctx, DefaultFullHttpResponse response)
		{
			ctx.executor().schedule(() -> {
				boolean keepAlive = HttpUtil.isKeepAlive(request);

				response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

				response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH,
					response.content().readableBytes());
				if (keepAlive) {
					response.headers().set(HttpHeaderNames.CONNECTION,
						HttpHeaderValues.KEEP_ALIVE);
				}
				try {
					ChannelFuture future = ctx.writeAndFlush(response);
					future.addListener(f -> { if (f.isSuccess()) ctx.read(); });
					return future;
				}
				finally {
					if (!keepAlive) {
						nettyEngine.writeAndClose((DuplexChannel) ctx.channel(), Unpooled.EMPTY_BUFFER);
					}
				}
			}, ctx.channel() instanceof AbstractKQueueStreamChannel ? 100 : 0, TimeUnit.MILLISECONDS);
		}
	}
}
