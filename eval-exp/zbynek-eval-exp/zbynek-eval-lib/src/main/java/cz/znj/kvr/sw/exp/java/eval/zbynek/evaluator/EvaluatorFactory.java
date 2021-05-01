package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator;

import java.lang.reflect.Type;


/**
 * Math expression evaluator.
 */
public interface EvaluatorFactory<T>
{
	interface Expression<T>
	{
		/**
		 * Gets expression type.  Type is known at compile time.
		 *
		 * @return
		 * 	type of expression result.
		 */
		Type getType();

		/**
		 * Evaluates expression in runtime context.
		 *
		 * @param parameters
		 * 	runtime context
		 *
		 * @return
		 * 	expression result
		 */
		T evaluate(Context<T> parameters);
	}

	interface Context<T>
	{
		T getVariable(String name);
	}

	Expression<T> parse(String expression);
}
