package cz.znj.kvr.sw.exp.java.netty.netty4.server.common.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DuplexChannel;
import lombok.RequiredArgsConstructor;


/**
 * Forwards one direction of connection.
 */
@RequiredArgsConstructor
public class ForwarderHandler extends ChannelInboundHandlerAdapter
{
	private final DuplexChannel writeChannel;

	private final ChannelPromise finishPromise;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf input = (ByteBuf) msg;
		ChannelFuture future = writeChannel.writeAndFlush(input);
		if (!writeChannel.isWritable()) {
			ctx.channel().config().setAutoRead(false);
			future.addListener((f) -> ctx.channel().config().setAutoRead(true));
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		writeChannel.shutdownOutput(finishPromise);
	}
}
