package cz.znj.kvr.sw.exp.java.guice.di.common.impl;

import cz.znj.kvr.sw.exp.java.guice.di.common.Facade;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.PostConstruct;

/**
 * @author
 * 	Zbynek Vyskovsky
 */
@Getter
@Setter
@Accessors(chain = true)
public class FacadeImpl implements Facade {
	@PostConstruct
	public FacadeImpl init()
	{
		return this;
	}

	private First first;

	private Second second;
}
