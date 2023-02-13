package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.gcloud.jetty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.jeeservlet.RoutingServlet;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.ServicerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.guice.TestModule;
import lombok.extern.java.Log;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;


import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;


@Log
public class GCloudJava11JettyRunner
{
	public static void main(String[] args) throws Exception
	{
		try {
			System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StrErrLog");
			System.setProperty("org.eclipse.jetty.LEVEL", "INFO");
			new GCloudJava11JettyRunner().run2(args);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void run1(String[] args) throws Exception
	{
		long base = System.currentTimeMillis();
		if (args.length != 1) {
			System.err.println("Usage: need a relative path to the war file to execute");
			System.exit(1);
		}

		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
		Server server = new Server(port);

		// The WebAppContext is the interface to provide configuration for a web
		// application. In this example, the context path is being set to "/" so
		// it is suitable for serving root context requests.
		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath("/");
		webapp.setWar(args[0]);
		ClassList classlist = ClassList.setServerDefault(server);

		// Enable Annotation Scanning.
		classlist.addBefore(
				"org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");

		// Set the the WebAppContext as the ContextHandler for the server.
		server.setHandler(webapp);

		// Start the server! By using the server.join() the server thread will
		// join with the current thread. See
		// "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html#join()"
		// for more details.
		server.start();
		long started = System.currentTimeMillis();
		log.info(() -> "Started in "+(started-base)+" ms");
		server.join();

	}

	public void run2(String[] args) throws Exception
	{
		long base = System.currentTimeMillis();
		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
		ContainerContext container = Guice.createInjector(new TestModule()).getInstance(ContainerContext.class);
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
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
		long started = System.currentTimeMillis();
		log.info(() -> "Started in "+(started-base)+" ms");
		server.join();
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
