package cz.znj.kvr.sw.exp.java.corejava.math;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.math.BigDecimal;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class BigDecimalTest
{
	@Test
	public void testPowerScaled()
	{
		BigDecimal dec = BigDecimal.valueOf(1, 2);
		AssertJUnit.assertEquals(new BigDecimal("0.01"), dec);
	}

	@Test
	public void testPowerUnscaled()
	{
		BigDecimal dec = BigDecimal.valueOf(1, -2);
		AssertJUnit.assertEquals(0, new BigDecimal("100").compareTo(dec));
	}

}
