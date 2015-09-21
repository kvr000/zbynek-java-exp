package cz.znj.kvr.sw.exp.java.netty;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.Future;


/**
 * Created by rat on 2015-09-20.
 */
public class IncrementServer
{
	public static void		main(String[] args) throws Exception
	{
		System.exit(new IncrementServer().run(args));
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
			ServerBootstrap b1 = new ServerBootstrap();
			b1.group(bossGroup, workerGroup)
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
//				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_REUSEADDR, true);

			// Bind and start to accept incoming connections.
			ChannelFuture f1 = b1.bind(port).sync();

			ServerBootstrap b2 = new ServerBootstrap();
			b2.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
						ch.pipeline().addLast(new LongMessageAdapter());
						ch.pipeline().addLast(new IncrementServerHandler());
					}
				})
//				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_REUSEADDR, true);

			// Bind and start to accept incoming connections.
			ChannelFuture f2 = b2.bind(port+1).sync();

			new Thread(() -> {
				try {
					Thread.sleep(200000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				f1.channel().close();
				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				f2.channel().close();
			}).start();
			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to gracefully
			// shut down your server.
			f1.channel().closeFuture().awaitUninterruptibly();
			f2.channel().closeFuture().awaitUninterruptibly();
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

	public static final int		port = 4200;
}
