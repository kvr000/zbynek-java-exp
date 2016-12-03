package cz.znj.kvr.sw.exp.java.guice.di.common.impl;

import cz.znj.kvr.sw.exp.java.guice.di.common.First;

/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class FirstImpl implements First {
	@Override
	public int getFirstValue() {
		return 1;
	}
}
