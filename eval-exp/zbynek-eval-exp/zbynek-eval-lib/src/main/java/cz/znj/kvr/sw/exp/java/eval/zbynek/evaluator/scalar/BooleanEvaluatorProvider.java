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

package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.scalar;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.CustomizableEvaluatorFactory;
import cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator.EvaluatorFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * Long integer expression evaluator.
 */
public class BooleanEvaluatorProvider
{
	@SuppressWarnings("unchecked")
	public static CustomizableEvaluatorFactory.Builder<Boolean> populateBoolean(CustomizableEvaluatorFactory.Builder<Boolean> builder)
	{
		return builder
			.type(Boolean.class)
			.unaryOperators(booleanUnaryOperators())
			.binaryOperators(booleanBinaryOperators())
			.functions((Map<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>>)(Object) crossFunctions());
	}

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> UN_NOT =
		Maps.immutableEntry("!", Maps.immutableEntry(UnaryNotOperator::new, 5));

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_AND =
		Maps.immutableEntry("&&", Maps.immutableEntry(BinAndOperator::new, 11));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_XOR =
		Maps.immutableEntry("^^", Maps.immutableEntry(BinXorOperator::new, 12));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_OR =
		Maps.immutableEntry("||", Maps.immutableEntry(BinOrOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_EQUAL =
		Maps.immutableEntry("==", Maps.immutableEntry(EqualOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_UNEQUAL =
		Maps.immutableEntry("!=", Maps.immutableEntry(UnequalOperator::new, 13));

	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>> FALSE_FUNCTION =
		Maps.immutableEntry("false", FalseFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>> TRUE_FUNCTION =
		Maps.immutableEntry("true", TrueFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<?>>, EvaluatorFactory.Expression<?>>> IFELSE_FUNCTION =
		Maps.immutableEntry("ifelse", IfelseFunction::new);

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BOOLEAN_UNARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>>builder()
		.put(UN_NOT)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BOOLEAN_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>>builder()
		.put(BIN_AND)
		.put(BIN_OR)
		.put(BIN_XOR)
		.put(BIN_EQUAL)
		.put(BIN_UNEQUAL)
		.build();

	public static final Map<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>> BOOLEAN_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>>builder()
		.put(FALSE_FUNCTION)
		.put(TRUE_FUNCTION)
		.build();

	@SuppressWarnings("unchecked")
	public static final Map<String, Function<List<EvaluatorFactory.Expression<?>>, EvaluatorFactory.Expression<?>>> CROSS_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<?>>, EvaluatorFactory.Expression<?>>>builder()
		.putAll((Map<? extends String, ? extends Function<List<EvaluatorFactory.Expression<?>>,
			EvaluatorFactory.Expression<?>>>)(Object) BOOLEAN_FUNCTIONS)
		.put(IFELSE_FUNCTION)
		.build();

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>>
	booleanUnaryOperators()
	{
		return BOOLEAN_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>, Integer>>
	booleanBinaryOperators()
	{
		return BOOLEAN_BINARY_OPERATORS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<Boolean>>, EvaluatorFactory.Expression<Boolean>>>
	booleanFunctions()
	{
		return BOOLEAN_FUNCTIONS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<?>>, EvaluatorFactory.Expression<?>>>
	crossFunctions()
	{
		return CROSS_FUNCTIONS;
	}

	public static abstract class BooleanTypedExpression implements EvaluatorFactory.Expression<Boolean>
	{
		@Override
		public Type getType()
		{
			return Boolean.class;
		}
	}

	public static abstract class UnaryBooleanExpression extends BooleanTypedExpression
	{
		protected final EvaluatorFactory.Expression<Boolean> underlying;

		public UnaryBooleanExpression(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			Preconditions.checkArgument(args.size() == 1, "Need one arguments for unary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Boolean.class, "Need Boolean type for arguments"));
			this.underlying = args.get(0);
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			Boolean value = underlying.evaluate(context);
			if (value == null)
				return null;
			return evaluateValid(value);
		}

		protected abstract Boolean evaluateValid(Boolean value);
	}

	public static abstract class BinaryBooleanExpression extends BooleanTypedExpression
	{
		protected final EvaluatorFactory.Expression<Boolean> left;
		protected final EvaluatorFactory.Expression<Boolean> right;

		public BinaryBooleanExpression(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			Preconditions.checkArgument(args.size() == 2, "Need two arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Boolean.class, "Need Boolean type for arguments"));
			this.left = args.get(0);
			this.right = args.get(1);
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			Boolean leftValue = left.evaluate(context);
			Boolean rightValue = right.evaluate(context);
			if (leftValue == null || rightValue == null)
				return null;
			return evaluateValid(leftValue, rightValue);
		}

		protected abstract Boolean evaluateValid(Boolean left, Boolean right);
	}

	public static abstract class NullaryBooleanExpression extends BooleanTypedExpression
	{
		public NullaryBooleanExpression(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			Preconditions.checkArgument(args.size() == 0, "Need zero arguments for nullary function");
		}
	}

	public static class UnaryNotOperator extends UnaryBooleanExpression
	{
		public UnaryNotOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean value)
		{
			return !value;
		}
	}

	public static class BinAndOperator extends BinaryBooleanExpression
	{
		public BinAndOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean left, Boolean right)
		{
			return left && right;
		}
	}

	public static class BinXorOperator extends BinaryBooleanExpression
	{
		public BinXorOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean left, Boolean right)
		{
			return left != right;
		}
	}

	public static class BinOrOperator extends BinaryBooleanExpression
	{
		public BinOrOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean left, Boolean right)
		{
			return left || right;
		}
	}

	public static class EqualOperator extends BinaryBooleanExpression
	{
		public EqualOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean left, Boolean right)
		{
			return left.equals(right);
		}
	}

	public static class UnequalOperator extends BinaryBooleanExpression
	{
		public UnequalOperator(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Boolean left, Boolean right)
		{
			return !left.equals(right);
		}
	}

	public static class FalseFunction extends NullaryBooleanExpression
	{
		public FalseFunction(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			return false;
		}
	}

	public static class TrueFunction extends NullaryBooleanExpression
	{
		public TrueFunction(List<EvaluatorFactory.Expression<Boolean>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			return true;
		}
	}

	public static class IfelseFunction<T> implements EvaluatorFactory.Expression<T>
	{
		private final EvaluatorFactory.Expression<Boolean> condition;

		private final EvaluatorFactory.Expression<T> left;
		private final EvaluatorFactory.Expression<T> right;

		@Override
		public Type getType()
		{
			return left.getType();
		}

		@SuppressWarnings("unchecked")
		public IfelseFunction(List<EvaluatorFactory.Expression<?>> args)
		{
			Preconditions.checkArgument(args.size() == 3, "Need 3 arguments for ifelse");
			this.condition = (EvaluatorFactory.Expression<Boolean>)args.get(0);
			this.left = (EvaluatorFactory.Expression<T>)args.get(1);
			this.right = (EvaluatorFactory.Expression<T>)args.get(2);
			Preconditions.checkArgument(this.condition.getType() == Boolean.class, "Need Boolean as first argument");
			Preconditions.checkArgument(this.left.getType() == this.right.getType(), "Need same type for second and third argument");
		}

		@SuppressWarnings("unchecked")
		@Override
		public T evaluate(EvaluatorFactory.Context<T> parameters)
		{
			return (Boolean.TRUE.equals(this.condition.evaluate((EvaluatorFactory.Context<Boolean>) parameters)) ?
				left : right).evaluate(parameters);
		}
	}
}
