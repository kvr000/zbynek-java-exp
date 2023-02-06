package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.jetty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.jeeservlet.RoutingServlet;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.ServicerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.guice.TestModule;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;


public class JettyRunner
{
	public static void main(String[] args) throws Exception
	{
		try {
			System.setProperty("com.google.inject.internal.cglib.$experimental_asm7", "true");
			new JettyRunner().run();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void run() throws Exception
	{
		ContainerContext container = Guice.createInjector(new TestModule()).getInstance(ContainerContext.class);
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8090);
		server.setConnectors(new Connector[] { connector });
		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(new ServletHolder(new RoutingServlet(container, getRootController(container))), "/");
		server.setHandler(servletHandler);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				server.stop();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
		server.start();
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

	private Server server;
}
