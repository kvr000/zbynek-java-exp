package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.io.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
public class LongBytesAdapter extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		byte[] input = (byte[]) msg;
		try {
			String s = new String(input).trim();
			ctx.fireChannelRead(Long.parseLong(s));
		}
		finally {
		}
	}

	@Override
	public void			write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
	{
		Long input = (Long)msg;
		byte[] output = input.toString().getBytes();
		ctx.writeAndFlush(output);
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
