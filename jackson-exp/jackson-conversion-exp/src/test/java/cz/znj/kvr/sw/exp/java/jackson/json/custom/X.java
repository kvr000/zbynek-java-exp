package cz.znj.kvr.sw.exp.java.jackson.json.custom;

import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


/**
 *
 */
public interface X
{
	public static final X instance = new Supplier<X>() { public X get() { return ServiceLoader.load(X.class).iterator().next(); } }.get();

	static X getInstance() {
		return instance;
	}
}
