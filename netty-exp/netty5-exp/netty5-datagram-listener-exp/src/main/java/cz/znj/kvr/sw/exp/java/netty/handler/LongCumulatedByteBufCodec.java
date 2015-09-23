package cz.znj.kvr.sw.exp.java.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.Charsets;


/**
 * Created by rat on 2015-09-20.
 */
public class LongCumulatedByteBufCodec extends ChannelHandlerAdapter
{
	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		ByteBuf input = (ByteBuf) msg;
		try {
			for (;;) {
				int readerIndex = input.readerIndex();
				int newLine = input.indexOf(input.readerIndex(), input.writerIndex(), (byte)'\n');
				if (newLine < 0)
					break;
				input.readerIndex(newLine+1);
				Long value = Long.valueOf(input.toString(readerIndex, newLine-readerIndex, Charsets.UTF_8));
				ctx.fireChannelRead(value);
			}
		}
		finally {
			ReferenceCountUtil.release(input);
		}
	}

	@Override
	public void                     write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
	{
		ctx.write(Unpooled.wrappedBuffer((msg.toString()+"\n").getBytes()));
	}
}
