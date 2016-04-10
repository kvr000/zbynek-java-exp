package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class DatagramMessageCodec extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		DatagramPacket packet = (DatagramPacket) msg;
		try {
			byte[] content = new byte[packet.content().readableBytes()];
			packet.content().readBytes(content);
			ctx.fireChannelRead(new DatagramMessage<byte[]>(content, packet.sender()));
		}
		finally {
			//ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void			write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
	{
		DatagramMessage<byte[]> input = (DatagramMessage<byte[]>) msg;
		ByteBuf buf = Unpooled.wrappedBuffer(input.getContent());
		ctx.writeAndFlush(new DatagramPacket(buf, input.getPeerAddress()));
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
