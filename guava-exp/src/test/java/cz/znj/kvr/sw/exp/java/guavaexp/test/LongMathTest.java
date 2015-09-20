package cz.znj.kvr.sw.exp.java.guavaexp.test;

import com.google.common.math.LongMath;
import com.google.common.primitives.UnsignedLong;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;


/**
 * Created by rat on 2015-09-20.
 */
public class LongMathTest
{
	@Test
	public void			testModulo()
	{
		Assert.assertEquals(-1, -4L%3L);
		Assert.assertEquals(2, LongMath.mod(-4L, 3L));
	}
}
