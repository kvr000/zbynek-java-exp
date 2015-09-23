package cz.znj.kvr.sw.exp.java.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
public class CumulatingStringBuilderHandler extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		ByteBuf input = (ByteBuf) msg;
		try {
			cumulator.append(input.toString(Charsets.UTF_8));
		}
		finally {
			ReferenceCountUtil.release(input);
		}
		ctx.fireChannelRead(cumulator);
	}

	protected StringBuilder         cumulator = new StringBuilder();
}
