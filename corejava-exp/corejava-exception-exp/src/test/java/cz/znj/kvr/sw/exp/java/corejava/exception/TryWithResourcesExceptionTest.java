package cz.znj.kvr.sw.exp.java.corejava.exception;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class TryWithResourcesExceptionTest
{
	@Test(expectedExceptions = NumberFormatException.class)
	public void			testCloseDespiteInitializationFailure() throws Exception
	{
		boolean finallyCalled = false;
		try {
			try (AutoCloseable closeable = throwException()) {
			}
			finally {
				finallyCalled = true;
			}
		}
		finally {
			Assert.assertTrue(finallyCalled);
		}
	}

	private AutoCloseable		throwException()
	{
		throw new NumberFormatException("failed");
	}
}
