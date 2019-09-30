package cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.benchmark;



public final class BenchmarkCommon
{
	public static final ClassLoader CLASS_LOADER = BenchmarkCommon.class.getClassLoader();

	public static Class<?> loadClass(String className) {
		try {
			return CLASS_LOADER.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
