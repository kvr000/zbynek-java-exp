package cz.znj.kvr.sw.exp.java.lombok.basic;


import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.testng.annotations.Test;

public class LombokBasic
{
	private static class MethodsClass
	{
		@Getter
		@Setter
		private int var = 0;
	}

	@Test
	public void testMethods() {
		MethodsClass cl = new MethodsClass();
		Assert.assertEquals(0, cl.getVar());
		cl.setVar(1);
		Assert.assertEquals(1, cl.getVar());
	}
}
