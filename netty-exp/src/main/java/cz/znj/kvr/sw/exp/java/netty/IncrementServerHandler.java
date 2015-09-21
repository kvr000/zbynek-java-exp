package cz.znj.kvr.sw.exp.java.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class IncrementServerHandler extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		Long input = (Long)msg;
		try {
			logger.debug("Got {}", input);
			doSleep(0);
			ctx.writeAndFlush(input+1);
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
	}

	private void			doSleep(long ms)
	{
		try {
			if (ms > 0)
				Thread.sleep(ms);
		}
		catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	protected Logger		logger = LogManager.getLogger();
}
