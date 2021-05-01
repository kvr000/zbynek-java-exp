package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator;

/**
 *
 */
public class NullContext<T> implements EvaluatorFactory.Context<T>
{
	@Override
	public T getVariable(String name)
	{
		return null;
	}
}
