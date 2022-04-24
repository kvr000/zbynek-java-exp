package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;

import java.net.SocketAddress;


public interface ChannelProvider
{
	default EventLoopGroup createBossEventLoopGroup()
	{
		return createEventLoopGroup(1);
	}

	default EventLoopGroup createWorkerEventLoopGroup()
	{
		return createEventLoopGroup(Runtime.getRuntime().availableProcessors());
	}

	EventLoopGroup createEventLoopGroup(int threads);

	default SocketAddress convertAddress(SocketAddress original)
	{
		return original;
	}

	ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address);

	ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address);

	ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address);
}
