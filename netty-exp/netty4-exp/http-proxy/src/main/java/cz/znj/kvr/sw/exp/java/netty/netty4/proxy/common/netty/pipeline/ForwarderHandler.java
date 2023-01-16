package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.pipeline;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyFutures;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.DuplexChannelConfig;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.CompletableFuture;


/**
 * Forwards one direction of connection.
 */
@Log4j2
public class ForwarderHandler extends ChannelInboundHandlerAdapter
{
	private final NettyEngine nettyEngine;

	private final DuplexChannel source;

	private final DuplexChannel destination;

	private final CompletableFuture<Void> finishPromise;

	private ChannelConfig config;

	private boolean isShutdown;

	public ForwarderHandler(
		NettyEngine nettyEngine,
		DuplexChannel source,
		DuplexChannel destination,
		CompletableFuture<Void> finishPromise
	)
	{
		this.nettyEngine = nettyEngine;
		this.source = source;
		this.destination = destination;
		this.finishPromise = finishPromise;
		this.config = source.config();

		config.setAutoRead(false);
		config.setAutoClose(false);
		if (config instanceof DuplexChannelConfig config0) {
			config0.setAllowHalfClosure(true);
		}

		source.closeFuture().addListener(f -> {
			NettyFutures.copy(source.closeFuture(), finishPromise);
		});
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception
	{
		super.handlerAdded(ctx);
		ctx.read();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		ByteBuf input = (ByteBuf) msg;
		destination.write(input);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
	{
		ChannelFuture future = destination.writeAndFlush(Unpooled.EMPTY_BUFFER);
		future.addListener((f) -> {
			ctx.read();
		});
	}

	@Override
      	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
	{
		if (evt instanceof ChannelInputShutdownEvent) {
			isShutdown = true;
			NettyFutures.copy(nettyEngine.shutdownOutput(destination), finishPromise);
		}
		else {
			ctx.read();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable ex) throws Exception
	{
		finishPromise.completeExceptionally(ex);
		ctx.close();
	}

	public boolean isShutdown()
	{
		return isShutdown;
	}
}
