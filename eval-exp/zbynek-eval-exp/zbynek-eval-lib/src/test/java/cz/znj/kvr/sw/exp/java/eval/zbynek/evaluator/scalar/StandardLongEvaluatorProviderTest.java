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
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.NullContext;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link StandardLongEvaluatorProviderTest}.
 */
public class StandardLongEvaluatorProviderTest
{
	CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.<Long>builder()).build();

	@Test
	public void testMath()
	{
		Long result = factory.parse("+-1+2-3*4/6%2*^1").evaluate(new NullContext<>());
		Assert.assertEquals(result, (Long) 1L);
	}

	@Test
	public void testBit()
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateBit(CustomizableEvaluatorFactory.<Long>builder()).build();
		{
			Long result = factory.parse("~-2").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 1L);
		}
		{
			Long result = factory.parse("2^3").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 1L);
		}
		{
			Long result = factory.parse("5&3").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 1L);
		}
		{
			Long result = factory.parse("3|4").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 7L);
		}
		{
			Long result = factory.parse("3|4&7^1").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 7L);
		}
	}

	@Test
	public void testShift()
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateBit(CustomizableEvaluatorFactory.<Long>builder()).build();
		{
			Long result = factory.parse("1<<3").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) 8L);
		}
		{
			Long result = factory.parse("-8>>2").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) (-8L>>2));
		}
		{
			Long result = factory.parse("-8>>>2").evaluate(new NullContext<>());
			Assert.assertEquals(result, (Long) (-8L>>>2));
		}
	}

	@Test
	public void testSwaps()
	{
		{
			long result = factory.parse("swap16(0x7edcba9876543210)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 0x1032L);
		}
		{
			long result = factory.parse("swap32(0x7edcba9876543210)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 0x10325476L);
		}
		{
			long result = factory.parse("swap64(0x7edcba9876543210)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 0x1032547698badc7eL);
		}
	}

	@Test
	public void testNullfor()
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateBit(CustomizableEvaluatorFactory.<Long>builder()).build();
		{
			long result = factory.parse("nullfor(1, 2)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1L);
		}
		{
			Long result = factory.parse("nullfor(1, 1)").evaluate(new NullContext<>());
			Assert.assertEquals(result, null);
		}
	}

	@Test
	public void testIfnull()
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateBit(CustomizableEvaluatorFactory.<Long>builder()).build();
		{
			long result = factory.parse("ifnull(1, 2)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1L);
		}
		{
			long result = factory.parse("ifnull(a, 1)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1L);
		}
	}
}
