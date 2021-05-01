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
 * Long integer expression evaluator.
 */
public class StandardLongEvaluatorProvider
{
	public static CustomizableEvaluatorFactory.Builder<Long> populateMath(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(mathUnaryOperators())
			.binaryOperators(mathBinaryOperators())
			.functions(commonFunctions());
	}

	public static CustomizableEvaluatorFactory.Builder<Long> populateBit(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(bitUnaryOperators())
			.binaryOperators(bitBinaryOperators())
			.functions(crossFunctions());
	}

	public static CustomizableEvaluatorFactory.Builder<Long> populateBitAndCalc(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(bitUnaryOperators())
			.binaryOperators(bitBinaryOperators())
			.functions(crossFunctions());
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> CustomizableEvaluatorFactory.Builder<?> populateAllAndBoolean(CustomizableEvaluatorFactory.Builder<T> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(Stream.concat(
					((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) bitAndCalcUnaryOperators()).entrySet().stream(),
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
							(args) -> (args.stream().allMatch(x -> x.getType() == Long.class) ? a.getKey() : b.getKey()).apply(args),
							a.getValue()
						);
					}
				))
			)
			.binaryOperators(Stream.concat(Stream.concat(
					((Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<T>>, EvaluatorFactory.Expression<T>>, Integer>>) (Object) bitAndCalcBinaryOperators()).entrySet().stream(),
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
							(args) -> (args.stream().allMatch(x -> x.getType() == Long.class) ? a.getKey() : b.getKey()).apply(args),
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

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> UN_PLUS =
		Maps.immutableEntry("+", Maps.immutableEntry(UnaryPlusOperator::new, 5));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> UN_MINUS =
		Maps.immutableEntry("-", Maps.immutableEntry(UnaryMinusOperator::new, 5));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> UN_TILDE =
		Maps.immutableEntry("~", Maps.immutableEntry(UnaryTildeOperator::new, 5));

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_PLUS =
		Maps.immutableEntry("+", Maps.immutableEntry(BinPlusOperator::new, 10));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_MINUS =
		Maps.immutableEntry("-", Maps.immutableEntry(BinMinusOperator::new, 10));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_MULTIPLY =
		Maps.immutableEntry("*", Maps.immutableEntry(BinMultiplyOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_DIVIDE =
		Maps.immutableEntry("/", Maps.immutableEntry(BinDivideOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_MODULO =
		Maps.immutableEntry("%", Maps.immutableEntry(BinModuloOperator::new, 8));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_POWER =
		Maps.immutableEntry("*^", Maps.immutableEntry(BinPowerOperator::new, 7));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_AND =
		Maps.immutableEntry("&", Maps.immutableEntry(BinAndOperator::new, 11));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_XOR =
		Maps.immutableEntry("^", Maps.immutableEntry(BinXorOperator::new, 12));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_OR =
		Maps.immutableEntry("|", Maps.immutableEntry(BinOrOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_LSHIFT =
		Maps.immutableEntry("<<", Maps.immutableEntry(BinLshiftOperator::new, 4));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_RSHIFT =
		Maps.immutableEntry(">>", Maps.immutableEntry(BinRshiftOperator::new, 4));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIN_RUSHIFT =
		Maps.immutableEntry(">>>", Maps.immutableEntry(BinRushiftOperator::new, 4));

	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_EQUAL =
		Maps.immutableEntry("==", Maps.immutableEntry(EqualOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_UNEQUAL =
		Maps.immutableEntry("!=", Maps.immutableEntry(UnequalOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_GREATERTHAN =
		Maps.immutableEntry(">", Maps.immutableEntry(GreaterThanOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_GREATEREQUAL =
		Maps.immutableEntry(">=", Maps.immutableEntry(GreaterEqualOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_LOWERTHAN =
		Maps.immutableEntry("<", Maps.immutableEntry(LowerThanOperator::new, 13));
	public static final Map.Entry<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> BIN_LOWEREQUAL =
		Maps.immutableEntry("<=", Maps.immutableEntry(LowerEqualOperator::new, 13));

	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> NULLFOR_FUNCTION =
		Maps.immutableEntry("nullfor", NullforFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> IFNULL_FUNCTION =
		Maps.immutableEntry("ifnull", IfnullFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>> ISNULL_FUNCTION =
		Maps.immutableEntry("isnull", IsNullFunction::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> SWAP16_FUNCTION =
		Maps.immutableEntry("swap16", Swap16Function::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> SWAP32_FUNCTION =
		Maps.immutableEntry("swap32", Swap32Function::new);
	public static final Map.Entry<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> SWAP64_FUNCTION =
		Maps.immutableEntry("swap64", Swap64Function::new);

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> COMMON_UNARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.put(UN_PLUS)
		.put(UN_MINUS)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_UNARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.put(UN_TILDE)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> MATH_UNARY_OPERATORS = COMMON_UNARY_OPERATORS;

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_AND_CALC_UNARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.putAll(COMMON_UNARY_OPERATORS)
		.putAll(BIT_UNARY_OPERATORS)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> COMMON_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.put(BIN_PLUS)
		.put(BIN_MINUS)
		.put(BIN_MULTIPLY)
		.put(BIN_DIVIDE)
		.put(BIN_MODULO)
		.put(BIN_POWER)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.put(BIN_AND)
		.put(BIN_XOR)
		.put(BIN_OR)
		.put(BIN_LSHIFT)
		.put(BIN_RSHIFT)
		.put(BIN_RUSHIFT)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>> CMP_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>>builder()
		.put(BIN_EQUAL)
		.put(BIN_UNEQUAL)
		.put(BIN_GREATERTHAN)
		.put(BIN_GREATEREQUAL)
		.put(BIN_LOWERTHAN)
		.put(BIN_LOWEREQUAL)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> MATH_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.putAll(COMMON_BINARY_OPERATORS)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_AND_CALC_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.putAll(COMMON_BINARY_OPERATORS)
		.putAll(BIT_BINARY_OPERATORS)
		.build();

	@SuppressWarnings("unchecked")
	public static final Map<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> COMMON_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>>builder()
		.put(NULLFOR_FUNCTION)
		.put(IFNULL_FUNCTION)
		.put(SWAP16_FUNCTION)
		.put(SWAP32_FUNCTION)
		.put(SWAP64_FUNCTION)
		.build();

	@SuppressWarnings("unchecked")
	public static final Map<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>> CROSS_FUNCTIONS = ImmutableMap.<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>>builder()
		.putAll(COMMON_FUNCTIONS)
		.put((Map.Entry<? extends String, ? extends Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>>) (Object)
			ISNULL_FUNCTION)
		.build();

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	commonUnaryOperators()
	{
		return COMMON_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	commonBinaryOperators()
	{
		return COMMON_BINARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	bitUnaryOperators()
	{
		return BIT_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	bitBinaryOperators()
	{
		return BIT_BINARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	mathUnaryOperators()
	{
		return MATH_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	mathBinaryOperators()
	{
		return MATH_BINARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	bitAndCalcUnaryOperators()
	{
		return BIT_AND_CALC_UNARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>
	bitAndCalcBinaryOperators()
	{
		return BIT_AND_CALC_BINARY_OPERATORS;
	}

	public static Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Boolean>>, Integer>>
	cmpBinaryOperators()
	{
		return CMP_BINARY_OPERATORS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>>
	commonFunctions()
	{
		return COMMON_FUNCTIONS;
	}

	public static Map<String, Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>>
	crossFunctions()
	{
		return CROSS_FUNCTIONS;
	}

	public static abstract class LongTypedExpression implements EvaluatorFactory.Expression<Long>
	{
		@Override
		public Type getType()
		{
			return Long.class;
		}
	}

	public static abstract class UnaryLongExpression extends LongTypedExpression
	{
		protected final EvaluatorFactory.Expression<Long> underlying;

		public UnaryLongExpression(List<EvaluatorFactory.Expression<Long>> args)
		{
			Preconditions.checkArgument(args.size() == 1, "Need one arguments for unary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Long.class, "Need Long type for arguments"));
			this.underlying = args.get(0);
		}

		@Override
		public Long evaluate(EvaluatorFactory.Context<Long> context)
		{
			Long value = underlying.evaluate(context);
			if (value == null)
				return null;
			return evaluateValid(value);
		}

		protected abstract Long evaluateValid(Long value);
	}

	public static abstract class BinaryLongExpression extends LongTypedExpression
	{
		protected final EvaluatorFactory.Expression<Long> left;
		protected final EvaluatorFactory.Expression<Long> right;

		public BinaryLongExpression(List<EvaluatorFactory.Expression<Long>> args)
		{
			Preconditions.checkArgument(args.size() == 2, "Need two arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Long.class, "Need Long type for arguments"));
			this.left = args.get(0);
			this.right = args.get(1);
		}

		@Override
		public Long evaluate(EvaluatorFactory.Context<Long> context)
		{
			Long leftValue = left.evaluate(context);
			Long rightValue = right.evaluate(context);
			if (leftValue == null || rightValue == null)
				return null;
			return evaluateValid(leftValue, rightValue);
		}

		protected abstract Long evaluateValid(Long left, Long right);
	}

	public static abstract class BooleanUnaryLongExpression implements EvaluatorFactory.Expression<Boolean>
	{
		protected final EvaluatorFactory.Expression<Long> underlying;

		public BooleanUnaryLongExpression(List<EvaluatorFactory.Expression<Long>> args)
		{
			Preconditions.checkArgument(args.size() == 1, "Need one arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Long.class, "Need Long type for arguments"));
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
			Long value = underlying.evaluate((EvaluatorFactory.Context<Long>) (Object) context);
			if (value == null)
				return null;
			return evaluateValid(value);
		}

		protected abstract Boolean evaluateValid(Long value);
	}

	public static abstract class BooleanBinaryLongExpression implements EvaluatorFactory.Expression<Boolean>
	{
		protected final EvaluatorFactory.Expression<Long> left;
		protected final EvaluatorFactory.Expression<Long> right;

		public BooleanBinaryLongExpression(List<EvaluatorFactory.Expression<Long>> args)
		{
			Preconditions.checkArgument(args.size() == 2, "Need two arguments for binary operator");
			args.forEach(arg -> Preconditions.checkArgument(arg.getType() == Long.class, "Need Long type for arguments"));
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
			Long leftValue = left.evaluate((EvaluatorFactory.Context<Long>) (Object) context);
			@SuppressWarnings("unchecked")
			Long rightValue = right.evaluate((EvaluatorFactory.Context<Long>) (Object) context);
			if (leftValue == null || rightValue == null)
				return null;
			return evaluateValid(leftValue, rightValue);
		}

		protected abstract Boolean evaluateValid(Long left, Long right);
	}

	public static class UnaryPlusOperator extends UnaryLongExpression
	{
		public UnaryPlusOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			return value;
		}
	}

	public static class UnaryMinusOperator extends UnaryLongExpression
	{
		public UnaryMinusOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			return -value;
		}
	}

	public static class UnaryTildeOperator extends UnaryLongExpression
	{
		public UnaryTildeOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			return ~value;
		}
	}

	public static class BinPlusOperator extends BinaryLongExpression
	{
		public BinPlusOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left+right;
		}
	}

	public static class BinMinusOperator extends BinaryLongExpression
	{
		public BinMinusOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left-right;
		}
	}

	public static class BinMultiplyOperator extends BinaryLongExpression
	{
		public BinMultiplyOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left*right;
		}
	}

	public static class BinDivideOperator extends BinaryLongExpression
	{
		public BinDivideOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			if (right == 0)
				throw new ArithmeticException("Divide by zero");
			return left/right;
		}
	}

	public static class BinModuloOperator extends BinaryLongExpression
	{
		public BinModuloOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			if (right == 0)
				throw new ArithmeticException("Divide by zero");
			return left%right;
		}
	}

	public static class BinPowerOperator extends BinaryLongExpression
	{
		public BinPowerOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return (long)Math.pow(left, right);
		}
	}

	public static class BinAndOperator extends BinaryLongExpression
	{
		public BinAndOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left&right;
		}
	}

	public static class BinXorOperator extends BinaryLongExpression
	{
		public BinXorOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left^right;
		}
	}

	public static class BinOrOperator extends BinaryLongExpression
	{
		public BinOrOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left|right;
		}
	}

	public static class BinLshiftOperator extends BinaryLongExpression
	{
		public BinLshiftOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left<<right;
		}
	}

	public static class BinRshiftOperator extends BinaryLongExpression
	{
		public BinRshiftOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left >> right;
		}
	}

	public static class BinRushiftOperator extends BinaryLongExpression
	{
		public BinRushiftOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left >>> right;
		}
	}

	public static class EqualOperator extends BooleanBinaryLongExpression
	{
		public EqualOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return left.equals(right);
		}
	}

	public static class UnequalOperator extends BooleanBinaryLongExpression
	{
		public UnequalOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return !left.equals(right);
		}
	}

	public static class GreaterThanOperator extends BooleanBinaryLongExpression
	{
		public GreaterThanOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return left > right;
		}
	}

	public static class GreaterEqualOperator extends BooleanBinaryLongExpression
	{
		public GreaterEqualOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return left >= right;
		}
	}

	public static class LowerThanOperator extends BooleanBinaryLongExpression
	{
		public LowerThanOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return left < right;
		}
	}

	public static class LowerEqualOperator extends BooleanBinaryLongExpression
	{
		public LowerEqualOperator(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Boolean evaluateValid(Long left, Long right)
		{
			return left <= right;
		}
	}

	public static class NullforFunction extends BinaryLongExpression
	{
		public NullforFunction(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long left, Long right)
		{
			return left.equals(right) ? null : left;
		}
	}

	public static class IfnullFunction extends BinaryLongExpression
	{
		public IfnullFunction(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Long evaluate(EvaluatorFactory.Context<Long> context)
		{
			return Optional.ofNullable(left.evaluate(context))
				.orElseGet(() -> right.evaluate(context));
		}

		@Override
		protected Long evaluateValid(Long left, Long right)
		{
			throw new UnsupportedOperationException("unreachable");
		}
	}

	public static class IsNullFunction extends BooleanUnaryLongExpression
	{
		public IsNullFunction(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Boolean evaluate(EvaluatorFactory.Context<Boolean> context)
		{
			return underlying.evaluate((EvaluatorFactory.Context<Long>) (Object) context) == null;
		}

		@Override
		protected Boolean evaluateValid(Long value)
		{
			throw new UnsupportedOperationException("unreachable");
		}
	}

	public static class Swap16Function extends UnaryLongExpression
	{
		public Swap16Function(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			return ((value&0xff)<<8) | ((value&0xff00)>>>8);
		}
	}

	public static class Swap32Function extends UnaryLongExpression
	{
		public Swap32Function(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			long v = value;
			v = ((v>>>16)&0xffffL)|((v<<16)&0xffff0000L);
			v = ((v>>>8)&0x00ff00ffL)|((v<<8)&0xff00ff00L);
			return v;
		}
	}

	public static class Swap64Function extends UnaryLongExpression
	{
		public Swap64Function(List<EvaluatorFactory.Expression<Long>> args)
		{
			super(args);
		}

		@Override
		public Long evaluateValid(Long value)
		{
			long v = value;
			v = (v>>>32)|(v<<32);
			v = ((v>>>16)&0x0000ffff0000ffffL)|((v<<16)&0xffff0000ffff0000L);
			v = ((v>>>8)&0x00ff00ff00ff00ffL)|((v<<8)&0xff00ff00ff00ff00L);
			return v;
		}
	}
}
