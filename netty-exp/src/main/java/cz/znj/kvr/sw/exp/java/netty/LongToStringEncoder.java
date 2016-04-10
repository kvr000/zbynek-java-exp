package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class LongToStringEncoder extends ChannelHandlerAdapter
{
	@Override
	public void			write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) { // (2)
		Long input = (Long)msg;
		ByteBuf buf = ctx.alloc().buffer();
		buf.writeBytes((input.toString()+"\n").getBytes());
		ctx.writeAndFlush(buf);
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
