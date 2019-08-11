package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.gcloud.sunhttpserver;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.net.httpserver.HttpServer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.sun.httpserver.RoutingHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.ControllerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.guice.TestModule;
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

	public RootController getRootController(ContainerContext container)
	{
		if (true) return container.getBean(RootController.class);
		return new RootController(
		container.getBean(Injector.class).getInstance(Key.get(ControllerRouter.class, Names.named("rootRouter"))),
		container.getBean(MessageBodyReader.class),
				container.getBean(MessageBodyWriter.class));
	}

	private HttpServer server;
}
