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
public class StandardLongEvaluatorProvider
{
	public static CustomizableEvaluatorFactory.Builder<Long> populateMath(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(mathUnaryOperators())
			.binaryOperators(mathBinaryOperators());
	}

	public static CustomizableEvaluatorFactory.Builder<Long> populateBit(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(bitUnaryOperators())
			.binaryOperators(bitBinaryOperators());
	}

	public static CustomizableEvaluatorFactory.Builder<Long> populateBitAndCalc(CustomizableEvaluatorFactory.Builder<Long> builder)
	{
		return builder
			.type(Long.class)
			.unaryOperators(bitUnaryOperators())
			.binaryOperators(bitBinaryOperators());
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
		Maps.immutableEntry("^", Maps.immutableEntry(BinPowerOperator::new, 7));
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
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.put(BIN_AND)
		.put(BIN_XOR)
		.put(BIN_OR)
		.put(BIN_LSHIFT)
		.put(BIN_RSHIFT)
		.put(BIN_RUSHIFT)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> MATH_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.putAll(COMMON_BINARY_OPERATORS)
		.put(BIN_POWER)
		.build();

	public static final Map<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>> BIT_AND_CALC_BINARY_OPERATORS = ImmutableMap.<String, Map.Entry<Function<List<EvaluatorFactory.Expression<Long>>, EvaluatorFactory.Expression<Long>>, Integer>>builder()
		.putAll(COMMON_BINARY_OPERATORS)
		.putAll(BIT_BINARY_OPERATORS)
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
}
