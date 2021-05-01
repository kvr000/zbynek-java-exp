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
	@Test
	public void testMath()
	{
		CustomizableEvaluatorFactory<Long> factory = StandardLongEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.<Long>builder()).build();
		Long result = factory.parse("+-1+2-3*4/6%2^1").evaluate(new NullContext<>());
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
}
