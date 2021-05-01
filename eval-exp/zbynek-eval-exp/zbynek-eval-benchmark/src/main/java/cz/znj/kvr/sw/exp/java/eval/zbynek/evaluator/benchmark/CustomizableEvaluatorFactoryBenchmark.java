package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.benchmark;

import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.CustomizableEvaluatorFactory;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.EvaluatorFactory;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.NullContext;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.scalar.StandardLongEvaluatorProvider;
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
import java.util.concurrent.TimeUnit;


/**
 * Benchmark parsing and evaluating expressions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 4, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
public class CustomizableEvaluatorFactoryBenchmark
{
	CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.<Long>builder())
			.build();
	EvaluatorFactory.Expression<Long> expression = factory.parse("2*8+10-8/2");

	@Benchmark
	public void benchmark0_construction(Blackhole blackhole) throws IOException
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.<Long>builder())
			.build();
		blackhole.consume(factory);
	}

	@Benchmark
	public void benchmark1_parse(Blackhole blackhole) throws IOException
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("2*8+10-8/2");
		blackhole.consume(expression);
	}

	@Benchmark
	public void benchmark2_evaluate(Blackhole blackhole) throws IOException
	{
		Long value = expression.evaluate(new NullContext<>());
		blackhole.consume(value);
	}

	@Benchmark
	public void benchmark3_native(Blackhole blackhole)
	{
		// likely a constant or optimized by JIT
		Long value = 2L*8L+10L-8L/2L;
		blackhole.consume(value);
	}
}
