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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Math expression evaluator.
 */
public class DoubleMathEvaluatorProvider
{
	public static CustomizableEvaluatorFactory.Builder<Double> populateMath(CustomizableEvaluatorFactory.Builder<Double> builder)
	{
		return builder
			.type(Double.class)
			.numberConverter((factory) -> Double::parseDouble)
			.unaryOperators(mathUnaryOperators())
			.binaryOperators(mathBinaryOperators())
			.functions(mathFunctions());
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> CustomizableEvaluatorFactory.Builder<?> populateAllAndBoolean(CustomizableEvaluatorFactory.Builder<T> builder)
	{
		return builder
			.type(Double.class)
			.numberConverter((factory) -> s -> (T)(Object) Double.parseDouble(s))
			.unaryOperators(Stream.concat(
						((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) mathUnaryOperators()).entrySet().stream(),
						((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) BooleanEvaluatorProvider.booleanUnaryOperators()).entrySet().stream()
					)
					.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(a, b) -> {
							if (a.getValue().equals(b.getValue())) {
								throw new IllegalArgumentException("Different priorities for operator");
							}
							return Maps.immutableEntry(
								(args) -> (args.stream().allMatch(x -> x.getType() == Double.class) ? a.getKey() : b.getKey()).apply(args),
								a.getValue()
							);
						}
					))
			)
			.binaryOperators(Stream.concat(Stream.concat(
							((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) mathBinaryOperators()).entrySet().stream(),
							((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) cmpBinaryOperators()).entrySet().stream()),
						((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) BooleanEvaluatorProvider.booleanBinaryOperators()).entrySet().stream()
					)
					.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(a, b) -> {
							if (!a.getValue().equals(b.getValue()))
								throw new IllegalArgumentException("Different priorities for operator");
							return Maps.immutableEntry(
								(args) -> (args.stream().allMatch(x -> x.getType() == Double.class) ? a.getKey() : b.getKey()).apply(args),
								a.getValue()
							);
						}
					))
			)
			.functions(Stream.concat(
						((Map<String, Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>>) (Object) crossFunctions()).entrySet().stream(),
						((Map<String, Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>>) (Object) BooleanEvaluatorProvider.crossFunctions()).entrySet().stream()
					)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
			);
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>>
	mathUnaryOperators()
	{
		return MATH_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>>
	mathBinaryOperators()
	{
		return MATH_BINARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>>
	cmpBinaryOperators()
	{
		return CMP_BINARY_OPERATORS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>>
	mathFunctions()
	{
		return MATH_FUNCTIONS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>>
	crossFunctions()
	{
		return CROSS_FUNCTIONS;
	}


	// Unary operators:

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> UN_PLUS =
		Maps.immutableEntry("+", Maps.immutableEntry(UnaryPlusOperator::new, 5));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> UN_MINUS =
		Maps.immutableEntry("-", Maps.immutableEntry(UnaryMinusOperator::new, 5));

	// Binary operators:

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_PLUS =
		Maps.immutableEntry("+", Maps.immutableEntry(BinPlusOperator::new, 10));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_MINUS =
		Maps.immutableEntry("-", Maps.immutableEntry(BinMinusOperator::new, 10));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_MULTIPLY =
		Maps.immutableEntry("*", Maps.immutableEntry(BinMultiplyOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_DIVIDE =
		Maps.immutableEntry("/", Maps.immutableEntry(BinDivideOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_MODULO =
		Maps.immutableEntry("%", Maps.immutableEntry(BinModuloOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> BIN_POWER =
		Maps.immutableEntry("*^", Maps.immutableEntry(BinPowerOperator::new, 6));

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_EQUAL =
		Maps.immutableEntry("==", Maps.immutableEntry(EqualOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_UNEQUAL =
		Maps.immutableEntry("!=", Maps.immutableEntry(UnequalOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_GREATERTHAN =
		Maps.immutableEntry(">", Maps.immutableEntry(GreaterThanOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_GREATEREQUAL =
		Maps.immutableEntry(">=", Maps.immutableEntry(GreaterEqualOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_LOWERTHAN =
		Maps.immutableEntry("<", Maps.immutableEntry(LowerThanOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_LOWEREQUAL =
		Maps.immutableEntry("<=", Maps.immutableEntry(LowerEqualOperator::new, 13));

	// Functions:

	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> NULLFOR_FUNCTION =
		Maps.immutableEntry("nullfor", NullForFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> IFNULL_FUNCTION =
		Maps.immutableEntry("ifnull", IfnullFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>> ISNULL_FUNCTION =
		Maps.immutableEntry("isnull", IsNullFunction::new);

	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_PI =
		Maps.immutableEntry("pi", PiConstant::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_E =
		Maps.immutableEntry("e", EConstant::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_RADIANS =
		Maps.immutableEntry("radians", RadiansFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_DEGREES =
		Maps.immutableEntry("degrees", DegreesFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_SIN =
		Maps.immutableEntry("sin", SinFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_COS =
		Maps.immutableEntry("cos", CosFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_TAN =
		Maps.immutableEntry("tan", TanFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_COTAN =
		Maps.immutableEntry("cotan", CotanFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ASIN =
		Maps.immutableEntry("asin", AsinFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ACOS =
		Maps.immutableEntry("acos", AcosFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ATAN =
		Maps.immutableEntry("atan", AtanFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ACOTAN =
		Maps.immutableEntry("acotan", AcotanFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ATAN2 =
		Maps.immutableEntry("atan2", Atan2Function::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_POW =
		Maps.immutableEntry("abs", AbsFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_ABS =
		Maps.immutableEntry("pow", PowFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_LN =
		Maps.immutableEntry("ln", LnFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_LOG10 =
		Maps.immutableEntry("log10", Log10Function::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_LOG2 =
		Maps.immutableEntry("log2", Log2Function::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> FUN_EXP =
		Maps.immutableEntry("exp", ExpFunction::new);


	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> MATH_UNARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>>builder()
		.put(UN_PLUS)
		.put(UN_MINUS)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>> MATH_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>, Integer>>builder()
		.put(BIN_PLUS)
		.put(BIN_MINUS)
		.put(BIN_MULTIPLY)
		.put(BIN_DIVIDE)
		.put(BIN_MODULO)
		.put(BIN_POWER)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>> CMP_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Boolean>>, Integer>>builder()
		.put(BIN_EQUAL)
		.put(BIN_UNEQUAL)
		.put(BIN_GREATERTHAN)
		.put(BIN_GREATEREQUAL)
		.put(BIN_LOWERTHAN)
		.put(BIN_LOWEREQUAL)
		.build();

	public static final Map<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> MATH_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>>builder()
		.put(FUN_PI)
		.put(FUN_E)
		.put(FUN_EXP)
		.put(FUN_LN)
		.put(FUN_LOG10)
		.put(FUN_LOG2)
		.put(FUN_ABS)
		.put(FUN_POW)
		.put(FUN_RADIANS)
		.put(FUN_DEGREES)
		.put(FUN_SIN)
		.put(FUN_COS)
		.put(FUN_TAN)
		.put(FUN_COTAN)
		.put(FUN_ASIN)
		.put(FUN_ACOS)
		.put(FUN_ATAN)
		.put(FUN_ATAN2)
		.put(FUN_ACOTAN)
		.put(NULLFOR_FUNCTION)
		.put(IFNULL_FUNCTION)
		.build();

	@SuppressWarnings("unchecked")
	public static final Map<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>> CROSS_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>>builder()
		.putAll(MATH_FUNCTIONS)
		.put((Map.Entry<? extends String, ? extends Function<List<EvaluatorFactory.Expression<Double>>, EvaluatorFactory.Expression<Double>>>) (Object)
			ISNULL_FUNCTION)
		.build();

	public static abstract class DoubleTypedExpression implements EvaluatorFactory.Expression<Double>
	{
		@Override
		public Type getType()
		{
			return Double.class;
		}
	}

	public static abstract class NullaryDoubleExpression extends DoubleTypedExpression
	{
		public NullaryDoubleExpression(List<EvaluatorFactory.Expression<Double>> args)
		{
			Preconditions.checkArgument(args.size() == 0, "Need zero arguments for nullary operator");
		}

		@Override
		public Double evaluate(EvaluatorFactory.Context<Double> context)
		{
			return evaluateValid();
		}

		protected abstract Double evaluateValid();
	}

	public static abstract class UnaryDoubleExpression extends DoubleTypedExpression
	{
		protected final EvaluatorFactory.Expression<Double> underlying;

		public UnaryDoubleExpression(List<EvaluatorFactory.Expression<Double>> args)
		{
			Preconditions.checkArgument(args.size() == 1, "Need one arguments for unary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Double.class, "Need Double type for arguments"));
			this.underlying = args.get(0);
		}

		@Override
		public Double evaluate(EvaluatorFactory.Context<Double> context)
		{
			Double value = underlying.evaluate(context);
			if (value == null)
				return null;
			return evaluateValid(value);
		}

		protected abstract Double evaluateValid(Double value);
	}

	public static abstract class BinaryDoubleExpression extends DoubleTypedExpression
	{
		protected final EvaluatorFactory.Expression<Double> left;
		protected final EvaluatorFactory.Expression<Double> right;

		public BinaryDoubleExpression(List<EvaluatorFactory.Expression<Double>> args)
		{
			Preconditions.checkArgument(args.size() == 2, "Need two arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Double.class, "Need Long type for arguments"));
			this.left = args.get(0);
			this.right = args.get(1);
		}

		@Override
		public Double evaluate(EvaluatorFactory.Context<Double> context)
		{
			Double leftValue = left.evaluate(context);
			Double rightValue = right.evaluate(context);
			if (leftValue == null || rightValue == null)
				return null;
			return evaluateValid(leftValue, rightValue);
		}

		protected abstract Double evaluateValid(Double left, Double right);
	}

	public static abstract class BooleanUnaryDoubleExpression implements EvaluatorFactory.Expression<Boolean>
	{
		protected final EvaluatorFactory.Expression<Double> underlying;

		public BooleanUnaryDoubleExpression(List<EvaluatorFactory.Expression<Double>> args)
		{
			Preconditions.checkArgument(args.size() == 1, "Need one arguments for unary operation");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Double.class, "Need Double type for arguments"));
			this.underlying = args.get(0);
		}

		@Override
		public Type getType()
		{
			return Boolean.class;
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			@SuppressWarnings("unchecked")
			Double value = underlying.evaluate((EvaluatorFactory.Context<Double>) (Object) context);
			if (value == null)
				return null;
			return evaluateValid(value);
		}

		protected abstract Boolean evaluateValid(Double value);
	}

	public static abstract class BooleanBinaryDoubleExpression implements EvaluatorFactory.Expression<Boolean>
	{
		protected final EvaluatorFactory.Expression<Double> left;
		protected final EvaluatorFactory.Expression<Double> right;

		public BooleanBinaryDoubleExpression(List<EvaluatorFactory.Expression<Double>> args)
		{
			Preconditions.checkArgument(args.size() == 2, "Need two arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Double.class, "Need Double type for arguments"));
			this.left = args.get(0);
			this.right = args.get(1);
		}

		@Override
		public Type getType()
		{
			return Boolean.class;
		}

		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			@SuppressWarnings("unchecked")
			Double leftValue = left.evaluate((EvaluatorFactory.Context<Double>) (Object) context);
			@SuppressWarnings("unchecked")
			Double rightValue = right.evaluate((EvaluatorFactory.Context<Double>) (Object) context);
			if (leftValue == null || rightValue == null)
				return null;
			return evaluateValid(leftValue, rightValue);
		}

		protected abstract Boolean evaluateValid(Double left, Double right);
	}


	// Operators and functions classes (anonymous classes would eliminate needs for constructors but then we would
	// lose nice debugging functionality):
	public static class UnaryPlusOperator extends UnaryDoubleExpression
	{
		public UnaryPlusOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return value;
		}
	}

	public static class UnaryMinusOperator extends UnaryDoubleExpression
	{
		public UnaryMinusOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return -value;
		}
	}

	// Binary operators:

	public static class BinPlusOperator extends BinaryDoubleExpression
	{
		public BinPlusOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left+right;
		}
	}

	public static class BinMinusOperator extends BinaryDoubleExpression
	{
		public BinMinusOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left-right;
		}
	}

	public static class BinMultiplyOperator extends BinaryDoubleExpression
	{
		public BinMultiplyOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left*right;
		}
	}

	public static class BinDivideOperator extends BinaryDoubleExpression
	{
		public BinDivideOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left/right;
		}
	}

	public static class BinModuloOperator extends BinaryDoubleExpression
	{
		public BinModuloOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left%right;
		}
	}

	public static class BinPowerOperator extends BinaryDoubleExpression
	{
		public BinPowerOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return Math.pow(left, right);
		}
	}

	public static class EqualOperator extends BooleanBinaryDoubleExpression
	{
		public EqualOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return left.equals(right);
		}
	}

	public static class UnequalOperator extends BooleanBinaryDoubleExpression
	{
		public UnequalOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return !left.equals(right);
		}
	}

	public static class GreaterThanOperator extends BooleanBinaryDoubleExpression
	{
		public GreaterThanOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return left > right;
		}
	}

	public static class GreaterEqualOperator extends BooleanBinaryDoubleExpression
	{
		public GreaterEqualOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return left >= right;
		}
	}

	public static class LowerThanOperator extends BooleanBinaryDoubleExpression
	{
		public LowerThanOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return left < right;
		}
	}

	public static class LowerEqualOperator extends BooleanBinaryDoubleExpression
	{
		public LowerEqualOperator(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Double left, Double right)
		{
			return left <= right;
		}
	}

	public static class NullForFunction extends BinaryDoubleExpression
	{
		public NullForFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return left.equals(right) ? null : left;
		}
	}

	public static class IfnullFunction extends BinaryDoubleExpression
	{
		public IfnullFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Double evaluate(EvaluatorFactory.Context<Double> context)
		{
			return Optional.ofNullable(left.evaluate(context))
				.orElseGet(() -> right.evaluate(context));
		}

		@Override
		protected Double evaluateValid(Double left, Double right)
		{
			throw new UnsupportedOperationException("unreachable");
		}
	}

	public static class IsNullFunction extends BooleanUnaryDoubleExpression
	{
		public IsNullFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			return underlying.evaluate((EvaluatorFactory.Context<Double>) (Object) context) == null;
		}

		@Override
		protected Boolean evaluateValid(Double value)
		{
			throw new UnsupportedOperationException("unreachable");
		}
	}

	public static class PiConstant extends NullaryDoubleExpression
	{
		public PiConstant(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid()
		{
			return Math.PI;
		}
	}

	public static class EConstant extends NullaryDoubleExpression
	{
		public EConstant(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid()
		{
			return Math.E;
		}
	}

	public static class RadiansFunction extends UnaryDoubleExpression
	{
		public RadiansFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.toRadians(value);
		}
	}

	public static class DegreesFunction extends UnaryDoubleExpression
	{
		public DegreesFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.toDegrees(value);
		}
	}

	public static class SinFunction extends UnaryDoubleExpression
	{
		public SinFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.sin(value);
		}
	}

	public static class CosFunction extends UnaryDoubleExpression
	{
		public CosFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.cos(value);
		}
	}

	public static class TanFunction extends UnaryDoubleExpression
	{
		public TanFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.tan(value);
		}
	}

	public static class CotanFunction extends UnaryDoubleExpression
	{
		public CotanFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return 1.0/Math.tan(value);
		}
	}

	public static class AsinFunction extends UnaryDoubleExpression
	{
		public AsinFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.asin(value);
		}
	}

	public static class AcosFunction extends UnaryDoubleExpression
	{
		public AcosFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.acos(value);
		}
	}


	public static class AtanFunction extends UnaryDoubleExpression
	{
		public AtanFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.atan(value);
		}
	}


	public static class AcotanFunction extends UnaryDoubleExpression
	{
		public AcotanFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.atan(1.0/value);
		}
	}


	public static class Atan2Function extends BinaryDoubleExpression
	{
		public Atan2Function(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return Math.atan2(left, right);
		}
	}

	public static class AbsFunction extends UnaryDoubleExpression
	{
		public AbsFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.abs(value);
		}
	}

	public static class PowFunction extends BinaryDoubleExpression
	{
		public PowFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double left, Double right)
		{
			return Math.pow(left, right);
		}
	}

	public static class LnFunction extends UnaryDoubleExpression
	{
		public LnFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.log(value);
		}
	}

	public static class Log10Function extends UnaryDoubleExpression
	{
		public Log10Function(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.log10(value);
		}
	}

	public static class Log2Function extends UnaryDoubleExpression
	{
		public Log2Function(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.log(value)/Math.log(2);
		}
	}

	public static class ExpFunction extends UnaryDoubleExpression
	{
		public ExpFunction(List<EvaluatorFactory.Expression<Double>> args)
		{
			super(args);
		}

		@Override
		public Double evaluateValid(Double value)
		{
			return Math.exp(value);
		}
	}
}
