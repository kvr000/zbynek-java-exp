package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container;

import com.google.inject.Injector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;


@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class InjectorContainerContext extends AbstractContainerContext
{
	@Getter
	private final Injector injector;
}
