package cz.znj.kvr.sw.exp.java.netty.persistentdatagram;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
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
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


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
		Channel childChannel;
		byte[] bytes;
		try {
			DatagramPacket packet = (DatagramPacket) msg;
			childChannel = getChildChannel(ctx.channel(), packet.sender());
			workersGroup.register(childChannel);
			ByteBuf content = Unpooled.copiedBuffer(packet.content());
			bytes = new byte[content.readableBytes()];
			content.readBytes(bytes);
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
		childChannel.pipeline().fireChannelRead(bytes);
	}

	public Channel			getChildChannel(Channel parentChannel, InetSocketAddress peerAddress)
	{
		return childChannels.computeIfAbsent(peerAddress, (InetSocketAddress peerAddress2) -> {
			PersistentDatagramChannel channel = new PersistentDatagramChannel(parentChannel, peerAddress);
			initChildChannel(channel);
			return channel;
		});
	}

	public abstract Channel		initChildChannel(Channel childChannel);

	@Override
	public void			exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	protected EventLoopGroup	workersGroup;

	protected Logger		logger = LogManager.getLogger();

	protected Map<InetSocketAddress, Channel> childChannels =
		CacheBuilder.<InetSocketAddress, Channel>newBuilder()
			.expireAfterAccess(60000, TimeUnit.MILLISECONDS)
			.removalListener((RemovalNotification<InetSocketAddress, Channel> notification) -> {
				notification.getValue().pipeline().fireChannelInactive();
				logger.error("Removed client from "+notification.getKey());
			})
			.build()
			.asMap();
}


