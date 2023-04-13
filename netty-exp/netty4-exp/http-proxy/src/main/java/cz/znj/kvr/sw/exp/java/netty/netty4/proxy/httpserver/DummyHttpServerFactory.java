package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.kqueue.AbstractKQueueStreamChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.base.function.ThrowingBiFunction;
import net.dryuf.base.function.delegate.TypeDelegatingTriFunction3;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyEngine;
import net.dryuf.netty.core.NettyServer;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.pipeline.TypeDistributingInboundHandler;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DummyHttpServerFactory
{
	private final NettyEngine nettyEngine;

	public CompletableFuture<Server> runServer(Config config, ThrowingBiFunction<String, String, String, Exception> requestHandler)
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

	private CompletableFuture<Void> runServer(Channel client, ThrowingBiFunction<String, String, String, Exception> requestHandler)
	{
		return new CompletableFuture<Void>() {
			{
				client.pipeline().addLast(
					new HttpServerCodec(),
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

	private class RequestHandler extends TypeDistributingInboundHandler<RequestHandler, HttpObject, Exception>
	{
		public static final TypeDelegatingTriFunction3<RequestHandler, ChannelHandlerContext, HttpObject, Void, Exception> READ_DISTRIBUTION =
			TypeDelegatingTriFunction3.<RequestHandler, ChannelHandlerContext, HttpObject, Void, Exception>callbacksBuilder()
				.add(HttpRequest.class, RequestHandler::channelReadRequest)
				.add(LastHttpContent.class, RequestHandler::channelReadLastContent)
				.add(HttpObject.class, (a, b, c) -> null)
				.build();

		private final CompletableFuture<Void> connectionFuture;

		private final ThrowingBiFunction<String, String, String, Exception> requestHandler;

		HttpRequest request;

		public RequestHandler(CompletableFuture<Void> connectionFuture, ThrowingBiFunction<String, String, String, Exception> requestHandler)
		{
			super(READ_DISTRIBUTION);
			this.connectionFuture = connectionFuture;
			this.requestHandler = requestHandler;
		}

		private Void channelReadRequest(ChannelHandlerContext ctx, HttpRequest msg)
		{
			this.request = msg;
			ctx.read();
			return null;
		}

		private Void channelReadLastContent(ChannelHandlerContext ctx, LastHttpContent msg)
		{
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
				return null;
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
			return null;
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
