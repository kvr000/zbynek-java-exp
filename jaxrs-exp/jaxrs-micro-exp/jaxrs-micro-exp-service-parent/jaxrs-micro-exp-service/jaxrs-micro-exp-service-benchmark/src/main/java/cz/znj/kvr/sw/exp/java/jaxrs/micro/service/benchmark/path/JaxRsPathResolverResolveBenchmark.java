package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.benchmark.path;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.path.JaxRsPathResolver;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.ControllerMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.JaxRsCaptureReader;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.JaxRsCaptureReaderImpl;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.MethodMeta;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Measured on Core(TM) i7-4510U CPU @ 2.00GHz:
 *
 * Benchmark                                           Mode  Cnt    Score     Error  Units
 * JaxRsPathResolverResolveBenchmark.benchmarkResolve  avgt    3  224.188 ± 121.627  ns/op
 *
 * Almost 5 millions path lookups per second on laptop CPU.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 2)
@Measurement(iterations = 5, time = 2)
public class JaxRsPathResolverResolveBenchmark
{
	@Benchmark
	public void benchmarkResolve() throws IOException
	{
		resolver.resolvePath("sub/deeper/zbynek-vyskovsky/", null);
	}

	private final JaxRsPathResolver<Object, Handler> resolver = ((Supplier<JaxRsPathResolver<Object, Handler>>) () -> {
		try (
				InputStream stream = Objects.requireNonNull(
						JaxRsPathResolver.class.getResourceAsStream("/cz/znj/kvr/sw/exp/java" +
							"/jaxrs/micro/processor/benchmark/main/controller" +
							"/JaxRsMetadata.xml"),
						() -> "Cannot open resource by class: /cz/znj/kvr/sw/exp/java/jaxrs/micro/controller/benchmark/main/controller/JaxRsMetadata.xml");
				JaxRsCaptureReader reader = new JaxRsCaptureReaderImpl(stream)
		) {
			JaxRsPathResolver.Builder<Object, Handler> builder = new JaxRsPathResolver.Builder<>();
			ControllerMeta controller;
			while ((controller = reader.next()) != null) {
				for (MethodMeta method: controller.getMethods()) {
					builder.registerPath(builder.concatPaths(controller.getPath(), method.getPath()), new Handler());
				}
			}
			return builder.build();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}).get();

	private static class Handler implements Predicate<Object>
	{
		@Override
		public boolean test(Object o)
		{
			return true;
		}
	}
}
