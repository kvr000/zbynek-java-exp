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
		double result = factory.parse("+-1+2-3*4/6%2^1").evaluate(new NullContext<>());
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
}
