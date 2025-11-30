package cz.znj.kvr.sw.exp.java.dagger.di.common.impl;

import cz.znj.kvr.sw.exp.java.dagger.di.common.Facade;
import cz.znj.kvr.sw.exp.java.dagger.di.common.First;
import cz.znj.kvr.sw.exp.java.dagger.di.common.Second;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.PostConstruct;
import jakarta.inject.Inject;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Getter
public class FacadeImpl implements Facade
{
	@PostConstruct
	public FacadeImpl init()
	{
		initialized = true;
		return this;
	}

	private final First first;

	private final Second second;

	private boolean initialized = false;
}
