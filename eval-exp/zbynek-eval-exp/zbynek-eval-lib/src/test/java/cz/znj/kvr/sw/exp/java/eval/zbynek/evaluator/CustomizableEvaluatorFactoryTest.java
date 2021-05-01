/*
 * Copyright 2015 Zbynek Vyskovsky mailto:kvr000@gmail.com http://github.com/kvr000/zbynek-java/exp/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.scalar.StandardLongEvaluatorProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * Tests for {@link CustomizableEvaluatorFactory} .
 */
public class CustomizableEvaluatorFactoryTest
{
	CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.builder()).build();

	@Test
	public void testEmpty()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse(""));
	}

	@Test
	public void testConstant()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("1977");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1977L);
	}

	@Test
	public void testTwoStart()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("1 2"));
	}

	@Test
	public void testSpace()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse(" \t1977  ");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1977L);
	}

	@Test
	public void testUnaryOperator()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("- 1977");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) (-1977L));
	}

	@Test
	public void testNoArgument()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("-"));
	}

	@Test
	public void testUnaryDoubleOperator()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("- - 1977");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1977L);
	}

	@Test
	public void testBinaryOperator()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("1977+3");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1980L);
	}

	@Test
	public void testBinaryDouble()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("1 + / 2"));
	}

	@Test
	public void testBinaryOperatorMulti()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("1977+3+12");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1992L);
	}

	@Test
	public void testBinaryOperatorPrio()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("1977+3*12");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 2013L);
	}

	@Test
	public void testBinaryOperatorReverseSamePrio()
	{
		EvaluatorFactory.Expression<Long> expression = withBuilder()
			.binaryOperators(ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
				.putAll(StandardLongEvaluatorProvider.mathBinaryOperators())
				.put("$$$^", Maps.immutableEntry(
					(args) -> new StandardLongEvaluatorProvider.BinaryLongExpression(args) {
						@Override
						public Long evaluateValid(Long left, Long right)
						{
							return (long) Math.pow(left, right);
						}
					},
					-6
				))
				.build()
			)
			.build().parse("5+2$$$^3$$$^3+6");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 0x0800000bL);
	}

	@Test
	public void testParenthesis()
	{
		EvaluatorFactory.Expression<Long> expression = factory.parse("1977*(3+12)");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 29655L);
	}

	@Test
	public void testParenthesisUnclosed()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("1977*(3+12"));
	}


	@Test
	public void testParenthesisUnmatching()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("1977*3+12)"));
	}

	@Test
	public void testFunction()
	{
		EvaluatorFactory.Expression<Long> expression = withSum().build()
			.parse("sum(1, 2, 3, 4, 5)");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 15L);
	}

	@Test
	public void testNestedFunction()
	{
		EvaluatorFactory.Expression<Long> expression = withSum().build()
			.parse("sum(1, 2, 3, sum(4, 7), 5)");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 22L);
	}

	@Test
	public void testExpressionInFunction()
	{
		EvaluatorFactory.Expression<Long> expression = withSum().build()
			.parse("sum(1*9, 2, 3*4, sum(4*5, 7*6), 5*3)");
		Long result = expression.evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 100L);
	}

	@Test
	public void testDoubleSeparator()
	{
		IllegalArgumentException ex = Assert.expectThrows(IllegalArgumentException.class, () ->
			factory.parse("sum(1,,2"));
	}

	@Test
	public void testVariables()
	{
		EvaluatorFactory.Expression<Long> expression = withSum().build()
			.parse("a+b");
		long result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L, "b", 3L)));
		Assert.assertEquals(result, 1980L);
	}

	@Test
	public void testVariablesNull()
	{
		EvaluatorFactory.Expression<Long> expression = withSum().build()
			.parse("a+b");
		Long result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L)));
		Assert.assertEquals(result, null);
	}

	private CustomizableEvaluatorFactory.Builder<Long> withBuilder()
	{
		return StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.builder());
	}

	private CustomizableEvaluatorFactory.Builder<Long> withSum()
	{
		return StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.builder())
			.functions(ImmutableMap.of(
				"sum", (args) -> new StandardLongEvaluatorProvider.LongTypedExpression()
				{
					@Override
					public Long evaluate(EvaluatorFactory.Context<Long> parameters)
					{
						return args.stream().reduce(0L, (u, e) -> u+e.evaluate(parameters), Long::sum);
					}
				}
			));
	}
}
