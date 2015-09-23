package cz.znj.kvr.sw.exp.java.netty;


import com.google.common.util.concurrent.MoreExecutors;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by rat on 2015-09-20.
 */
public class MixedTransportServer
{
	public static void		main(String[] args) throws Exception
	{
		System.exit(new MixedTransportServer().run(args));
	}

	public int			run(String[] args)
	{
		return this.process();
	}

	public int			process()
	{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap tcpb = new ServerBootstrap();
			tcpb.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
						ch.pipeline().addLast(new StringToLongDecoder());
						ch.pipeline().addLast(new LongToStringEncoder());
						ch.pipeline().addLast(new IncrementServerHandler());
					}
				})
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

			// Bind and start to accept incoming connections.
			ChannelFuture tcpf = tcpb.bind(port).sync();

			Bootstrap udpb = new Bootstrap();
			udpb.group(bossGroup)
				.channel(NioDatagramChannel.class)
				.handler(new SimpleChannelInboundHandler()
				{
					@Override
					protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception
					{
						try {
							DatagramPacket packet = ((DatagramPacket) msg).copy();
							executor.execute(() -> {
								try {
									InetSocketAddress client = packet.sender();
									ByteBuf content = packet.content().copy();
									logger.error("Got content from "+client+", io.netty.channel from "+ctx.channel().remoteAddress());
									Thread.sleep(5000);
									ctx.writeAndFlush(new DatagramPacket(content, client));
								}
								catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
							});
						}
						finally {
							//ReferenceCountUtil.release(msg);
						}
					}
				})
				.option(ChannelOption.SO_REUSEADDR, true);

			// Bind and start to accept incoming connections.
			ChannelFuture udpf = udpb.bind(port).sync();
			udpf.channel().pipeline()
//				.addFirst(new TimeServerHandler())
//				.addFirst(new StringToLongDecoder())
//				.addFirst(new LongToStringEncoder())
			;

			new Thread(() -> {
				try {
					Thread.sleep(200000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				tcpf.channel().close();
				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				udpf.channel().close();
			}).start();
			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to gracefully
			// shut down your server.
			tcpf.channel().closeFuture().awaitUninterruptibly();
			udpf.channel().closeFuture().awaitUninterruptibly();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			Future<?> shutdownWorkerFuture = workerGroup.shutdownGracefully();
			Future<?> shutdownBossFuture = bossGroup.shutdownGracefully();
			shutdownWorkerFuture.awaitUninterruptibly();
			shutdownBossFuture.awaitUninterruptibly();
		}
		return 0;
	}

	int				port = 4200;

	Logger				logger = LogManager.getLogger(getClass());

	Executor			executor = false ? MoreExecutors.newDirectExecutorService() : Executors.newCachedThreadPool();
}
