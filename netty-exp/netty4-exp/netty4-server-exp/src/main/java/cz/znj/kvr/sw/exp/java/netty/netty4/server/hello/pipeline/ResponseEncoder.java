package cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline;

import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.model.HelloResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;


/**
 *
 */
public class ResponseEncoder extends ChannelOutboundHandlerAdapter
{
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		HelloResponse input = (HelloResponse) msg;
		String output = input.getGreeting();
		ctx.write(output, promise);
	}
}
