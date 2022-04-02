package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward.test.reproduce;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import lombok.Getter;
import org.testng.annotations.Test;

public class Fail
{
	@Getter
	private EventLoopGroup bossGroup = new EpollEventLoopGroup();
	@Getter
	private EventLoopGroup workerGroup = new EpollEventLoopGroup();

	@Test
	public void testMultiFamilyBind()
	{

	}
}
