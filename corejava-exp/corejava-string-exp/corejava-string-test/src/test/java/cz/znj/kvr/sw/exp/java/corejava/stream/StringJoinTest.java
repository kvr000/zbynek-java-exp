package cz.znj.kvr.sw.exp.java.corejava.stream;

import org.testng.annotations.Test;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;


public class StringJoinTest
{
	private static final Collector<String, StringBuilder, String> collectorItemAppend = Collector.of(
		StringBuilder::new,
		(StringBuilder b, String s) -> b.append(s).append("\n"),
		StringBuilder::append,
		StringBuilder::toString
	);

	@Test
	public void collectorItemAppend_empty_empty()
	{
		String result = Stream.<String>of()
			.collect(collectorItemAppend);

		assertEquals(result, "");
	}

	@Test
	public void collectorItemAppend_singleItem_fullLine()
	{
		String result = Stream.<String>of("hello")
			.collect(collectorItemAppend);

		assertEquals(result, "hello\n");
	}

	@Test
	public void collectorItemAppend_multipleItems_fullLines()
	{
		String result = Stream.<String>of("hello", "world")
			.collect(collectorItemAppend);

		assertEquals(result, "hello\nworld\n");
	}

	@Test
	public void stringJoinerSuffix_empty_hasUnwantedSuffix()
	{
		String result = Stream.<String>of()
			.collect(Collectors.joining("\n", "", "\n"));

		assertEquals(result, "\n");
	}

	@Test
	public void stringJoinerSuffix_single_hasExpectedSuffix()
	{
		String result = Stream.<String>of("hello")
			.collect(Collectors.joining("\n", "", "\n"));

		assertEquals(result, "hello\n");
	}

	@Test
	public void stringJoinerSuffix_multi_hasAllEols()
	{
		String result = Stream.<String>of("hello", "world")
			.collect(Collectors.joining("\n", "", "\n"));

		assertEquals(result, "hello\nworld\n");
	}
}
