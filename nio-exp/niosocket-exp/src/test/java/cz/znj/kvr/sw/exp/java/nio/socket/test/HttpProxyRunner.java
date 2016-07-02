package cz.znj.kvr.sw.exp.java.nio.socket.test;

import cz.znj.kvr.sw.exp.java.nio.socket.forward.HttpProxyFactory;
import cz.znj.kvr.sw.exp.java.nio.socket.forward.PortForwarder;

import java.net.InetSocketAddress;


/**
 * Proxy runner
 */
public class HttpProxyRunner
{
	public static void main(String[] args) throws Exception
	{
		new HttpProxyFactory(new PortForwarder())
			.runProxy(new InetSocketAddress("localhost", 4444))
			.get();
	}
}
