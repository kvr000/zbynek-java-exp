package cz.znj.kvr.sw.exp.java.dagger.di.lifecycle;

import cz.znj.kvr.sw.exp.java.dagger.di.common.dagger.DaggerInterfaceImplComponent;
import cz.znj.kvr.sw.exp.java.dagger.di.common.dagger.InterfaceImplComponent;
import cz.znj.kvr.sw.exp.java.dagger.di.common.impl.FacadeImpl;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Javax annotation tests.
 */
public class JavaxAnnotationTest
{
	@Test(enabled = false) // PostConstruct does not work
	public void testLifecycle()
	{
		InterfaceImplComponent dagger = DaggerInterfaceImplComponent.create();
		Assert.assertTrue(((FacadeImpl) dagger.facade()).isInitialized(), "PostConstruct initialized");
	}
}
