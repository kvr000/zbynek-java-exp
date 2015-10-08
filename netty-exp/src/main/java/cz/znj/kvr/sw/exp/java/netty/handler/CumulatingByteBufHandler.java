package cz.znj.kvr.sw.exp.java.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;


/**
 * Created by rat on 2015-09-20.
 */
public class CumulatingByteBufHandler extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		ByteBuf input = (ByteBuf) msg;
		try {
			cumulator.writeBytes(input);
		}
		finally {
			ReferenceCountUtil.release(input);
		}
		ctx.fireChannelRead(cumulator);
		cumulator.discardReadBytes();
	}

	@Override
	public void                     finalize()
	{
		ReferenceCountUtil.release(cumulator);
	}

	protected ByteBuf               cumulator = Unpooled.unreleasableBuffer(Unpooled.buffer());
}
