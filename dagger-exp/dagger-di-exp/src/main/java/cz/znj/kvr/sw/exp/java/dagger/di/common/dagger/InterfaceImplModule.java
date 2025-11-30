package cz.znj.kvr.sw.exp.java.dagger.di.common.dagger;

import cz.znj.kvr.sw.exp.java.dagger.di.common.Facade;
import cz.znj.kvr.sw.exp.java.dagger.di.common.First;
import cz.znj.kvr.sw.exp.java.dagger.di.common.Second;
import cz.znj.kvr.sw.exp.java.dagger.di.common.impl.FacadeImpl;
import cz.znj.kvr.sw.exp.java.dagger.di.common.impl.FirstImpl;
import cz.znj.kvr.sw.exp.java.dagger.di.common.impl.SecondImpl;
import dagger.Module;
import dagger.Provides;

import jakarta.inject.Singleton;


/**
 *
 */
@Module
public class InterfaceImplModule
{
	@Singleton
	@Provides
	public static First first()
	{
		return new FirstImpl();
	}

	@Provides
	@Singleton
	public static Second second()
	{
		return new SecondImpl();
	}

	@Provides
	@Singleton
	public Facade facade(First first, Second second)
	{
		return new FacadeImpl(first, second);
	}
}
