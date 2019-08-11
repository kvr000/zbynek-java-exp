package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader;


import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public interface JaxRsCaptureReader extends Closeable
{
	ControllerMeta next() throws IOException;

	default List<ControllerMeta> readAll() throws IOException
	{
		ArrayList<ControllerMeta> out = new ArrayList<>();
		ControllerMeta controller;
		while ((controller = next()) != null) {
			out.add(controller);
		}
		return out;
	}

	default void close() throws IOException
	{
	}
}
