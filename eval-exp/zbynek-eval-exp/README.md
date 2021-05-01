# Zbynek math expression evaluation library

## Introduction

The library provides a framework for defining expression, parsing them and evaluating them.  The core offers basic math expression for double math and common operations for long integer types.

The framework supports:
- Parenthesis.
- Operators precedence, both left and right.
- Operators, unary and binary.
- Constants.
- Functions.
- Variables.
- Type safety: type is checked at parse time and allows safely checking mix of boolean conditions and math operations for example.
- Null handling: operations (if designed so) can accept `null` values and provide `null` result at output.
- Lazy evaluation: allows safe conditional evaluation.

Almost anything can be customized:
- Parser, to reasonable level, though completely new one can be provided.
- Operators.
- Functions.
- Variables.
- Constant parsing (for example, 0b can be added for bit numbers).

The API is made to be parsed once and executed multiple times.  The parsing is of course the slowest operation, therefore it should be avoided or cached (it is still very fast though!).

## Usage

### Double Math

Math expression for double type.

```
// Creates new instance, initializing with double math operations and functions:
EvaluatorFactory factory = DoubleMathEvaluatorProvider.populateMath(CustomizableEvaluatorFactory.builder());

// Parses the expression:
EvaluatorFactory.Expression<Long> expression = factory.parse("a+b");

// Evaluates expression, based on provided context parameters:
Double result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L, "b", 3L)));

// Something more complex:
EvaluatorFactory.Expression<Long> expression = factory.parse("sin(pi()/2)*(2+1)");
```

Double math provider supports common operations and functions:
- Unary `+`, `-`
- Binary `+`, `-`, `*`, `/`, `%`
- Constants: `pi`, `e`
- Conversion functions: `radians`, `degrees`
- Goniometric functions: `sin`, `cos`, `tan`, `cotan`, `asin`, `acos`, `atan`, `acotan`, `atan2`
- Exponential functions: `ln`, `log2`, `log10`, `exp`, `pow`
- Other: `abs`

### Math expressions for double with boolean

Math expressions for double type, in addition allowing comparison boolean operators and appropriate functions.

```
// Creates new instance, initializing with double math operations and functions:
EvaluatorFactory factory = DoubleEvaluatorProvider.populateBitAndCalc(CustomizableEvaluatorFactory.builder());

// Parses the expression:
EvaluatorFactory.Expression<Long> expression = factory.parse("nullfor(ifelse(a <= b, a, b), 5)");

// Evaluates expression, based on provided context parameters:
Long result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L, "b", 3L)));
```

In addition to standard long expression provider, the following are supported:
- Comparison `==`, `!=`, `>`, `>=`, `<`, `<=`
- Functions `isnull`, `ifnull`, `ifelse`

### Long integer expressions

```
// Creates new instance, initializing with double math operations and functions:
EvaluatorFactory factory = StandardLongEvaluatorProvider.populateBitAndCalc(CustomizableEvaluatorFactory.builder());

// Parses the expression:
EvaluatorFactory.Expression<Long> expression = factory.parse("a+b");

// Evaluates expression, based on provided context parameters:
Long result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L, "b", 3L)));

// Something more complex:
EvaluatorFactory.Expression<Long> expression = factory.parse("(a+b)*swap32((c<<2)&0xff)");
```

Long expression provider supports common operations and functions:
- Unary `+`, `-`, `~`
- Binary `+`, `-`, `*`, `/`, `%`, `*^`
- Bit `&`, `^`, `|`, `<<`, `>>`, `>>>`
- Functions `nullfor`, `swap16`, `swap32`, `swap64`

### Long integer expressions with boolean

Math expressions for long type, in addition allowing comparison boolean operators and appropriate functions.

```
// Creates new instance, initializing with double math operations and functions:
EvaluatorFactory factory = StandardLongEvaluatorProvider.populateBitAndCalc(CustomizableEvaluatorFactory.builder());

// Parses the expression:
EvaluatorFactory.Expression<Long> expression = factory.parse("nullfor(ifelse(a <= b, a, b), 5)");

// Evaluates expression, based on provided context parameters:
Long result = expression.evaluate(new MapContext<>(ImmutableMap.of("a", 1977L, "b", 3L)));
```

In addition to standard long expression provider, the following are supported:
- Comparison `==`, `!=`, `>`, `>=`, `<`, `<=`
- Functions `isnull`, `ifnull`, `ifelse`

## Benchmarks

Both parser and evaluator are fast.  Internally, AST is used to represent parsed result and then execute it.  Here are some numbers for simple (`2*8+10-8/2`) expression and 2015 laptop CPU:

```
Benchmark                                                      Mode  Cnt    Score     Error  Units
CustomizableEvaluatorFactoryBenchmark.benchmark0_construction  avgt    4   87.654 ±  22.651  ns/op
CustomizableEvaluatorFactoryBenchmark.benchmark1_parse         avgt    4  777.944 ± 296.776  ns/op
CustomizableEvaluatorFactoryBenchmark.benchmark2_evaluate      avgt    4   49.751 ±  10.577  ns/op
```


## License

The code is released under version 2.0 of the [Apache License][].


## Stay in Touch

Zbynek Vyskovsky

Feel free to contact me at kvr000@gmail.com and http://github.com/kvr000

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
