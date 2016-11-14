package cz.znj.kvr.sw.exp.java.lombok.basic;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class LombokDelegate
{
	private static class DelegateObject
	{
		private interface Adder {
			void add(String value);
		}

		@Getter
		@Delegate(types = Adder.class)
		private Set<String> values = new HashSet<>();
	}

	@Test
	public void testDelegate() {
		DelegateObject d = new DelegateObject();
		d.add("Hello");
		Assert.assertEquals(1, d.getValues().size());
	}
}
