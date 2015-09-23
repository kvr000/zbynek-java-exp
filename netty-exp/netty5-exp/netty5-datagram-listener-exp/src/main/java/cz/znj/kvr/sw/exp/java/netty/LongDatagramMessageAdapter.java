package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.io.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class LongDatagramMessageAdapter extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		DatagramMessage<byte[]> message = (DatagramMessage<byte[]>) msg;
		String s = new String(message.getContent(), Charsets.UTF_8).trim();
		ctx.fireChannelRead(new DatagramMessage<Long>(Long.parseLong(s), message.getPeerAddress()));
	}

	@Override
	public void			write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
	{
		DatagramMessage<Long> input = (DatagramMessage<Long>) msg;
		DatagramMessage<byte[]> output = new DatagramMessage<>(String.valueOf(input.getContent()).getBytes(), input.getPeerAddress());
		ctx.writeAndFlush(output);
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
