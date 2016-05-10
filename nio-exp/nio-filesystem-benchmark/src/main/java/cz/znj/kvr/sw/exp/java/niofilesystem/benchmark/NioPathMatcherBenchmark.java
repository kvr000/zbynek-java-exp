package cz.znj.kvr.sw.exp.java.niofilesystem.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;


/**
 * Benchmark parsing and evaluating path matcher.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 4, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
public class NioPathMatcherBenchmark
{
	Path pathFile = Paths.get("file.ext");
	Path pathDir = Paths.get("the/dir/file.ext");

	PathMatcher recursiveMatcher = FileSystems.getDefault().getPathMatcher("glob:**.ext");

	@Benchmark
	public void b1_1_parse(Blackhole blackhole) throws IOException
	{
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.ext");
		blackhole.consume(matcher);
	}

	@Benchmark
	public void b2_1_matchFile(Blackhole blackhole) throws IOException
	{
		boolean result = recursiveMatcher.matches(pathFile);
		blackhole.consume(result);
	}

	@Benchmark
	public void b2_1_matchRecursive(Blackhole blackhole) throws IOException
	{
		boolean result = recursiveMatcher.matches(pathDir);
		blackhole.consume(result);
	}
}
