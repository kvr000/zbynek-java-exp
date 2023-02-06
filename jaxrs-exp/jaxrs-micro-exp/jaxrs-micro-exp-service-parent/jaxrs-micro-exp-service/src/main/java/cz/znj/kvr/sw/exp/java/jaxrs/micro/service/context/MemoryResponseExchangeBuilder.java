package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context;

import net.dryuf.bigio.iostream.CommittableOutputStream;
import net.dryuf.bigio.iostream.FilterCommittableOutputStream;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class MemoryResponseExchangeBuilder extends AbstractResponseExchangeBuilder
{
	protected MemoryResponseExchangeBuilder(RequestExchange request, int status)
	{
		super(request, status);
	}

	@Override
	public CommittableOutputStream openBodyStream() throws IOException
	{
		if (this.output == null) {
			buffer = new ByteArrayOutputStream();
			output = new FilterCommittableOutputStream(new ByteArrayOutputStream())
			{
				@Override
				public void committable(boolean b)
				{
					committed = b;
				}

				@Override
				public void close() throws IOException
				{
					MemoryResponseExchangeBuilder.this.close();
				}
			};
		}
		return this.output;
	}

	@Override
	public void close() throws IOException
	{
		if (closed) {
			return;
		}
		closed = true;
		if (committed) {
			request.respond(status, getOutputLength());
			try (OutputStream stream = request.getResponseBody()) {
				buffer.writeTo(stream);
			}
		}
		else {
			byte[] error = "Internal Server Error when producing response".getBytes(StandardCharsets.UTF_8);
			request.respond(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), error.length);
			try (OutputStream stream = request.getResponseBody()) {
				stream.write(error);
			}
		}
	}

	protected long getOutputLength()
	{
		return buffer.size();
	}

	private ByteArrayOutputStream buffer;

	private CommittableOutputStream output;

	private boolean closed;

	private boolean committed = true;
}
