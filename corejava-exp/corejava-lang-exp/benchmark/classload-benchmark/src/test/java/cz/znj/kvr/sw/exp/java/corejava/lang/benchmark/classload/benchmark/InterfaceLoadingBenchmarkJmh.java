package cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.benchmark;


import cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.generator.ClassLoadingBenchmarkGenerator;
import lombok.extern.java.Log;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


/**
 * JMH benchmark. This does not work well for need of freshly started JVMs.
 */
@Log
public class InterfaceLoadingBenchmarkJmh
{
	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@Warmup(iterations = JmhBenchmarkCommon.WARMUP_ITERATIONS, batchSize = JmhBenchmarkCommon.BATCH_SIZE)
	@Fork(warmups = JmhBenchmarkCommon.FORK_WARMUPS, value = JmhBenchmarkCommon.FORK_COUNT)
	@Measurement(iterations = 1, batchSize = JmhBenchmarkCommon.BATCH_SIZE)
	public void measureInterfaceLoad()
	{
		for (int i = 0; i < ClassLoadingBenchmarkGenerator.BENCHMARK_CLASS_COUNT; ++i) {
			BenchmarkCommon.loadClass(ClassLoadingBenchmarkGenerator.BENCHMARK_INTERFACE_PREFIX+i);
		}
	}
}
