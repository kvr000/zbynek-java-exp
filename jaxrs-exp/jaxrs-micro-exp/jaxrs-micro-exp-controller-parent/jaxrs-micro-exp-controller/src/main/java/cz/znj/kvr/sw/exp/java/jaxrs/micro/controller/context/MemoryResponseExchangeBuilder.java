package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class MemoryResponseExchangeBuilder extends AbstractResponseExchangeBuilder
{
	protected MemoryResponseExchangeBuilder(RequestExchange request, int status)
	{
		super(request, status);
	}

	@Override
	public OutputStream openBodyStream() throws IOException
	{
		return output = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException
			{
				MemoryResponseExchangeBuilder.this.close();
			}
		};
	}

	@Override
	public void close() throws IOException
	{
		if (closed) {
			return;
		}
		closed = true;
		request.respond(status, getOutputLength());
		try (OutputStream stream = request.getResponseBody()) {
			output.writeTo(stream);
		}
	}

	protected long getOutputLength()
	{
		return output.size();
	}

	private ByteArrayOutputStream output;

	private boolean closed;
}
