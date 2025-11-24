package com.github.kvr000.exp.java.http.benchmark;

import com.github.kvr000.exp.java.http.benchmark.support.TheServer;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;


@Log4j2
@State(Scope.Benchmark)
@Warmup(iterations = BenchmarkSettings.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkSettings.MEASUREMENT_ITERATIONS, batchSize = BenchmarkSettings.BATCH_SIZE, time = BenchmarkSettings.TIMEOUT_SEC)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = "-Xmx8G")
public class ClientBenchmark
{
	@State(value = Scope.Benchmark)
	public static class UrlConnectionState
	{
		TheServer server = new TheServer();
		URL url = Try.of(() -> new URL(server.url() + "big_16GB")).get();

		@TearDown
		public void close() throws Exception
		{
			server.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class ApacheHttpClientState
	{
		TheServer server = new TheServer();
		URL url = Try.of(() -> new URL(server.url() + "big_16GB")).get();

		CloseableHttpClient httpClient = HttpClients.createDefault();

		@TearDown
		public void close() throws Exception
		{
			httpClient.close();
			server.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class VertxWebClientState
	{
		TheServer server = new TheServer();
		URL url = Try.of(() -> new URL(server.url() + "big_16GB")).get();

		Vertx vertx = Vertx.vertx();
		WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(false).setDefaultHost(server.hostname()).setDefaultPort(server.port()));

		@TearDown
		public void close() throws Exception
		{
			webClient.close();
			vertx.close();
			server.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class VertxHttpClientState
	{
		TheServer server = new TheServer();
		URL url = Try.of(() -> new URL(server.url() + "big_16GB")).get();

		Vertx vertx = Vertx.vertx();
		HttpClient httpClient = vertx.createHttpClient(new WebClientOptions().setSsl(false).setDefaultHost(server.hostname()).setDefaultPort(server.port()));

		@TearDown
		public void close() throws Exception
		{
			httpClient.close();
			vertx.close();
			server.close();
		}
	}

	@Benchmark
	public void b1_UrlConnection(UrlConnectionState state, Blackhole blackhole) throws Exception
	{
		URLConnection con = state.url.openConnection();
		con.connect();
		try (InputStream input = con.getInputStream()) {
			long result = IOUtils.consume(input);
			blackhole.consume(result);
		}
	}

	@Benchmark
	public void b2_ApacheHttpClient(ApacheHttpClientState state, Blackhole blackhole) throws Exception
	{
		HttpGet get = new HttpGet(state.url.toURI());
		state.httpClient.execute(get, (ClassicHttpResponse response) -> {
			if (response.getCode() != 200) {
				throw new RuntimeException("Unexpected HTTP " + response.getCode());
			}
			try (InputStream in = response.getEntity().getContent()) {
				blackhole.consume(IOUtils.consume(in));
			}
			return null;
		});
	}

	@Benchmark
	public void b3_VertxHttpClient(VertxHttpClientState state, Blackhole blackhole) throws Exception
	{
		MutableLong size = new MutableLong();
		HttpClientResponse response = state.httpClient.request(HttpMethod.GET, "/big_16GB")
			.await()
			.send()
			.await();
		if (response.statusCode() != 200) {
			throw new IOException("Unexpected status: " + response.statusCode());
		}
		response.handler((buffer) -> size.add(buffer.length()));
		response.end().await();
		if (size.longValue() != 16L*1024*1024*1024) {
			throw new IOException("Unexpected size: " + size);
		}
		blackhole.consume(size.longValue());
	}

	@Benchmark
	public void b4_VertxWebClient(VertxWebClientState state, Blackhole blackhole) throws Exception
	{
		MutableLong size = new MutableLong();
		HttpResponse<Void> response = state.webClient.get("/big_16GB")
			.as(BodyCodec.pipe(new DummyWriteStream(size)))
			.send().await();
		if (response.statusCode() != 200) {
			throw new IllegalStateException("Unexpected status: " + response.statusCode());
		}
		if (size.longValue() != 16L*1024*1024*1024) {
			throw new IllegalStateException("Unexpected size: " + size);
		}
		blackhole.consume(size.longValue());
	}

	@RequiredArgsConstructor
	public static class DummyWriteStream implements WriteStream<Buffer>
	{
		final byte[] output = new byte[16*1024*1024];
		final MutableLong size;

		@Override
		public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler)
		{
			return this;
		}

		@Override
		public Future<Void> write(Buffer buffer)
		{
			size.add(buffer.length());
			buffer.getBytes(output);
			return Future.succeededFuture();
		}

		@Override
		public Future<Void> end()
		{
			return Future.succeededFuture();
		}

		@Override
		public WriteStream<Buffer> setWriteQueueMaxSize(int i)
		{
			return this;
		}

		@Override
		public boolean writeQueueFull()
		{
			return false;
		}

		@Override
		public WriteStream<Buffer> drainHandler(Handler<Void> handler)
		{
			return this;
		}
	}
}
