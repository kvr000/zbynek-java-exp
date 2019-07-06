package cz.znj.kvr.sw.exp.java.corejava.lang.classload;


/**
 * Combining factory methods into interface method (which is bad approach but may be needed for some work-arounds).
 *
 * @author
 * 	Zbynek Vyskovsky
 */
public interface InterfaceStaticMethods
{
	public static final InterfaceStaticMethods instance = new InterfaceStaticMethods() {
		@Override
		public String greet(String name)
		{
			return "Hello";
		}
	};

	public static InterfaceStaticMethods getInstance()
	{
		return instance;
	}

	String greet(String name);
}
