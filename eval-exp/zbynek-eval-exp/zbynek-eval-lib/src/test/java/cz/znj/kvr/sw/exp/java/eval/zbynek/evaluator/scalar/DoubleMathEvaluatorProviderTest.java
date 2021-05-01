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
 *
 */
public class DoubleMathEvaluatorProviderTest
{
	CustomizableEvaluatorFactory<Double> factory = DoubleMathEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.<Double>builder()).build();

	@Test
	public void testMath()
	{
		double result = factory.parse("+-1+2-3*4/6%2*^1").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0, 0.001);
	}

	@Test
	public void testDivZero()
	{
		double result = factory.parse("1/0").evaluate(new NullContext<>());
		Assert.assertEquals(result, Double.POSITIVE_INFINITY);
	}

	@Test
	public void testNaN()
	{
		double result = factory.parse("0/0").evaluate(new NullContext<>());
		Assert.assertEquals(result, Double.NaN);
	}

	@Test
	public void testRadians()
	{
		double result = factory.parse("radians(90)").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.toRadians(90), 0.001);
	}

	@Test
	public void testDegrees()
	{
		double result = factory.parse("degrees(pi())").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.toDegrees(Math.PI), 0.001);
	}

	@Test
	public void testSin()
	{
		double result = factory.parse("sin(pi()/2)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0, 0.0001);
	}

	@Test
	public void testAsin()
	{
		double result = factory.parse("asin(sin(pi()/2))").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.PI/2, 0.0001);
	}

	@Test
	public void testCos()
	{
		double result = factory.parse("cos(0)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0, 0.0001);
	}

	@Test
	public void testAcos()
	{
		double result = factory.parse("acos(cos(0))").evaluate(new NullContext<>());
		Assert.assertEquals(result, 0, 0.0001);
	}

	@Test
	public void testTan()
	{
		double result = factory.parse("tan(pi()/4)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0, 0.0001);
	}

	@Test
	public void testAtan()
	{
		double result = factory.parse("atan(tan(pi()/4))").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.PI/4, 0.0001);
	}

	@Test
	public void testCotan()
	{
		double result = factory.parse("cotan(pi()/4)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1.0, 0.0001);
	}

	@Test
	public void testAcotan()
	{
		double result = factory.parse("acotan(cotan(pi()/4))").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.PI/4, 0.0001);
	}

	@Test
	public void testAtan2()
	{
		double result = factory.parse("atan2(2, 2)").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.PI/4, 0.0001);
	}

	@Test
	public void testLn()
	{
		double result = factory.parse("ln(e())").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1, 0.0001);
	}

	@Test
	public void testLog2()
	{
		double result = factory.parse("log2(8)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 3, 0.0001);
	}

	@Test
	public void testLog10()
	{
		double result = factory.parse("log10(1000)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 3, 0.0001);
	}

	@Test
	public void testExp()
	{
		double result = factory.parse("exp(2)").evaluate(new NullContext<>());
		Assert.assertEquals(result, Math.exp(2), 0.0001);
	}

	@Test
	public void testPow()
	{
		double result = factory.parse("pow(10, 3)").evaluate(new NullContext<>());
		Assert.assertEquals(result, 1000, 0.0001);
	}

	@Test
	public void testNullfor()
	{
		{
			Double result = factory.parse("nullfor(1, 2)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1.0);
		}
		{
			Double result = factory.parse("nullfor(1, 1)").evaluate(new NullContext<>());
			Assert.assertEquals(result, null);
		}
	}

	@Test
	public void testIfnull()
	{
		{
			double result = factory.parse("ifnull(1, 2)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 1.0);
		}
		{
			double result = factory.parse("ifnull(a, 2)").evaluate(new NullContext<>());
			Assert.assertEquals(result, 2.0);
		}
	}
}
