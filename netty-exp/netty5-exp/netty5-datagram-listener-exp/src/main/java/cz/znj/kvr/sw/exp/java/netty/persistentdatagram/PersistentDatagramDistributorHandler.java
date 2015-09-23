package cz.znj.kvr.sw.exp.java.netty.persistentdatagram;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
@ChannelHandler.Sharable
public abstract class PersistentDatagramDistributorHandler extends ChannelHandlerAdapter
{
	public PersistentDatagramDistributorHandler(EventLoopGroup workersGroup)
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
		workersGroup.execute(() -> childChannel.pipeline().fireChannelRead(bytes));
	}

	public Channel			getChildChannel(Channel parentChannel, InetSocketAddress peerAddress)
	{
		return childChannels.computeIfAbsent(peerAddress, (InetSocketAddress peerAddress2) -> {
			Channel channel = new EmbeddedChannel() {
				@Override
				public SocketAddress remoteAddress() {
					return peerAddress;
				}
			};
			channel.pipeline().removeLast();
			initChildChannel(channel);
			channel.pipeline().addFirst(new ChannelHandlerAdapter() {
				@Override
				public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
					parentChannel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer((byte[]) msg), peerAddress),
						new DefaultChannelPromise(parentChannel)
							.addListener(new GenericFutureListener<ChannelPromise>()
							{
								@Override
								public void operationComplete(ChannelPromise future) throws Exception
								{
									promise.setSuccess();
								}
							}));
				}
			});
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
