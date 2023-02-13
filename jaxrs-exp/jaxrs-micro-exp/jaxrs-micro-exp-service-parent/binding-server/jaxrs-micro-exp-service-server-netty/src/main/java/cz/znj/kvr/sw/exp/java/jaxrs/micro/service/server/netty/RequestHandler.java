package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.netty;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


@Log
@RequiredArgsConstructor
public class RequestHandler extends SimpleChannelInboundHandler<HttpObject>
{
	private final ContainerContext container;

	private final RootServicer rootServicer;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;

	private final CompletableFuture<Void> connectionPromise;

	private HttpRequest request;

	private ByteBuf bodyBuf;

	public RequestHandler(ContainerContext container, RootServicer rootServicer, CompletableFuture<Void> connectionPromise)
	{
		this.rootServicer = rootServicer;
		this.container = container;
		this.responseExchangeBuilderProvider = container.getInjector().getInstance(ResponseExchangeBuilderProvider.class);
		this.connectionPromise = connectionPromise;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception
	{
		if (msg instanceof HttpRequest) {
			this.request = (HttpRequest) msg;
			ctx.read();
			bodyBuf = Unpooled.buffer();
		}
		else if (msg instanceof HttpContent) {
			bodyBuf.writeBytes(((HttpContent) msg).content());
			if (msg instanceof LastHttpContent) {
				handle(new NettyRequestExchange(responseExchangeBuilderProvider, request, bodyBuf, ctx));
			}
			else {
				ctx.read();
			}
		}
	}

	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.read();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		connectionPromise.complete(null);
		ctx.fireChannelInactive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
		throws Exception {
		connectionPromise.completeExceptionally(cause);
		ctx.close();
	}

	private void handle(RequestExchange exchange) throws IOException {
		log.info("start");
		rootServicer.call(exchange, container).join();
		log.info("end");
	}
}
