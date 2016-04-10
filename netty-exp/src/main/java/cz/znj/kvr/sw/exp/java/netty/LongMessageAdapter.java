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
public class LongMessageAdapter extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
		ByteBuf buf = (ByteBuf)msg;
		try {
			String s = buf.toString(Charsets.UTF_8);
			ctx.fireChannelRead(Long.parseLong(s));
		}
		finally {
			buf.release(); // (3)
		}
	}

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
