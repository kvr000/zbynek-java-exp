package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DuplexChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.netty.core.NettyEngine;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * HttpProxyFactory tests.
 */
public class NettyHttpProxyFactoryTest
{
	@Test
	public void replaceBuffer_same_replace()
	{
		ByteBuf in = createByteBuf("hello", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceBuffer(in, 1, 4, "xyz".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("hxyzo"));
	}

	@Test
	public void replaceBuffer_longer_replace()
	{
		ByteBuf in = createByteBuf("hello", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceBuffer(in, 1, 4, "longer".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("hlongero"));
	}

	@Test
	public void replaceBuffer_longerWithSpace_replace()
	{
		ByteBuf in = createByteBuf("hello", 10);
		ByteBuf out = NettyHttpProxyFactory.replaceBuffer(in, 1, 4, "longer".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("hlongero"));
	}

	@Test
	public void replaceHeaderValue_existWithExist_update()
	{
		ByteBuf in = createByteBuf("GET / HTTP/1.0\r\nConnection: keep-alive\r\nHost: hello.example.com\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHeaderValue(in, "connection".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertEquals("keep-alive".getBytes(StandardCharsets.UTF_8), old); return "close".getBytes(StandardCharsets.UTF_8); });
		MatcherAssert.assertThat(out, new BufToPosEquals("GET / HTTP/1.0\r\nConnection: close\r\nHost: hello.example.com\r\n\r\n"));
	}

	@Test
	public void replaceHeaderValue_notExistWithExist_add()
	{
		ByteBuf in = createByteBuf("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHeaderValue(in, "connection".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertEquals(null, old); return "close".getBytes(StandardCharsets.UTF_8); });
		MatcherAssert.assertThat(out, new BufToPosEquals("GET / HTTP/1.0\r\nHost: hello.example.com\r\nconnection: close\r\n\r\n"));
	}

	@Test
	public void replaceHeaderValue_matchInBody_untouched()
	{
		ByteBuf in = createByteBuf("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHeaderValue(in, "bodyheader".getBytes(StandardCharsets.UTF_8), (old) -> { Assert.assertEquals(old, null); return null; });
		MatcherAssert.assertThat(out, new BufToPosEquals("GET / HTTP/1.0\r\nHost: hello.example.com\r\n\r\n"));
	}

	@Test
	public void replaceBuffer_shorter_replace()
	{
		ByteBuf in = createByteBuf("hello", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceBuffer(in, 1, 4, "x".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("hxo"));
	}

	@Test
	public void replaceHttpMethodHost_noHost_untouched()
	{
		ByteBuf in = createByteBuf("GET /hello HTTP/1.0\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHttpMethodHost(in, "example.com".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("GET /hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_wrongProto_untouched()
	{
		ByteBuf in = createByteBuf("GET https://example.com/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHttpMethodHost(in, "localhost".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("GET https://example.com/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_withoutPort_updated()
	{
		ByteBuf in = createByteBuf("GET http://example.com/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHttpMethodHost(in, "localhost".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("GET http://localhost/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void replaceHttpMethodHost_withPort_updated()
	{
		ByteBuf in = createByteBuf("GET http://example.com:80/hello HTTP/1.0\r\n\r\n", -1);
		ByteBuf out = NettyHttpProxyFactory.replaceHttpMethodHost(in, "localhost:1234".getBytes(StandardCharsets.UTF_8));
		MatcherAssert.assertThat(out, new BufToPosEquals("GET http://localhost:1234/hello HTTP/1.0\r\n\r\n"));
	}

	@Test
	public void getServerAddress_empty_original()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.build();
		String address = NettyHttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("www.example.com:80", address);
	}

	@Test
	public void getServerAddress_mappedHostPort_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com:80", "localhost:1234"))
			.build();
		String address = NettyHttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("localhost:1234", address);
	}

	@Test
	public void getServerAddress_mappedHost_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com", "localhost"))
			.build();
		String address = NettyHttpProxyFactory.remappedHost(config, "www.example.com", "80");
		Assert.assertEquals("localhost:80", address);
	}

	@Test
	public void getServerAddress_mappedHostWithoutPort_remapped()
	{
		HttpProxyFactory.Config config = HttpProxyFactory.Config.builder()
			.remapHosts(Collections.singletonMap("www.example.com:1234", "localhost"))
			.build();
		String address = NettyHttpProxyFactory.remappedHost(config, "www.example.com", "1234");
		Assert.assertEquals("localhost:1234", address);
	}

	@Test
	public void hostPattern_connect_getConnectHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("CONNECT my-host.example.com HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals(null, m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_connectWithPort_getConnectHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("CONNECT my-host.example.com:443 HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals("443", m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_connectDoubleSpace_getConnectHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("CONNECT  my-host.example.com HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("CONNECT", m.group(1));
		Assert.assertEquals("my-host.example.com", m.group(2));
		Assert.assertEquals(null, m.group(3));
		Assert.assertEquals("HTTP/1.0", m.group(4));
	}

	@Test
	public void hostPattern_fullUrl_getUrlHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("GET http://my-host.example.com/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals(null, m.group(7));
	}

	@Test
	public void hostPattern_fullUrlWithPort_getUrlHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("GET http://my-host.example.com:123/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals("123", m.group(7));
	}

	@Test
	public void hostPattern_fullUrlDoubleSpace_getUrlHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("GET  http://my-host.example.com/path/file HTTP/1.0\nHost: localhost\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("GET", m.group(1));
		Assert.assertEquals("http", m.group(5));
		Assert.assertEquals("my-host.example.com", m.group(6));
		Assert.assertEquals(null, m.group(7));
	}

	@Test
	public void hostPattern_host_getHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("GET /path/file HTTP/1.0\nHost: my-host.example.com\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("my-host.example.com", m.group(8));
		Assert.assertEquals(null, m.group(9));
	}

	@Test
	public void hostPattern_hostWithPort_getHost()
	{
		Matcher m = NettyHttpProxyFactory.HOST_PATTERN.matcher("GET /path/file HTTP/1.0\nHost: my-host.example.com\nconnection:close\n\n");
		Assert.assertTrue(m.find());
		Assert.assertEquals("my-host.example.com", m.group(8));
		Assert.assertEquals(null, m.group(9));
	}

	@Test
	public void requestParsing_incompleteHeaders_respondBadRequest() throws Exception
	{
		ClientFixture f = new ClientFixture();
		when(f.nettyEngine.writeAndClose(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("GET / HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.writeAndClose(eq(f.client), any());
	}

	@Test
	public void requestParsing_connectToUrl_respondBadRequest() throws Exception
	{
		ClientFixture f = new ClientFixture();
		when(f.nettyEngine.writeAndClose(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("CONNECT http://localhost:443/ HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.writeAndClose(eq(f.client), any());
	}

	@Test
	public void requestParsing_missingHost_respondBadRequest() throws Exception
	{
		ClientFixture f = new ClientFixture();
		when(f.nettyEngine.writeAndClose(any(), any()))
			.thenReturn(CompletableFuture.completedFuture(null));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("GET /path HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.writeAndClose(eq(f.client), any());
	}


	@SuppressWarnings("unchecked")
	@Test(timeOut = 10000L)
	public void requestParsing_connectToHostFails_connectAnd503() throws Exception
	{
		ServerFixture f = new ServerFixture();
		when(f.nettyEngine.connect(any(), any(), any()))
			.thenReturn(FutureUtil.exception(new IOException("failed to connect")));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("CONNECT localhost HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.clientPipeline, times(1))
			.remove(NettyHttpProxyFactory.RequestReaderHandler.class);
		verify(f.nettyEngine, times(1))
			.writeAndClose(eq(f.client), any());
	}

	@SuppressWarnings("unchecked")
	@Test(timeOut = 10000L)
	public void requestParsing_connectToHost_connect() throws Exception
	{
		ServerFixture f = new ServerFixture();
		doReturn(succeededChannelFuture()).when(f.client).writeAndFlush(any());
		doReturn(succeededChannelFuture()).when(f.server).writeAndFlush(any());
		when(f.nettyEngine.connect(any(), any(), any()))
			.thenReturn(CompletableFuture.completedFuture(f.server));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("CONNECT localhost HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.connect(any(), eq(InetSocketAddress.createUnresolved("localhost", 443)), any());
		verify(f.client, times(1))
			.writeAndFlush(any());
		verify(f.server, times(1))
			.writeAndFlush(Unpooled.EMPTY_BUFFER);
		verify(f.clientPipeline, times(1))
			.remove(NettyHttpProxyFactory.RequestReaderHandler.class);
		verify(f.nettyEngine, times(1)).forwardDuplex(f.client, f.server);
	}

	@Test(timeOut = 10000L)
	public void requestParsing_connectToHostPort_connect() throws Exception
	{
		ServerFixture f = new ServerFixture();
		doReturn(succeededChannelFuture()).when(f.client).writeAndFlush(any());
		doReturn(succeededChannelFuture()).when(f.server).writeAndFlush(any());
		when(f.nettyEngine.connect(any(), any(), any()))
			.thenReturn(CompletableFuture.completedFuture(f.server));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer("CONNECT localhost:1234 HTTP/1.0\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.connect(any(), eq(InetSocketAddress.createUnresolved("localhost", 1234)), any());
		verify(f.client, times(1))
			.writeAndFlush(any());
		verify(f.server, times(1))
			.writeAndFlush(Unpooled.EMPTY_BUFFER);
		verify(f.clientPipeline, times(1))
			.remove(NettyHttpProxyFactory.RequestReaderHandler.class);
		verify(f.nettyEngine, times(1)).forwardDuplex(f.client, f.server);
	}

	@Test(timeOut = 10000L)
	public void requestParsing_methodToHost_forward() throws Exception
	{
		ServerFixture f = new ServerFixture();
		doReturn(succeededChannelFuture()).when(f.client).writeAndFlush(any());
		doReturn(succeededChannelFuture()).when(f.server).writeAndFlush(any());
		when(f.nettyEngine.connect(any(), any(), any()))
			.thenReturn(CompletableFuture.completedFuture(f.server));
		f.handler.channelRead(f.clientCtx, Unpooled.wrappedBuffer(("POST /path HTTP/1.0\nhost: localhost\nconnection: keep-alive\n\nsome body").getBytes(StandardCharsets.UTF_8)));
		verify(f.nettyEngine, times(1))
			.connect(any(), eq(InetSocketAddress.createUnresolved("localhost", 80)), any());
		verify(f.client, times(1))
			.writeAndFlush(Unpooled.EMPTY_BUFFER);
		verify(f.server, times(1))
			.write(Unpooled.wrappedBuffer("POST /path HTTP/1.0\nhost: localhost:80\nconnection: close\n\n".getBytes(StandardCharsets.UTF_8)));
		verify(f.server, times(1))
			.writeAndFlush(Unpooled.wrappedBuffer("some body".getBytes(StandardCharsets.UTF_8)));
		verify(f.clientPipeline, times(1))
			.remove(NettyHttpProxyFactory.RequestReaderHandler.class);
		verify(f.nettyEngine, times(1)).forwardDuplex(f.client, f.server);
	}

	public static class ClientFixture
	{
		HttpProxyFactory.Config proxyConfig;
		NettyEngine nettyEngine;
		ChannelPipeline clientPipeline;
		ChannelConfig clientConfig;
		DuplexChannel client;
		NettyHttpProxyFactory proxy;
		NettyHttpProxyFactory.RequestReaderHandler handler;
		ChannelHandlerContext clientCtx;
		CompletableFuture<Void> closeFuture;

		public ClientFixture()
		{
			proxyConfig = HttpProxyFactory.Config.builder().build();
			nettyEngine = mock(NettyEngine.class);
			clientConfig = mock(ChannelConfig.class);
			clientPipeline = mock(ChannelPipeline.class);
			client = mock(DuplexChannel.class);
			proxy = new NettyHttpProxyFactory(nettyEngine);
			closeFuture = mock(CompletableFuture.class);
			handler = proxy.new RequestReaderHandler(proxyConfig, closeFuture);
			clientCtx = mock(ChannelHandlerContext.class);
			when(clientCtx.channel())
				.thenReturn(client);
			when(clientCtx.pipeline())
				.thenReturn(clientPipeline);
			when(client.pipeline())
				.thenReturn(clientPipeline);
			when(client.config())
				.thenReturn(clientConfig);

			when(nettyEngine.resolve(any()))
				.thenAnswer((answer) ->
					CompletableFuture.completedFuture(answer.getArgument(0))
				);
		}
	}

	public static class ServerFixture extends ClientFixture
	{
		ChannelPipeline serverPipeline;
		ChannelConfig serverConfig;
		DuplexChannel server;

		public ServerFixture()
		{
			serverPipeline = mock(ChannelPipeline.class);
			serverConfig = mock(ChannelConfig.class);
			server = mock(DuplexChannel.class);
			when(server.pipeline())
				.thenReturn(serverPipeline);
			when(server.config())
				.thenReturn(serverConfig);
		}
	}

	private static ByteBuf createByteBuf(String value, int size)
	{
		ByteBuf b = Unpooled.buffer(size < 0 ? value.length() : size);
		b.writeBytes(value.getBytes(StandardCharsets.UTF_8));
		return b;
	}

	@SuppressWarnings("unchecked")
	@SneakyThrows
	private <V> ChannelFuture succeededChannelFuture()
	{
		ChannelFuture future = mock(ChannelFuture.class);
		when(future.get())
			.thenReturn(null);
		when(future.addListener(any()))
			.thenAnswer((answer) -> {
				((GenericFutureListener<Future<Void>>)answer.getArgument(0)).operationComplete(future);
				return null;
			});
		return future;
	}

	@RequiredArgsConstructor
	static class BufToPosEquals extends BaseMatcher<ByteBuf>
	{
		private final byte[] expected;

		public BufToPosEquals(String expected)
		{
			this.expected = expected.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public boolean matches(Object actual0)
		{
			ByteBuf actual = (ByteBuf) actual0;
			if (actual.writerIndex()-actual.readerIndex() != expected.length)
				return false;
			for (int i = actual.readerIndex(); i < actual.writerIndex(); ++i) {
				if (actual.getByte(i) != expected[i-actual.readerIndex()])
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
