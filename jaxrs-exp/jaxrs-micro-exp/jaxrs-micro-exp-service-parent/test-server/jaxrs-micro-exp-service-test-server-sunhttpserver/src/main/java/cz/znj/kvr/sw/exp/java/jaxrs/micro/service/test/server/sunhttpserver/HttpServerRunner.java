package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.sunhttpserver;

import com.google.inject.Guice;
import com.sun.net.httpserver.HttpServer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.sunhttpserver.RoutingHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.guice.TestModule;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;


@Log4j2
public class HttpServerRunner
{
	public static void main(String[] args) throws Exception
	{
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		try {
			System.setProperty("com.google.inject.internal.cglib.$experimental_asm7", "true");
			new HttpServerRunner().run();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	protected void run() throws Exception
	{
		long base = System.currentTimeMillis();
		ContainerContext container = Guice.createInjector(new TestModule()).getInstance(ContainerContext.class);

		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new RoutingHandler(container, getRootController(container)));
		server.setExecutor(null);
		server.start();
		long started = System.currentTimeMillis();
		log.info(() -> "Started in "+(started-base)+" ms");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				server.stop(0);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	public RootServicer getRootController(ContainerContext container)
	{
		return container.getInjector().getInstance(RootServicer.class);
	}

	private HttpServer server;
}
