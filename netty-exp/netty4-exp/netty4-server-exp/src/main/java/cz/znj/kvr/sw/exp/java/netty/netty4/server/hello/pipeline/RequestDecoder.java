package cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline;

import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.model.HelloRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;


/**
 *
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter
{
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf input = (ByteBuf) msg;
		HelloRequest output = new HelloRequest();
		output.setName((String) input.toString(StandardCharsets.UTF_8));
		ctx.fireChannelRead(output);
	}
}
