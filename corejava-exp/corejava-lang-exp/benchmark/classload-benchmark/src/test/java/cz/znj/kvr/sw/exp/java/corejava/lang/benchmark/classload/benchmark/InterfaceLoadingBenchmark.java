package cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.benchmark;


import cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.generator.ClassLoadingBenchmarkGenerator;
import lombok.extern.java.Log;

import java.util.logging.Level;


/**
 *
 */
@Log
public class InterfaceLoadingBenchmark
{
	public static void main(String[] args)
	{
		for (int i = 0; i < ClassLoadingBenchmarkGenerator.WARMUP_CLASS_COUNT; ++i) {
			BenchmarkCommon.loadClass(ClassLoadingBenchmarkGenerator.WARMUP_INTERFACE_PREFIX+i);
		}
		long startTime = System.nanoTime();
		for (int i = 0; i < ClassLoadingBenchmarkGenerator.BENCHMARK_CLASS_COUNT; ++i) {
			BenchmarkCommon.loadClass(ClassLoadingBenchmarkGenerator.BENCHMARK_INTERFACE_PREFIX+i);
		}
		long endTime = System.nanoTime();
		log.log(Level.INFO, String.format("Interfaces load: %d in %d ns, 1 interface in %d ns", ClassLoadingBenchmarkGenerator.BENCHMARK_CLASS_COUNT, endTime-startTime, (endTime-startTime)/ClassLoadingBenchmarkGenerator.BENCHMARK_CLASS_COUNT));
	}
}
