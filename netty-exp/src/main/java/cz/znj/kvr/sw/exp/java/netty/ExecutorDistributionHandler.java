package cz.znj.kvr.sw.exp.java.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executor;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class ExecutorDistributionHandler extends ChannelHandlerAdapter
{
	public ExecutorDistributionHandler(Executor executor)
	{
		this.executor = executor;
	}

	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		executor.execute(() -> {
			logger.error("Got message "+msg);
			ctx.fireChannelRead(msg);
		});
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	protected Executor		executor;

	protected Logger		logger = LogManager.getLogger();
}
