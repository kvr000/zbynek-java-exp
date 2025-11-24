package com.github.kvr000.exp.java.http.benchmark.support;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;


public class TheServer implements Closeable
{
	HttpServer server;

	@SneakyThrows
	public TheServer()
	{
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/big_16GB", exchange -> {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			byte[] body = new byte[16*1024*1024];
			exchange.sendResponseHeaders(200, 0);
			try (OutputStream os = exchange.getResponseBody()) {
				for (int i = 0; i < 16*64; ++i) {
					os.write(body);
				}
			}
		});
		server.start();
	}

	public InetSocketAddress address()
	{
		return server.getAddress();
	}

	public String hostname()
	{
		return server.getAddress().getHostName();
	}

	public int port()
	{
		return server.getAddress().getPort();
	}

	public String url()
	{
		InetSocketAddress address = address();
		return "http://" + address.getHostName() + ":" + address.getPort() + "/";
	}

	@Override
	public void close()
	{
		server.stop(0);
	}
}
