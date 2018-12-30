package cz.znj.kvr.sw.exp.java.dagger.di.common.dagger;

import cz.znj.kvr.sw.exp.java.dagger.di.common.Facade;
import cz.znj.kvr.sw.exp.java.dagger.di.common.First;
import cz.znj.kvr.sw.exp.java.dagger.di.common.Second;
import dagger.Component;

import javax.inject.Singleton;


/**
 *
 */
@Component(modules = InterfaceImplModule.class)
@Singleton
public interface InterfaceImplComponent
{
	First first();

	Second second();

	Facade facade();
}
