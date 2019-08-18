package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Generic list based Condition.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class ListValueCondition implements Condition
{
	@Override
	public Map<String, Object> attributes()
	{
		return Collections.singletonMap("value", values);
	}

	@Getter
	private final List<String> values;
}
