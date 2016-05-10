package cz.znj.kvr.sw.exp.java.nio.socket.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;


/**
 * Simple HTTP server for testing.
 */
public class TestingNettyHttpServer
{
	public static void main(String[] args) throws Exception
	{
		new TestingNettyHttpServer().run(args);
	}

	private int run(String[] args) throws Exception
	{
		return execute();
	}

	private int execute() throws Exception
	{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new TestServerInitializer());

			ChannelFuture channelFuture = serverBootstrap.bind("localhost", 9999).sync();
			System.out.println("server is ready");
			channelFuture.channel().closeFuture().sync();
		}
		finally {
			//Error closing connection gracefully
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
		return 0;
	}

	private static class TestServerInitializer extends ChannelInitializer<SocketChannel>
	{
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			//Get pipe
			ChannelPipeline pipeline = ch.pipeline();
			//HTTP codec provided by netty
			pipeline.addLast("myHttpServerCodec",new HttpServerCodec());
			//Add custom processor
			pipeline.addLast("myHttpServerHandler",new TestHttpServerHandler());
		}
	}

	public static class TestHttpServerHandler extends SimpleChannelInboundHandler<HttpObject>
	{
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

			if (msg instanceof HttpRequest) {
				HttpRequest httpRequest = (HttpRequest) msg;

				ByteBuf content = Unpooled.copiedBuffer("hello，I am the server\n", StandardCharsets.UTF_8);
				DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

				response.headers().set("content-type", "text/plain;charset=UTF-8");
				response.headers().set("content-length", content.readableBytes());

				ctx.writeAndFlush(response);
				if (httpRequest.headers().contains("connection", "close", true))
					ctx.close();
			}

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			ctx.close();
		}
	}
}
