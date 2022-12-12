package cz.znj.kvr.sw.exp.java.message.pubsub.jedis.benchmark;


import com.google.common.base.Strings;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


@RequiredArgsConstructor
public class BenchmarkFormatter implements AutoCloseable
{
	private final PrintStream writer;

	private List<Benchmark> benchmarks = new ArrayList<>();

	public void printBenchmark(Benchmark benchmark) {
		benchmarks.add(benchmark);
	}

	public void close()
	{
		int nameLength = getLength("Benchmark", Benchmark::getName);
		int modeLength = getLength("Mode", Benchmark::getMode);
		int cntLength = getLength("Cnt", b -> "0");
		int scoreLength = getLength("Score", Benchmark::getScore);
		int errorLength = getLength("Error", Benchmark::getError);
		int unitsLength = getLength("Units", Benchmark::getUnits);

		String format = "%-"+nameLength+"s  %-"+modeLength+"s  %"+cntLength+"s  %"+scoreLength+"s  %"+errorLength+"s  %-"+unitsLength+"s\n";
		writer.print(String.format(format,
			"Benchmark",
			"Mode",
			"Cnt",
			"Score",
			"Error",
			"Units"
		));
		for (Benchmark benchmark: benchmarks) {
			writer.print(String.format(format,
				Strings.nullToEmpty(benchmark.getName()),
				Strings.nullToEmpty(benchmark.getMode()),
				0,
				Strings.nullToEmpty(benchmark.getScore()),
				Strings.nullToEmpty(benchmark.getError()),
				Strings.nullToEmpty(benchmark.getUnits())
			));
		}
	}

	private int getLength(String base, Function<Benchmark, String> mapper)
	{
		return benchmarks.stream()
			.map(mapper)
			.map(s -> Optional.ofNullable(s).map(String::length).orElse(0))
			.reduce(base.length(), Math::max);
	}

	@Value
	@Builder(builderClassName = "Builder")
	public static class Benchmark
	{
		String name;
		String mode;
		String units;
		String error;
		String score;
	}
}
