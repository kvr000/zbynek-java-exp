package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.echo.test;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.echo.EchoEndTest;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.test.ClientServerTester;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Simple Echo server and client running in high parallelism.
 */
@Log4j2
public class EchoTestRunner
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new EchoTestRunner().run(args));
	}

	public int run(String[] args) throws Exception
	{
		return execute();
	}

	public int execute() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			SocketAddress serverAddress = EchoEndTest.runEchoServer(tester, InetSocketAddress.createUnresolved("localhost", 40100));
			log.info("EchoServer listening on: {}", serverAddress);
			Thread.sleep(Long.MAX_VALUE);
		}
		return 0;
	}
}
