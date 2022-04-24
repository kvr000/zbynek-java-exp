package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class NettyServer implements Server
{
	private final Channel channel;

	@Override
	public CompletableFuture<Void> closedFuture()
	{
		return NettyFutures.toCompletable(channel.closeFuture());
	}

	public SocketAddress listenAddress()
	{
		return channel.localAddress();
	}

	@Override
	public CompletableFuture<Void> cancel()
	{
		return NettyFutures.toCompletable(channel.close());
	}

	@Override
	public void close()
	{
		channel.close().syncUninterruptibly();
	}
}
