package cz.znj.kvr.sw.exp.java.corejava.stream;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class StreamParallelOrderedTest
{
	@Test
	public void collectToList_parallelOrdered_retainOrder()
	{
		List<Integer> values = IntStream.range(1, Runtime.getRuntime().availableProcessors()+1)
			.boxed()
			.collect(Collectors.toList());
		Collections.reverse(values);

		Stopwatch watch = Stopwatch.createStarted();

		List<Integer> result = values.stream().parallel()
			.map(new Function<Integer, Integer>() {
				@Override
				@SneakyThrows
				public Integer apply(Integer v)
				{
					Thread.sleep(v*100);
					return v;
				}
			})
			.toList();

		assertEquals(result, values);
		assertTrue(watch.elapsed(TimeUnit.MILLISECONDS) < (values.get(0)+1L)*100L*3/2);
	}

	@Test
	public void collectToImmutableList_parallelOrdered_retainOrder()
	{
		List<Integer> values = IntStream.range(1, Runtime.getRuntime().availableProcessors()+1)
			.boxed()
			.collect(Collectors.toList());
		Collections.reverse(values);

		Stopwatch watch = Stopwatch.createStarted();

		List<Integer> result = values.stream().parallel()
			.map(new Function<Integer, Integer>() {
				@Override
				@SneakyThrows
				public Integer apply(Integer v)
				{
					Thread.sleep(v*100);
					return v;
				}
			})
			.collect(ImmutableList.toImmutableList());

		assertEquals(result, values);
		assertTrue(watch.elapsed(TimeUnit.MILLISECONDS) < (values.get(0)+1L)*100L*3/2);
	}
}
