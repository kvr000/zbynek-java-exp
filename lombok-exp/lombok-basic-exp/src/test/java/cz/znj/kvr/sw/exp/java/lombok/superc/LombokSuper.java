package cz.znj.kvr.sw.exp.java.lombok.superc;


import lombok.AllArgsConstructor;


public class LombokSuper
{
	@AllArgsConstructor
	private static class ParentClass
	{
		private String parentName;
	}

	/* This does not work, complains about non-existent parent constructor. */
//	@AllArgsConstructor
//	private static class ChildClass extends ParentClass
//	{
//		private String childName;
//	}
}
