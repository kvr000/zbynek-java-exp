package cz.znj.kvr.sw.exp.java.netty;

import java.net.InetSocketAddress;

/**
 * Created by rat on 2015-09-20.
 */
public class DatagramMessage<T>
{
	public				DatagramMessage(T content, InetSocketAddress peerAddress)
	{
		this.content = content;
		this.peerAddress = peerAddress;
	}

	public T			getContent()
	{
		return content;
	}

	public InetSocketAddress	getPeerAddress()
	{
		return peerAddress;
	}

	protected T			content;

	protected InetSocketAddress	peerAddress;
}
