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

package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.scalar;

import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.CustomizableEvaluatorFactory;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.MapContext;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.NullContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;


/**
 * Tests for {@link DoubleEvaluatorProviderCrossTest}.
 */
public class DoubleEvaluatorProviderCrossTest
{
	CustomizableEvaluatorFactory<?> factory = DoubleMathEvaluatorProvider.populateAllAndBoolean(CustomizableEvaluatorFactory.builder()).build();

	@Test
	public void testMath()
	{
		Object result = factory.parse("+-1+2-3*4/6%2*^1").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0);
	}

	@Test
	public void testCmp()
	{
		{
			Object result = factory.parse("1 == 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("1 == 0").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			Object result = factory.parse("1 != 0").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("1 != 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			Object result = factory.parse("0 < 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("1 < 0").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			Object result = factory.parse("1 <= 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("1 <= 0").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			Object result = factory.parse("1 > 0").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("0 > 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			Object result = factory.parse("1 >= 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			Object result = factory.parse("0 >= 1").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
	}

	@Test
	public void testIsnull()
	{
		{
			Object result = factory.parse("isnull(a)").evaluate(new MapContext<>(Collections.singletonMap("a", null)));
			Assert.assertEquals(result, true);
		}
		{
			@SuppressWarnings("unchecked")
			Object result = factory.parse("isnull(a)").evaluate(new MapContext(Collections.singletonMap("a", (Object) 1L)));
			Assert.assertEquals(result, false);
		}
	}

	@Test
	public void testCrossOperator()
	{
		{
			Object result = factory.parse("(1 == 1) == (0 == 0)").evaluate(new MapContext<>(Collections.singletonMap("a", null)));
			Assert.assertEquals(result, true);
		}
	}

	@Test
	public void testIfelse()
	{
		{
			Object result = factory.parse("ifelse(true(), 1, 0)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1.0);
		}
		{
			@SuppressWarnings("unchecked")
			Object result = factory.parse("ifelse(false(), 1, 0)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 0.0);
		}
	}
}
