package cz.znj.kvr.sw.exp.java.nio.socket;

import lombok.extern.log4j.Log4j2;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;


@Log4j2
public class UdpAsyncSocketTest
{
	@Ignore("AsynchronousSocketChannel does not support UDP")
	@Test(timeOut = 1000L)
	public void testRemoteAddress() throws Exception
	{
		try (AsynchronousSocketChannel listener = AsynchronousSocketChannel.open();
		     DatagramChannel client = DatagramChannel.open()) {
			listener.bind(null);
			ByteBuffer buf = ByteBuffer.allocate(2048);
			listener.read(buf, 0, new CompletionHandler<Integer, Integer>()
			{
				@Override
				public void completed(Integer result, Integer attachment)
				{
					try {
						log.info("Received package: len={} remote={}", result, listener.getRemoteAddress());
						Assert.assertNotNull(listener.getRemoteAddress());
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					buf.flip();
					listener.write(buf);
				}

				@Override
				public void failed(Throwable exc, Integer attachment)
				{
					Assert.fail("Failed to receive message", exc);
				}
			});
			;
			client.send(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), listener.getLocalAddress());
		}
	}
}
