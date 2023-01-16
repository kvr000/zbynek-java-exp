package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.DomainSocketAddress;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.util.concurrent.TimeUnit;


public class KqueueChannelProvider implements ChannelProvider
{
	static {
		KQueueEventLoopGroup events = new KQueueEventLoopGroup();
		events.shutdownGracefully(0, 0, TimeUnit.SECONDS);
	}

	@Override
	public EventLoopGroup createEventLoopGroup(int threads)
	{
		return new KQueueEventLoopGroup(threads);
	}

	@Override
	public SocketAddress convertAddress(SocketAddress original)
	{
		if (original instanceof UnixDomainSocketAddress address) {
			return new DomainSocketAddress(address.getPath().toString());
		}
		else {
			return original;
		}
	}

	@Override
	public ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return KQueueServerSocketChannel::new;
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return KQueueServerDomainSocketChannel::new;
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return () -> new KQueueSocketChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress)address).getAddress()));
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return KQueueDomainSocketChannel::new;
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
	{
		return KQueueDatagramChannel::new;
	}
}
