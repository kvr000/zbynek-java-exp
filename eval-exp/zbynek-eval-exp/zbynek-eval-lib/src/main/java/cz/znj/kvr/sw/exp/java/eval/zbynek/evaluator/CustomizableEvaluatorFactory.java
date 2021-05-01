package cz.znj.kvr.sw.exp.java.eval.zbynek.evaluator;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Math expression evaluator.
 */
@Builder(builderClassName = "Builder")
public class CustomizableEvaluatorFactory<T> implements EvaluatorFactory<T>
{
	private final Type type;

	private final Character separator;

	private final boolean allowTrailingSeparator;

	private final BiFunction<StringIterator, ParserDesired, TokenInfo<T>> tokenParser;

	private final Predicate<StringIterator> spaceSkipper;

	private final Function<StringIterator, Expression<T>> stringParser;

	private final Function<StringIterator, Expression<T>> numberParser;

	private final Function<String, T> numberConverter;

	private final Function<StringIterator, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>> unaryOperatorParser;

	private final Function<StringIterator, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>> binaryOperatorParser;

	private final Function<StringIterator, String> nameParser;

	private final Function<String, Expression<T>> variableResolver;

	private final BiFunction<String, List<Expression<T>>, Expression<T>> functionResolver;

	private final Map<String, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>> unaryOperators;

	private final Map<String, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>> binaryOperators;

	private final Map<String, Function<List<Expression<T>>, Expression<T>>> functions;

	protected CustomizableEvaluatorFactory(Builder<T> builder)
	{
		this.type = Objects.requireNonNull(builder.type, "type");
		this.separator = builder.separator;
		this.allowTrailingSeparator = builder.allowTrailingSeparator;
		this.tokenParser = Optional.ofNullable(builder.tokenParser).map(r -> r.apply(this)).orElse(this::parseNextToken);
		this.spaceSkipper = Optional.ofNullable(builder.spaceSkipper).map(r -> r.apply(this)).orElse(this::skipSpaces);
		this.stringParser = Optional.ofNullable(builder.stringParser).map(r -> r.apply(this)).orElse(this::parseString);
		this.numberParser = Optional.ofNullable(builder.numberParser).map(r -> r.apply(this)).orElse(this::parseNumber);
		this.numberConverter = Optional.ofNullable(builder.numberConverter).map(r -> r.apply(this)).orElse(this::convertNumber);
		this.nameParser = Optional.ofNullable(builder.nameParser).map(r -> r.apply(this)).orElse(this::parseName);
		this.variableResolver = Optional.ofNullable(builder.variableResolver).map(r -> r.apply(this)).orElse(this::resolveVariableContext);
		this.functionResolver = Optional.ofNullable(builder.functionResolver).map(r -> r.apply(this)).orElse(this::resolveFunction);
		this.unaryOperatorParser = Optional.ofNullable(builder.unaryOperatorParser).map(r -> r.apply(this)).orElse(this::parseUnaryOperator);
		this.binaryOperatorParser = Optional.ofNullable(builder.binaryOperatorParser).map(r -> r.apply(this)).orElse(this::parseBinaryOperator);
		this.unaryOperators = Optional.ofNullable(builder.unaryOperators).orElseGet(Collections::emptyMap);
		this.binaryOperators = Optional.ofNullable(builder.binaryOperators).orElseGet(Collections::emptyMap);
		this.functions = Optional.ofNullable(builder.functions).orElseGet(Collections::emptyMap);
	}

	public Expression<T> parse(String expression)
	{
		StringIterator iterator = new StringIterator(expression);
		try {
			Deque<ParseElement<T>> parseStack = new ArrayDeque<>();
			ParserDesired state = ParserDesired.Start;

			parseStack.push(new ParseElement<T>().initNone().newOperands());

			{
				TokenInfo<T> tokenInfo = tokenParser.apply(iterator, state);
				switch (tokenInfo.type) {
				case Finish:
					throw new IllegalArgumentException("Got end at the beginning");

				case Separator:
					throw new IllegalArgumentException("Got separator at the beginning");

				case Operator:
					parseStack.push(new ParseElement<T>().initOperation(tokenInfo.expressionProvider, tokenInfo.priority, tokenInfo.argCount, parseStack.element().operands));
					state = ParserDesired.Start;
					break;

				case Value:
					parseStack.element().operands.add(tokenInfo.expressionProvider.apply(Collections.emptyList()));
					state = ParserDesired.Extend;
					break;

				case OpenParenthesis:
					parseStack.push(new ParseElement<T>().initParenthesis());
					state = ParserDesired.Start;
					break;

				case Function:
					parseStack.push(new ParseElement<T>().initFunction(tokenInfo.name));
					state = ParserDesired.Start;
					break;

				case CloseParenthesis:
				default:
					throw new IllegalArgumentException("Start of expression expected, got token: "+tokenInfo.type);
				}
			}

			for (; ; ) {
				TokenInfo<T> tokenInfo = tokenParser.apply(iterator, state);
				switch (tokenInfo.type) {
				case Finish:
					if (state != ParserDesired.Extend) {
						throw new IllegalArgumentException("Expected expression but got end");
					}
					else {
						ParseElement<T> last = reduceExpression(parseStack);
						switch (last.type) {
						case None:
							if (last.operands.size() != 1) {
								throw new IllegalArgumentException("Missing expression");
							}
							return last.operands.get(0);

						case OpenParenthesis:
							throw new IllegalArgumentException("Unclosed parenthesis");

						case Function:
							throw new IllegalArgumentException("Missing right parenthesis in function call");

						default:
							throw new IllegalArgumentException("Internal error: Unreachable");
						}
					}

				case Separator:
					if (state != ParserDesired.Extend) {
						throw new IllegalArgumentException("Expected start of expression, got separator");
					}
					else {
						ParseElement<T> last = reduceExpression(parseStack);
						if (last.type != ParseElement.Type.Function) {
							throw new IllegalArgumentException("Got separator outside of function");
						}
						if (last.operands.isEmpty()) {
							throw new IllegalArgumentException("Got separator before the first operand");
						}
						parseStack.push(last);
						state = ParserDesired.Start;
					}
					break;

				case Value:
					if (state != ParserDesired.Start) {
						throw new IllegalArgumentException("Expected end or continuation of expression, got new expression");
					}
					else {
						parseStack.element().operands.add(tokenInfo.expressionProvider.apply(Collections.emptyList()));
						state = ParserDesired.Extend;
					}
					break;

				case Operator:
					if (state != ParserDesired.Start) {
						for (; ; ) {
							ParseElement<T> last = parseStack.element();
							if (last.type != ParseElement.Type.Operation)
								break;
							if (tokenInfo.priority > 0 ? last.priority > tokenInfo.priority : last.priority >= -tokenInfo.priority)
								break;
							reduceOperator(last);
							parseStack.remove();
						}
					}
					parseStack.push(new ParseElement<T>().initOperation(tokenInfo.expressionProvider, tokenInfo.priority < 0 ? -tokenInfo.priority : tokenInfo.priority, tokenInfo.argCount, parseStack.element().operands));
					state = ParserDesired.Start;
					break;

				case OpenParenthesis:
					if (state != ParserDesired.Start) {
						throw new IllegalArgumentException("Expected continuation of expression but got opening parenthesis");
					}
					else {
						parseStack.push(new ParseElement<T>().initParenthesis());
						state = ParserDesired.Start;
					}
					break;

				case CloseParenthesis:
					if (state == ParserDesired.Extend) {
						ParseElement<T> last = reduceExpression(parseStack);
						if (last.type == ParseElement.Type.OpenParenthesis) {
							if (last.operands.size() != 1)
								throw new IllegalArgumentException("Internal error, expected single operand at opening parenthesis");
							parseStack.element().operands.addAll(last.operands);
						}
						else if (last.type == ParseElement.Type.Function) {
							Expression<T> function = functionResolver.apply(last.name, last.operands);
							parseStack.element().operands.add(function);
						}
						else {
							throw new IllegalArgumentException("Internal error, expected open, function or operation on stack");
						}
					}
					else {
						ParseElement<T> last = parseStack.remove();
						if (last.type == ParseElement.Type.OpenParenthesis) {
							if (last.operands.size() != 1)
								throw new IllegalArgumentException("Empty parenthesis");
							parseStack.element().operands.addAll(last.operands);
						}
						else if (last.type == ParseElement.Type.Function) {
							if (!allowTrailingSeparator && !last.operands.isEmpty()) {
								throw new IllegalArgumentException("Missing argument after separator");
							}
							Expression<T> function = functionResolver.apply(last.name, last.operands);
							parseStack.element().operands.add(function);
						}
						else {
							throw new IllegalArgumentException("Internal error, expected open, function or operation on stack");
						}
					}
					state = ParserDesired.Extend;
					break;

				case Function:
					if (state != ParserDesired.Start) {
						throw new IllegalArgumentException("Expected start of expression but got closing parenthesis");
					}
					else {
						parseStack.push(new ParseElement<T>().initFunction(tokenInfo.name));
						state = ParserDesired.Start;
					}
					break;
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid expression at "+iterator.getPos()+": "+ex.getMessage(), ex);
		}
	}

	private void reduceOperator(ParseElement<T> element)
	{
		Preconditions.checkArgument(element.type == ParseElement.Type.Operation);
		if (element.argCount > element.operands.size()) {
			throw new IllegalArgumentException("Internal error evaluating operands number");
		}
		List<Expression<T>> args = element.operands.subList(element.operands.size()-element.argCount, element.operands.size());
		Expression<T> wrapped = element.expressionProvider.apply(args);
		args.clear();
		element.operands.add(wrapped);
	}

	private ParseElement<T> reduceExpression(Deque<ParseElement<T>> parseStack)
	{
		for (;;) {
			ParseElement<T> last = parseStack.remove();
			if (last.type == ParseElement.Type.Operation) {
				reduceOperator(last);
			}
			else {
				return last;
			}
		}
	}

	private TokenInfo<T> parseNextToken(StringIterator string, ParserDesired state)
	{
		if (!spaceSkipper.test(string))
			return TokenInfo.<T>builder().type(TokenType.Finish).build();

		char c = string.peekNext();
		switch (c) {
		case '(':
			string.next();
			return TokenInfo.<T>builder().type(TokenType.OpenParenthesis).build();

		case ')':
			string.next();
			return TokenInfo.<T>builder().type(TokenType.CloseParenthesis).build();

		case '"':
			return TokenInfo.<T>builder().type(TokenType.Value)
				.expressionProvider(Optional.of(stringParser.apply(string)).map(v -> ((Function<List<Expression<T>>, Expression<T>>) (args) -> v)).get())
				.build();
		}
		if (separator != null && c == separator) {
			string.next();
			return TokenInfo.<T>builder().type(TokenType.Separator).build();
		}
		else if (Character.isDigit(c) ||
			(state == ParserDesired.Start && (c == '-' || c == '+') && Character.isDigit(string.peekNextNext()))) {
			return TokenInfo.<T>builder().type(TokenType.Value)
				.expressionProvider(Optional.of(numberParser.apply(string)).map(v -> ((Function<List<Expression<T>>, Expression<T>>) (args) -> v)).get())
				.build();
		}
		else if (Character.isUnicodeIdentifierStart(c)) {
			String name = nameParser.apply(string);
			if (skipSpaces(string) && string.peekNext() == '(') {
				string.next();
				return TokenInfo.<T>builder().type(TokenType.Function)
					.name(name)
					.build();
			}
			else {
				return TokenInfo.<T>builder().type(TokenType.Value)
					.expressionProvider(args -> variableResolver.apply(name))
					.build();
			}
		}
		else {
			Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> operator =
				state == ParserDesired.Start ? unaryOperatorParser.apply(string) :
					binaryOperatorParser.apply(string);
			return TokenInfo.<T>builder().type(TokenType.Operator)
				.argCount(state == ParserDesired.Start ? 1 : 2)
				.expressionProvider(operator.getKey())
				.priority(operator.getValue())
				.build();
		}
	}

	private boolean skipSpaces(StringIterator string)
	{
		while (string.hasNext()) {
			if (!Character.isWhitespace(string.peekNext()))
				return true;
			string.next();
		}
		return false;
	}

	private String parseName(StringIterator string)
	{
		StringBuilder output = new StringBuilder();
		output.append(string.next());
		while (string.hasNext()) {
			if (!Character.isUnicodeIdentifierPart(string.peekNext()))
				break;
			output.append(string.next());
		}
		return output.toString();
	}

	private Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> parseUnaryOperator(StringIterator string)
	{
		return parseOperator(string, unaryOperators);
	}

	private Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> parseBinaryOperator(StringIterator string)
	{
		return parseOperator(string, binaryOperators);
	}

	private Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> parseOperator(StringIterator string, Map<String, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>> operators)
	{
		Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> confirmed = null;
		string.mark();
		StringBuilder output = new StringBuilder();
		output.append(string.next());
		for (;;) {
			Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer> expression = operators.get(output.toString());
			if (expression != null) {
				confirmed = expression;
				string.mark();
			}
			if (!string.hasNext()) {
				break;
			}
			char c = string.next();
			if (Character.isWhitespace(c) || Character.isUnicodeIdentifierPart(c)) {
				break;
			}
			output.append(c);
		}
		string.revert();
		if (confirmed != null) {
			return confirmed;
		}
		throw new IllegalArgumentException("Unknown operator found in sequence: "+output);
	}

	private Expression<T> parseString(StringIterator string)
	{
		StringBuilder output = new StringBuilder();
		char quote = string.next();
		for (;;) {
			if (!string.hasNext())
				throw new IllegalArgumentException("Unfinished string");
			char c = string.next();
			if (c == quote)
				break;
			if (c == '\\') {
				if (!string.hasNext())
					throw new IllegalArgumentException("Unfinished string after \\");
				c = string.next();
			}
			output.append(c);
		}
		@SuppressWarnings("unchecked")
		ConstantExpression<T> expr = new ConstantExpression<T>(type, (T) output.toString());
		return expr;
	}

	private Expression<T> parseNumber(StringIterator string)
	{
		StringBuilder output = new StringBuilder();
		output.append(string.next());
		while (string.hasNext()) {
			char c = string.next();
			if (Character.isUnicodeIdentifierPart(c) || c == '.') {
				output.append(c);
			}
			else {
				string.previous();
				break;
			}
		}
		return new ConstantExpression<T>(type, numberConverter.apply(output.toString()));
	}

	// default integer implementation, critical to override
	@SuppressWarnings("unchecked")
	private T convertNumber(String sb)
	{
		return (T) (Object) Long.parseLong(sb);
	}

	private <T> Expression<T> resolveVariableUnsupported(String name)
	{
		throw new IllegalArgumentException("Unknown variable name: "+name);
	}

	private <T> Expression<T> resolveVariableUndefined(String name)
	{
		return new UndefinedExpression<>(type);
	}

	private <T> Expression<T> resolveVariableContext(String name)
	{
		return new VariableExpression<T>(type, name);
	}

	private Expression<T> resolveFunction(String name, List<Expression<T>> args)
	{
		return Optional.ofNullable(functions.get(name))
			.map(resolver -> resolver.apply(args))
			.orElseThrow(() -> new IllegalArgumentException("Unknown function: "+name));
	}

	enum TokenType
	{
		Finish,
		Separator,
		Value,
		Operator,
		OpenParenthesis,
		CloseParenthesis,
		Function,
	}

	enum ParserDesired
	{
		Start,
		Extend,
	}

	@lombok.Builder
	static class TokenInfo<T>
	{
		TokenType type;
		String name;
		int argCount;
		int priority;
		Function<List<Expression<T>>, Expression<T>> expressionProvider;
	}

	@RequiredArgsConstructor
	static class StringIterator
	{
		private final String string;

		@Getter
		private int pos = 0;

		private int mark = -1;

		void mark()
		{
			this.mark = pos;
		}

		void revert()
		{
			this.pos = this.mark;
		}

		char next()
		{
			return string.charAt(pos++);
		}

		char previous()
		{
			return string.charAt(--pos);
		}

		char peekNext()
		{
			return string.charAt(pos);
		}

		char peekNextNext()
		{
			return pos+1 < string.length() ? string.charAt(pos+1) : (char) -1;
		}

		boolean hasNext()
		{
			return pos < string.length();
		}
	}

	static class ParseElement<T>
	{
		enum Type {
			None,
			Operation,
			Function,
			OpenParenthesis,
		};

		Type type = Type.None;
		Function<List<Expression<T>>, Expression<T>> expressionProvider;
		String name;
		int priority;
		int argCount;
		boolean isPrefix;
		List<Expression<T>> operands;

		ParseElement<T> initNone()
		{
			type = Type.None;
			return newOperands();
		}

		ParseElement<T> initOperation(Function<List<Expression<T>>, Expression<T>> expressionProvider, int priority, int argCount, List<Expression<T>> operands)
		{
			type = Type.Operation;
			this.expressionProvider = expressionProvider;
			this.priority = priority;
			this.argCount = argCount;
			this.operands = operands;
			return this;
		}

		ParseElement<T> initParenthesis()
		{
			type = Type.OpenParenthesis;
			return newOperands();
		}

		ParseElement<T> initFunction(String name)
		{
			type = Type.Function;
			this.name = name;
			return newOperands();
		}

		ParseElement<T> newOperands()
		{
			this.operands = new ArrayList<>();
			return this;
		}
	};

	@RequiredArgsConstructor
	public static abstract class TypedExpression<T> implements Expression<T>
	{
		@Getter
		private final Type type;
	}

	public static class ConstantExpression<T> extends TypedExpression<T>
	{
		private final T value;

		public ConstantExpression(Type type, T value)
		{
			super(type);
			this.value = value;
		}

		@Override
		public T evaluate(Context<T> parameters)
		{
			return value;
		}
	}

	public static class UndefinedExpression<T> extends TypedExpression<T>
	{
		public UndefinedExpression(Type type)
		{
			super(type);
		}

		@Override
		public T evaluate(Context<T> parameters)
		{
			return null;
		}
	}

	public static class VariableExpression<T> extends TypedExpression<T>
	{
		private final String name;

		public VariableExpression(Type type, String name)
		{
			super(type);
			this.name =name;
		}

		@Override
		public T evaluate(Context<T> parameters)
		{
			return parameters.getVariable(name);
		}
	}

	public static class Builder<T>
	{
		private Character separator = ',';

		private Function<CustomizableEvaluatorFactory<T>, BiFunction<StringIterator, ParserDesired, TokenInfo<T>>> tokenParser;

		private Function<CustomizableEvaluatorFactory<T>, Predicate<StringIterator>> spaceSkipper;

		private Function<CustomizableEvaluatorFactory<T>, Function<StringIterator, Expression<T>>> stringParser;

		private Function<CustomizableEvaluatorFactory<T>, Function<StringIterator, Expression<T>>> numberParser;

		private Function<CustomizableEvaluatorFactory<T>, Function<String, T>> numberConverter;

		private Function<CustomizableEvaluatorFactory<T>, Function<StringIterator, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>>> unaryOperatorParser;

		private Function<CustomizableEvaluatorFactory<T>, Function<StringIterator, Map.Entry<Function<List<Expression<T>>, Expression<T>>, Integer>>> binaryOperatorParser;

		private Function<CustomizableEvaluatorFactory<T>, Function<StringIterator, String>> nameParser;

		private Function<CustomizableEvaluatorFactory<T>, Function<String, Expression<T>>> variableResolver;

		private Function<CustomizableEvaluatorFactory<T>, BiFunction<String, List<Expression<T>>, Expression<T>>> functionResolver;

		public CustomizableEvaluatorFactory<T> build()
		{
			return new CustomizableEvaluatorFactory<T>(this);
		}
	}
}
