package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.io.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class StringToLongDecoder extends ChannelHandlerAdapter
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
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
