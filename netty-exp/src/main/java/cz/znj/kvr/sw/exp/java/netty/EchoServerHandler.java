package cz.znj.kvr.sw.exp.java.netty;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;


/**
 * Created by rat on 2015-09-20.
 */
public class EchoServerHandler extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
		try {
			ctx.writeAndFlush(msg);
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
