package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * General key-value based Condition.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class KeyValueCondition implements Condition
{
	@Override
	public Map<String, Object> attributes()
	{
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("key", key);
		map.put("value", value);
		return map;
	}

	@Getter
	private final String key;

	@Getter
	private final String value;
}
