package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * HttpProxyFactory tests.
 */
public class HttpProxyFactoryTest
{
	@Test
	public void hostPattern_connect_getConnectHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("CONNECT my-host.example.com HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals(null, m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_connectWithPort_getConnectHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("CONNECT my-host.example.com:443 HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals("443", m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_connectDoubleSpace_getConnectHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("CONNECT  my-host.example.com HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals(null, m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_fullUrl_getUrlHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("GET http://my-host.example.com/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals(null, m.group(7));
	}

	@Test
	public void hostPattern_fullUrlWithPort_getUrlHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("GET http://my-host.example.com:123/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals("123", m.group(7));
	}

	@Test
	public void hostPattern_fullUrlDoubleSpace_getUrlHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("GET  http://my-host.example.com/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals(null, m.group(7));
	}

	@Test
	public void hostPattern_host_getHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("GET /path/file HTTP/1.0\nHost: my-host.example.com\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("my-host.example.com", m.group(8));
		Assert.assertEquals(null, m.group(9));
	}

	@Test
	public void hostPattern_hostWithPort_getHost()
	{
		Matcher m = HttpProxyFactory.HOST_PATTERN.matcher("GET /path/file HTTP/1.0\nHost: my-host.example.com\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("my-host.example.com", m.group(8));
		Assert.assertEquals(null, m.group(9));
	}

	@Test
	public void requestParsing_incompleteHeaders_respondBadRequest()
	{
		ClientFixture f = new ClientFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"GET / HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		when(f.portForwarder.writeAndShutdown(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client);
		verify(f.portForwarder, times(1))
			.writeAndShutdown(eq(f.client), any());
	}

	@Test
	public void requestParsing_connectToUrl_respondBadRequest()
	{
		ClientFixture f = new ClientFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"CONNECT http://localhost:443/ HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		when(f.portForwarder.writeAndShutdown(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client);
		verify(f.portForwarder, times(1))
			.writeAndShutdown(eq(f.client), any());
	}

	@Test
	public void requestParsing_missingHost_respondBadRequest()
	{
		ClientFixture f = new ClientFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"GET /path HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		when(f.portForwarder.writeAndShutdown(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client);
		verify(f.portForwarder, times(1))
			.writeAndShutdown(eq(f.client), any());
	}

	@Test(timeout = 1000L)
	public void requestParsing_connectToHost_connect() throws ExecutionException, InterruptedException
	{
		ServerFixture f = new ServerFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"CONNECT localhost HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		Mockito.doAnswer(answer -> {
			((Consumer<AsynchronousSocketChannel>) answer.getArgument(1)).accept(f.server);
			((CompletionHandler<Void, Integer>) answer.getArgument(2)).completed(null, 0);
			return null;
		})
			.when(f.portForwarder).connect(eq(new InetSocketAddress("localhost", 443)), any(), any());
		when(f.portForwarder.writeFully(eq(f.client), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.writeFully(eq(f.server), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.runBothForward(f.client, f.server))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client)
			.get();
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.client), any());
		verify(f.portForwarder, times(1))
			.runBothForward(f.client, f.server);
	}

	@Test(timeout = 1000L)
	public void requestParsing_connectToHostPort_connect() throws ExecutionException, InterruptedException
	{
		ServerFixture f = new ServerFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"CONNECT localhost:123 HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		Mockito.doAnswer(answer -> {
				((Consumer<AsynchronousSocketChannel>) answer.getArgument(1)).accept(f.server);
				((CompletionHandler<Void, Integer>) answer.getArgument(2)).completed(null, 0);
				return null;
			})
			.when(f.portForwarder).connect(eq(new InetSocketAddress("localhost", 123)), any(), any());
		when(f.portForwarder.writeFully(eq(f.client), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.writeFully(eq(f.server), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.runBothForward(f.client, f.server))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client)
			.get();
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.client), any());
		verify(f.portForwarder, times(1))
			.runBothForward(f.client, f.server);
	}

	@Test(timeout = 1000L)
	public void requestParsing_methodToHost_forward() throws ExecutionException, InterruptedException
	{
		ServerFixture f = new ServerFixture();
		Mockito.doAnswer(answer -> {
				((ByteBuffer) answer.getArgument(0)).put(
					"POST /path HTTP/1.0\nhost: localhost\nconnection: keep-alive\n\nsome body".getBytes(StandardCharsets.UTF_8)
				);
				((CompletionHandler<Integer, Integer>) answer.getArgument(4)).completed(1, 0);
				return null;
			})
			.when(f.client).read(any(ByteBuffer.class), anyLong(), any(), any(), any(CompletionHandler.class));
		Mockito.doAnswer(answer -> {
				((Consumer<AsynchronousSocketChannel>) answer.getArgument(1)).accept(f.server);
				((CompletionHandler<Void, Integer>) answer.getArgument(2)).completed(null, 0);
				return null;
			})
			.when(f.portForwarder).connect(eq(new InetSocketAddress("localhost", 80)), any(), any());
		when(f.portForwarder.writeFully(eq(f.client), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.writeFully(eq(f.server), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		when(f.portForwarder.runBothForward(f.client, f.server))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.proxy.runServer(f.client)
			.get();
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.client), eq(ByteBuffer.wrap(new byte[0])));
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.server), eq(ByteBuffer.wrap("POST /path HTTP/1.0\nhost: localhost\nconnection: close\r\n\nsome body".getBytes(StandardCharsets.UTF_8))));
		verify(f.portForwarder, times(1))
			.runBothForward(f.client, f.server);
	}

	public static class ClientFixture
	{
		PortForwarder portForwarder;
		AsynchronousSocketChannel client;
		HttpProxyFactory proxy;

		public ClientFixture()
		{
			portForwarder = mock(PortForwarder.class);
			client = mock(AsynchronousSocketChannel.class);
			proxy = new HttpProxyFactory(portForwarder);
		}
	}

	public static class ServerFixture extends ClientFixture
	{
		AsynchronousSocketChannel server;

		public ServerFixture()
		{
			server = mock(AsynchronousSocketChannel.class);
		}
	}
}
