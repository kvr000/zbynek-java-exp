package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;


public interface ResponseExchangeBuilder extends Closeable
{
	void addHeader(String name, String value);

	void addCookie(String name, String value);

	void addCookie(String name, String value, String path);

	void addCookie(String name, String value, String path, long expires);

	OutputStream openBodyStream() throws IOException;

	default void close() throws IOException
	{
	}
}
