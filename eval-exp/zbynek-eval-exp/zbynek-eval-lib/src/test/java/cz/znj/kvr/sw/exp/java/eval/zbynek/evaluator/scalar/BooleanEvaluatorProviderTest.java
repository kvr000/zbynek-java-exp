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
 * Tests for {@link BooleanEvaluatorProviderTest}.
 */
public class BooleanEvaluatorProviderTest
{
	CustomizableEvaluatorFactory<Boolean> factory = BooleanEvaluatorProvider.populateBoolean(CustomizableEvaluatorFactory.<Boolean>builder()).build();

	@Test
	public void testNot()
	{
		{
			boolean result = factory.parse("!false()").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			boolean result = factory.parse("!true()").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
	}

	@Test
	public void testCmp()
	{
		{
			boolean result = factory.parse("true() == true()").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			boolean result = factory.parse("true() == false()").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			boolean result = factory.parse("true() != true()").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
		{
			boolean result = factory.parse("true() != false()").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
	}

	@Test
	public void testIfelse()
	{
		{
			boolean result = factory.parse("ifelse(true(), true(), false())").evaluate(new NullContext<>());
			Assert.assertEquals(result, true);
		}
		{
			boolean result = factory.parse("ifelse(false(), true(), false())").evaluate(new NullContext<>());
			Assert.assertEquals(result, false);
		}
	}
}
