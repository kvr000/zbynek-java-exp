package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.provider;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.NettyEngine;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;


public class NioChannelProvider implements ChannelProvider
{
	@Override
	public EventLoopGroup createBossEventLoopGroup()
	{
		return new NioEventLoopGroup(1);
	}

	@Override
	public EventLoopGroup createWorkerEventLoopGroup()
	{
		return new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public EventLoopGroup createEventLoopGroup(int threads)
	{
		return new NioEventLoopGroup(threads);
	}

	public SocketAddress convertAddress(SocketAddress original)
	{
		if (original instanceof DomainSocketAddress address) {
			return UnixDomainSocketAddress.of(address.path());
		}
		else {
			return original;
		}
	}

	@Override
	public ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return () -> new NioServerSocketChannel(SelectorProvider.provider(),
				NettyEngine.getNettyProtocolByAddress(((InetSocketAddress)address).getAddress()));
		}
		else if (address instanceof UnixDomainSocketAddress || address instanceof DomainSocketAddress) {
			return () -> new NioServerSocketChannel(new DelegatedSelectorProvider(SelectorProvider.provider())
			{
				@Override
				public ServerSocketChannel openServerSocketChannel() throws IOException
				{
					ProtocolFamily protocol = StandardProtocolFamily.UNIX;
					return super.openServerSocketChannel(protocol);
				}
			});
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return () -> new NioSocketChannel(new DelegatedSelectorProvider(SelectorProvider.provider())
			{
				@Override
				public SocketChannel openSocketChannel() throws IOException
				{
					ProtocolFamily protocol =
						NettyEngine.getProtocolByAddress(((InetSocketAddress)address).getAddress());
					return super.openSocketChannel(protocol);
				}
			});
		}
		else if (address instanceof UnixDomainSocketAddress || address instanceof DomainSocketAddress) {
			return () -> new NioSocketChannel(new DelegatedSelectorProvider(SelectorProvider.provider())
			{
				@Override
				public ServerSocketChannel openServerSocketChannel() throws IOException
				{
					ProtocolFamily protocol = StandardProtocolFamily.UNIX;
					return super.openServerSocketChannel(protocol);
				}
			});
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return () -> new NioDatagramChannel(new DelegatedSelectorProvider(SelectorProvider.provider())
			{
				@Override
				public java.nio.channels.DatagramChannel openDatagramChannel() throws IOException
				{
					ProtocolFamily protocol =
						NettyEngine.getProtocolByAddress(((InetSocketAddress)address).getAddress());
					return super.openDatagramChannel(protocol);
				}
			});
		}
		else if (address instanceof UnixDomainSocketAddress || address instanceof DomainSocketAddress) {
			return () -> new NioDatagramChannel(new DelegatedSelectorProvider(SelectorProvider.provider())
			{
				@Override
				public java.nio.channels.DatagramChannel openDatagramChannel() throws IOException
				{
					ProtocolFamily protocol = StandardProtocolFamily.UNIX;
					return super.openDatagramChannel(protocol);
				}
			});
		}
		else {
			return NioDatagramChannel::new;
		}
	}

	@RequiredArgsConstructor
	public static class DelegatedSelectorProvider extends SelectorProvider
	{
		@Delegate
		private final SelectorProvider delegate;
	}
}
