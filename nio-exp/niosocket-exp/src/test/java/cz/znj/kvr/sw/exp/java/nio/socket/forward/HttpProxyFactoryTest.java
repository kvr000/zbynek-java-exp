package cz.znj.kvr.sw.exp.java.nio.socket.forward;

import lombok.RequiredArgsConstructor;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
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
	public void replaceBuffer_same_replace()
	{
		ByteBuffer in = createByteBuffer("hello", -1);
		ByteBuffer out = HttpProxyFactory.replaceBuffer(in, 1, 4, "xyz".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("hxyzo"));
	}

	@Test
	public void replaceBuffer_longer_replace()
	{
		ByteBuffer in = createByteBuffer("hello", -1);
		ByteBuffer out = HttpProxyFactory.replaceBuffer(in, 1, 4, "longer".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("hlongero"));
	}

	@Test
	public void replaceBuffer_longerWithSpace_replace()
	{
		ByteBuffer in = createByteBuffer("hello", 10);
		ByteBuffer out = HttpProxyFactory.replaceBuffer(in, 1, 4, "longer".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("hlongero"));
	}

	@Test
	public void replaceHeaderValue_existWithExist_update()
	{
		ByteBuffer in = createByteBuffer("GET / HTTP/1.0\r\nConnection: keep-alive\r\nHost: hello.example.com\r\n\r\nsome body data", -1);
		ByteBuffer out = HttpProxyFactory.replaceHeaderValue(in, 0, 64, "connection".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertArrayEquals("keep-alive".getBytes(StandardCharsets.UTF_8), old); return "close".getBytes(StandardCharsets.UTF_8); });
		Assert.assertThat(out, new BufferToPosEquals("GET / HTTP/1.0\r\nConnection: close\r\nHost: hello.example.com\r\n\r\nsome body data"));
	}

	@Test
	public void replaceHeaderValue_notExistWithExist_add()
	{
		ByteBuffer in = createByteBuffer("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\nsome body data", -1);
		ByteBuffer out = HttpProxyFactory.replaceHeaderValue(in, 0, 41, "connection".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertArrayEquals(null, old); return "close".getBytes(StandardCharsets.UTF_8); });
		Assert.assertThat(out, new BufferToPosEquals("GET / HTTP/1.0\r\nHost: hello.example.com\r\nconnection: close\r\n\r\nsome body data"));
	}

	@Test
	public void replaceHeaderValue_matchInBody_untouched()
	{
		ByteBuffer in = createByteBuffer("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\nBodyHeader: xyz\r\n\r\nsome body data", -1);
		ByteBuffer out = HttpProxyFactory.replaceHeaderValue(in, 0, 41, "bodyheader".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertArrayEquals(null, old); return null; });
		Assert.assertThat(out, new BufferToPosEquals("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\nBodyHeader: xyz\r\n\r\nsome body data"));
	}

	@Test
	public void replaceBuffer_shorter_replace()
	{
		ByteBuffer in = createByteBuffer("hello", -1);
		ByteBuffer out = HttpProxyFactory.replaceBuffer(in, 1, 4, "x".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("hxo"));
	}

	@Test
	public void replaceHttpMethodHost_noHost_untouched()
	{
		ByteBuffer in = createByteBuffer("GET /hello HTTP/1.0\r\n\r\n", -1);
		ByteBuffer out = HttpProxyFactory.replaceHttpMethodHost(in, in.position(), "example.com".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("GET /hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_wrongProto_untouched()
	{
		ByteBuffer in = createByteBuffer("GET https://example.com/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuffer out = HttpProxyFactory.replaceHttpMethodHost(in, in.position(), "localhost".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("GET https://example.com/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_withoutPort_updated()
	{
		ByteBuffer in = createByteBuffer("GET http://example.com/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuffer out = HttpProxyFactory.replaceHttpMethodHost(in, in.position(), "localhost".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("GET http://localhost/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_withPort_updated()
	{
		ByteBuffer in = createByteBuffer("GET http://example.com:80/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuffer out = HttpProxyFactory.replaceHttpMethodHost(in, in.position(), "localhost:1234".getBytes(StandardCharsets.UTF_8));
		Assert.assertThat(out, new BufferToPosEquals("GET http://localhost:1234/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void getServerAddress_empty_original()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.build();
		String address = HttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("www.example.com:80", address);
	}

	@Test
	public void getServerAddress_mappedHostPort_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com:80", "localhost:1234"))
			.build();
		String address = HttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("localhost:1234", address);
	}

	@Test
	public void getServerAddress_mappedHost_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com", "localhost"))
			.build();
		String address = HttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("localhost:80", address);
	}

	@Test
	public void getServerAddress_mappedHostWithoutPort_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com:1234", "localhost"))
			.build();
		String address = HttpProxyFactory.remappedHost(config, "www.example.com", "1234");
		Assert.assertEquals("localhost:1234", address);
	}

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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build());
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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build());
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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build());
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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build())
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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build())
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
		f.proxy.runServer(f.client, HttpProxyFactory.Config.builder().build())
			.get();
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.client), eq(ByteBuffer.wrap(new byte[0])));
		verify(f.portForwarder, times(1))
			.writeFully(eq(f.server), eq(ByteBuffer.wrap("POST /path HTTP/1.0\nhost: localhost:80\nconnection: close\n\nsome body".getBytes(StandardCharsets.UTF_8))));
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

	private static ByteBuffer createByteBuffer(String value, int size)
	{
		ByteBuffer b = ByteBuffer.allocate(size < 0 ? value.length() : size);
		b.put(value.getBytes(StandardCharsets.UTF_8));
		return b;
	}

	@RequiredArgsConstructor
	static class BufferToPosEquals extends BaseMatcher<ByteBuffer>
	{
		private final byte[] expected;

		public BufferToPosEquals(String expected)
		{
			this.expected = expected.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public boolean matches(Object actual0)
		{
			ByteBuffer actual = (ByteBuffer) actual0;
			if (actual.position() != expected.length)
				return false;
			for (int i = 0; i < actual.position(); ++i) {
				if (actual.get(i) != expected[i])
					return false;
			}
			return true;
		}

		@Override
		public void describeMismatch(Object item, Description description) {
			ByteBuffer actual = (ByteBuffer) item;
			description.appendText("was ").appendValue(actual).appendText(" but expected ").appendValue(expected).appendText(":\nactual != expected:\n");
			byte[] dump = new byte[actual.position()];
			for (int i = 0; i < actual.position(); ++i) {
				dump[i] = actual.get(i);
			}
			description.appendText("\"").appendText(new String(dump, StandardCharsets.UTF_8)).appendText("\" != \"")
				.appendText(new String(expected, StandardCharsets.UTF_8)).appendText("\"");
		}

		@Override
		public void describeTo(Description description)
		{
			description.appendText(expected.toString());
		}
	}
}
