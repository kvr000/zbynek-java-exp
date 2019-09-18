package cz.znj.kvr.sw.exp.java.corejava.classpath.benchmark;

import lombok.extern.log4j.Log4j2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;


@Log4j2
public class ClasspathPropertiesLoaderBenchmark
{
	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkUtf8Reader() throws IOException
	{
		try (Reader reader = openClasspathPropertiesReader()) {
			Properties properties = new Properties();
			properties.load(reader);
		}
		Optional.<String>empty().map(String::length);
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkInputStream() throws IOException
	{
		try (InputStream reader = openClasspathProperties()) {
			Properties properties = new Properties();
			properties.load(reader);
		}
	}

	private InputStream openClasspathProperties()
	{
		return Objects.requireNonNull(getClass().getResourceAsStream(
				"/cz/znj/kvr/sw/exp/java/corejava/classpath/benchmark/ClasspathPropertiesLoaderBenchmark-resource1000.properties"),
				"Failed to open ClasspathPropertiesLoaderBenchmark-resource1000.properties");
	}

	private Reader openClasspathPropertiesReader()
	{
		return new InputStreamReader(openClasspathProperties(), StandardCharsets.UTF_8);
	}
}
