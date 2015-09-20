package cz.znj.kvr.sw.exp.java.guavaexp.test;

import com.google.common.primitives.UnsignedLong;
import com.google.common.primitives.UnsignedLongs;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;


/**
 * Created by rat on 2015-09-20.
 */
public class GuavaUnsignedLongTest
{
	@Test
	public void			testFormat()
	{
		Assert.assertEquals("1234", UnsignedLong.valueOf(1234).toString());
		Assert.assertEquals("18446744073709551615", UnsignedLong.fromLongBits(-1).toString());
		Assert.assertEquals("18446744073709551615", UnsignedLongs.toString(-1));
		Assert.assertEquals(new BigInteger(1, new byte[]{ -1, -1, -1, -1, -1, -1, -1, -1 }).toString(), UnsignedLong.fromLongBits(-1).toString());
	}

	@Test
	public void			testDivMod()
	{
		// the number actually is not -1 but (unsignedlong)-1
		Assert.assertEquals(0, UnsignedLongs.remainder(-1, 3));
	}
}
