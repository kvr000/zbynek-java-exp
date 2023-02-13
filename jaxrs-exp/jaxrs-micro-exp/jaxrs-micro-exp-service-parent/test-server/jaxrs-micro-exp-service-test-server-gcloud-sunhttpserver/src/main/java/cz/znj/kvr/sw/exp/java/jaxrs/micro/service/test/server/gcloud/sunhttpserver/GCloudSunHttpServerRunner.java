package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.gcloud.sunhttpserver;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.net.httpserver.HttpServer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.sunhttpserver.RoutingHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.ServicerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.guice.TestModule;
import lombok.extern.java.Log;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.net.InetSocketAddress;


@Log
public class GCloudSunHttpServerRunner
{
	public static void main(String[] args) throws Exception
	{
		try {
			System.setProperty("com.google.inject.internal.cglib.$experimental_asm7", "true");
			new GCloudSunHttpServerRunner().run();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void run() throws Exception
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
		if (true) return container.getInjector().getInstance(RootServicer.class);
		return new RootServicer(
			container.getInjector().getInstance(Key.get(ServicerRouter.class, Names.named("rootRouter"))),
			container.getInjector().getInstance(MessageBodyReader.class),
			container.getInjector().getInstance(MessageBodyWriter.class)
		);
	}

	private HttpServer server;
}
