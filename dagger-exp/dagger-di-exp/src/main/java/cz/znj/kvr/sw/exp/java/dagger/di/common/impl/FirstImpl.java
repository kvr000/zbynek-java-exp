package cz.znj.kvr.sw.exp.java.dagger.di.common.impl;

import cz.znj.kvr.sw.exp.java.dagger.di.common.First;
import lombok.AllArgsConstructor;

import jakarta.inject.Inject;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
@AllArgsConstructor(onConstructor = @__(@Inject))
public class FirstImpl implements First {
	@Override
	public int getFirstValue() {
		return 1;
	}
}
