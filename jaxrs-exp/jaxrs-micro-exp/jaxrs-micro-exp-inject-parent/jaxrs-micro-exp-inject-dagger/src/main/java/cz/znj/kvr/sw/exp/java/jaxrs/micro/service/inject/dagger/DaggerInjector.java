package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.inject.dagger;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.Element;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.TypeConverterBinding;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DaggerInjector implements Injector
{
	public DaggerInjector(Object component)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void injectMembers(Object instance)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(Class<T> type)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Map<Key<?>, Binding<?>> getBindings()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Map<Key<?>, Binding<?>> getAllBindings()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> Binding<T> getBinding(Key<T> key)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> Binding<T> getBinding(Class<T> type)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> Binding<T> getExistingBinding(Key<T> key)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> Provider<T> getProvider(Key<T> key)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> T getInstance(Key<T> key)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> T getInstance(Class<T> type)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Injector getParent()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Injector createChildInjector(Iterable<? extends Module> modules)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Injector createChildInjector(Module... modules)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Map<Class<? extends Annotation>, Scope> getScopeBindings()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Set<TypeConverterBinding> getTypeConverterBindings()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public List<Element> getElements()
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Map<TypeLiteral<?>, List<InjectionPoint>> getAllMembersInjectorInjectionPoints()
	{
		throw new UnsupportedOperationException("TODO");
	}
}
