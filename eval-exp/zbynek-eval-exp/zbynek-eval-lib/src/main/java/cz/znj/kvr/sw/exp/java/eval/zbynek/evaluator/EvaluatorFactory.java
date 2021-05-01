/*
 * Copyright 2015 Zbynek Vyskovsky mailto:kvr000@gmail.com http://github.com/kvr000/zbynek-java/exp/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
