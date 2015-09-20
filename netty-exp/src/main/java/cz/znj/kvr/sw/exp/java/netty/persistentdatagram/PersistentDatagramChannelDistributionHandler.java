package cz.znj.kvr.sw.exp.java.netty.persistentdatagram;

import cz.znj.kvr.sw.exp.java.netty.netty.MyEmbeddedEventLoop;
import cz.znj.kvr.sw.exp.java.netty.persistentdatagram.PersistentDatagramChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executor;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public abstract class PersistentDatagramChannelDistributionHandler extends ChannelHandlerAdapter
{
	public PersistentDatagramChannelDistributionHandler(EventLoopGroup workersGroup)
	{
		this.workersGroup = workersGroup;
	}

	@Override
	public void			channelRead(ChannelHandlerContext ctx, Object msg)
	{
		DatagramPacket packet = (DatagramPacket) msg;
		PersistentDatagramChannel childChannel = new PersistentDatagramChannel((DatagramChannel)ctx.channel(), packet.sender());
		initChildChannel(childChannel);
		workersGroup.register(childChannel);
		ByteBuf content = Unpooled.copiedBuffer(packet.content());
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes(bytes);
//		executor.execute(() -> {
//			logger.error("Got message "+msg);
//			childChannel.pipeline().fireChannelRead(bytes);
//		});
		childChannel.pipeline().fireChannelRead(bytes);
	}

	public abstract void		initChildChannel(Channel childChannel);

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	protected EventLoopGroup	workersGroup;

	protected Logger		logger = LogManager.getLogger();
}
