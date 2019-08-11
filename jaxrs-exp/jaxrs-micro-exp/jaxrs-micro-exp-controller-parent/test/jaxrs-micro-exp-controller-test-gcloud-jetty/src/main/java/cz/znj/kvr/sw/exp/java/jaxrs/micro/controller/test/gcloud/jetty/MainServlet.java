package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.gcloud.jetty;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class MainServlet extends HttpServlet
{
	public MainServlet(RootController rootController)
	{
		this.rootController = rootController;
	}

	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException
	{
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("Hello, Zbynek");
	}

	private final RootController rootController;
}
