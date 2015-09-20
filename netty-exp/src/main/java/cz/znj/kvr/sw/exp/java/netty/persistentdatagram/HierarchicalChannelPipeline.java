package cz.znj.kvr.sw.exp.java.netty.persistentdatagram;


import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannelHandlerContext;
import io.netty.channel.AbstractChannelPipeline;


/**
 * Created by rat on 2015-09-20.
 */
public class HierarchicalChannelPipeline extends AbstractChannelPipeline
{
	public HierarchicalChannelPipeline(AbstractChannel channel)
	{
		super(channel);
	}

	@Override
	protected AbstractChannelHandlerContext createHeadContext()
	{
		return null; //new ForwardingHeadContext();
	}
}
