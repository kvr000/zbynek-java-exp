package cz.znj.kvr.sw.exp.java.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public class IncrementDatagramHandler extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		DatagramMessage<Long> input = (DatagramMessage<Long>) msg;
		try {
			System.out.println("Got "+input.getContent());
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			ctx.writeAndFlush(new DatagramMessage<Long>(input.getContent()+1, input.getPeerAddress()));
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
