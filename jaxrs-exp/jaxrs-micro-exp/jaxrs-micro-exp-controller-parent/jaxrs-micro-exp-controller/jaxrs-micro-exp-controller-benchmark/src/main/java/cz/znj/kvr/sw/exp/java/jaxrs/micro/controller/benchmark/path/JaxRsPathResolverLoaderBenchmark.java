package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.benchmark.path;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.path.JaxRsPathResolver;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader.ControllerMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader.JaxRsCaptureReader;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader.JaxRsCaptureReaderImpl;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader.MethodMeta;
import lombok.extern.log4j.Log4j2;
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
 * Benchmark                                       Mode  Cnt      Score      Error  Units
 * JaxRsPathResolverLoaderBenchmark.benchmarkLoad  avgt    3  86082.397 ± 6678.058  ns/op
 *
 * About 11600 API loads per second on laptop CPU.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 2)
@Measurement(iterations = 5, time = 2)
public class JaxRsPathResolverLoaderBenchmark
{
	@Benchmark()
	public void benchmarkLoad() throws IOException
	{
		try (
				InputStream stream = Objects.requireNonNull(
						JaxRsPathResolver.class.getResourceAsStream("/cz/znj/kvr/sw/exp/java/jaxrs/micro/controller/benchmark/main/controller/JaxRsMetadata.xml"),
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
			builder.build();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static class Handler implements Predicate<Object>
	{
		@Override
		public boolean test(Object o)
		{
			return true;
		}
	}
}
