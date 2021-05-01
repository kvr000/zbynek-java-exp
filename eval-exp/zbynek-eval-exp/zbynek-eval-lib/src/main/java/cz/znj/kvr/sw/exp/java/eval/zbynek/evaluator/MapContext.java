package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator;

import lombok.RequiredArgsConstructor;

import java.util.Map;


/**
 * Context implementation referring to provided Map.
 */
@RequiredArgsConstructor
public class MapContext<T> implements EvaluatorFactory.Context<T>
{
	private final Map<String, T> variables;

	@Override
	public T getVariable(String name)
	{
		return variables.get(name);
	}
}
