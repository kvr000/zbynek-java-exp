package cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline;

import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.model.HelloRequest;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.model.HelloResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 *
 */
public class ProcessingHandler extends ChannelInboundHandlerAdapter
{
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		HelloRequest input = (HelloRequest) msg;
		HelloResponse output = new HelloResponse();
		output.setGreeting("Hello, " + input.getName());
		ctx.writeAndFlush(output);
	}
}
