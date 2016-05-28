package cz.znj.kvr.sw.exp.java.netty.netty4.server.hello;

import cz.znj.kvr.sw.exp.java.netty.netty4.server.common.NettyRuntime;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline.ProcessingHandler;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline.RequestDecoder;
import cz.znj.kvr.sw.exp.java.netty.netty4.server.hello.pipeline.ResponseEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.LineSeparator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;


/**
 * Echo server runner.
 */
public class HelloRunner
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new HelloRunner().run(args));
	}

	public int run(String[] args) throws IOException, InterruptedException
	{
		try (NettyRuntime nettyRuntime = new NettyRuntime()) {
			nettyRuntime.listen(
				null,
				new InetSocketAddress("localhost", 4100),
				new ChannelInitializer<DuplexChannel>() {
					@Override
					public void initChannel(DuplexChannel ch)
						throws Exception {
						ch.pipeline().addLast(
							new LineBasedFrameDecoder(1024, true, true),
							new LineEncoder(LineSeparator.UNIX, StandardCharsets.UTF_8),
							new RequestDecoder(),
							new ResponseEncoder(),
							new ProcessingHandler());
					}
				}
			);
		}
		return 0;
	}
}
