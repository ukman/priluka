# Priluka

Priluka is an experimental Java library for building compact parsers from
ordinary Java classes.

The core idea is to describe a language grammar as a set of Java classes,
annotations, fields, and naming conventions. Parsing should produce an
instance of the grammar class directly, without forcing the user to write a
separate AST mapping layer.

## Vocabulary

Priluka uses classic compiler-construction terminology, close to Aho/Ullman
and the Dragon Book:

- grammar
- terminal
- nonterminal
- production rule
- derivation
- parser
- lexer
- token

For now, Priluka intentionally does not choose a parser family such as LL(1),
LR(k), recursive descent, PEG, or another approach. The first design target is
the Java-based grammar description format. A grammar is read through reflection,
then an internal parser is built from that discovered grammar model.

## Goal

Make parser definitions feel like data model definitions:

```java
// @Grammar(separator = SPACE) // implicit default parameters
public class A {
    private int a;
    private int b;
}
```

The class above describes a language made from two integers separated by a
space:

```text
1 2
34 56
```

Target usage:

```java
A a = Parser.parse(A.class, "456 78");
```

The parser should return an initialized instance of `A`.

## Coordinates

Primary Java package:

```text
io.github.ukman.priluka
```

Maven coordinates:

```xml
<groupId>io.github.ukman</groupId>
<artifactId>priluka</artifactId>
```

## Current Implementation Status

Implemented:

- annotation API in `io.github.ukman.priluka.annotation`
- runtime `Token`, `GrammarException`, and `ParseException`
- `Parser.describe(...)`
- `Parser.init(classes).describe(...)`
- `Parser.initFromOuterClass(...).describe(...)`
- compact `GrammarModel`
- BNF-like diagnostics through `GrammarModel.toBnf()`
- reflection discovery for constructor productions
- multiple constructors as alternatives
- interface alternatives inside an explicit class universe
- built-in terminals for `Integer`, `Double`, and `Boolean`
- explicit `@Terminal` and `@Keyword` terminals
- arrays, collections, `Optional<T>`, `@OneOrMore`, and `@Separator`
- internal lexer `Lexeme` model
- internal master-regexp builder with lexer priority ordering
- keyword branch quoting in master regexps
- lexer tokenization loop with same-span terminal recheck
- skip-token filtering
- keyword carrier optimization for keywords covered by terminals such as `Id`
- manual lexer performance baseline test

Not implemented yet:

- NFA compiler
- NFA simulation
- parse trace
- object construction from a parse trace
- real `Parser.parse(...)`

Manual lexer benchmark:

```bash
mvn -Dpriluka.perf=true \
    -Dtest=LexerPerformanceTest \
    -Dpriluka.perf.bytes=5242880 \
    -Dpriluka.perf.warmup=2 \
    -Dpriluka.perf.runs=3 \
    test
```

The benchmark compares:

- strict lexer mode: no duplicate terminal recheck, each lexeme has one terminal
  type
- multi-variant lexer mode: identifier/keyword ambiguity through keyword carrier
  maps, without full duplicate-regexp recheck

The full same-span duplicate-regexp recheck is intentionally not part of this
baseline because it is much slower and should be reserved for grammars that
need general overlapping terminals beyond the common `Id`/keyword case.

The benchmark also reports scan-only mode, which counts tokens without creating
`Lexeme` objects or storing token lists. This estimates the potential win from a
streaming/cursor lexer or more careful allocation strategy.

Experimental `experiment/brics-lexer` result after splitting lexer API and
implementations:

```text
java-regex strict: bytes=5242919 tokens=1065406 avg=0.5948s speed=8.41 MiB/s
java-regex multi-variant: bytes=5242919 tokens=1065406 avg=0.9498s speed=5.26 MiB/s
java-regex strict scan-only: bytes=5242919 tokens=1065406 avg=0.3859s speed=12.96 MiB/s
java-regex multi-variant scan-only: bytes=5242919 tokens=1065406 avg=0.3259s speed=15.34 MiB/s
brics strict: bytes=5242919 tokens=1065406 avg=0.8274s speed=6.04 MiB/s
brics multi-variant: bytes=5242919 tokens=1065406 avg=0.5911s speed=8.46 MiB/s
brics strict scan-only: bytes=5242919 tokens=1065406 avg=0.1294s speed=38.65 MiB/s
brics multi-variant scan-only: bytes=5242919 tokens=1065406 avg=0.1157s speed=43.20 MiB/s
```

In this branch `Lexer` is an engine-neutral interface. `JavaRegexLexer` keeps
the previous master-regexp implementation, while `BricsLexer` uses
`dk.brics.automaton.RunAutomaton` as its scanning foundation. The brics
implementation is intentionally simple: one automaton per master terminal,
longest match at the current position, and regular Java object allocation for
`Lexeme` in full tokenization mode. The scan-only numbers show that
deterministic automata can be much faster than the previous `java.util.regex`
lexer path, while the full tokenization numbers still expose allocation and
ambiguity-collection costs.

The previous `java.util.regex` lexer path uses `Matcher.find()` plus an explicit gap check
(`matcher.start() == currentPosition`) instead of resetting `region(...)` and
calling `lookingAt()` at every token boundary. This keeps error detection for
unmatched input while allowing the regex engine to scan more efficiently.

Manual Java regex engine baseline:

```bash
mvn -Dpriluka.perf=true \
    -Dtest=RegexEnginePerformanceTest \
    -Dpriluka.perf.bytes=5242880 \
    -Dpriluka.perf.warmup=3 \
    -Dpriluka.perf.runs=5 \
    test
```

This benchmark generates about 5 MiB of random 1-10 digit numbers separated by
spaces and counts matches of `[0-9]+` with plain `Matcher.find()`, without
calling `group()`.

It also includes a mixed-token regex baseline with whitespace, identifiers,
numbers, quoted strings, and one-character operators:

```text
\s+|[A-Za-z_][A-Za-z0-9_]*|[0-9]+|"([^"\\]|\\.)*"|[+\-=()\[\]{}.,*/?]
```

Manual regex engine comparison:

```bash
mvn -Dpriluka.perf=true \
    -Dtest=RegexEngineComparisonPerformanceTest \
    -Dpriluka.perf.bytes=5242880 \
    -Dpriluka.perf.warmup=3 \
    -Dpriluka.perf.runs=5 \
    test
```

This benchmark tokenizes the same generated mixed stream and determines token
type for every lexeme:

- `java.util.regex` as one master regexp with capture groups
- `RE2/J` as one master regexp with capture groups
- `dk.brics.automaton` as one `RunAutomaton` per token type, choosing the
  longest match at the current position

Current local result on the 5 MiB generated stream:

```text
java.util.regex master: bytes=5242883 tokens=668585 avg=0.1423s speed=35.13 MiB/s
RE2/J master: bytes=5242883 tokens=668585 avg=2.6879s speed=1.86 MiB/s
dk.brics per-token: bytes=5242883 tokens=668585 avg=0.0756s speed=66.18 MiB/s
```

This does not yet prove that brics is the final lexer implementation, because
the test uses only five token families and brics is not using one tagged master
automaton here. It does show that precompiled deterministic automata are a
serious candidate for the fast lexer path, while RE2/J is not attractive for
this particular Java master-regexp tokenization shape.

## Design Direction

- A grammar is primarily described by Java classes.
- Fields can describe grammar parts when Priluka uses field order to infer a
  constructor-like production, especially together with Lombok.
- Field order is semantically important unless explicitly overridden.
- Primitive Java types can map to built-in lexical or syntactic rules.
- Annotations provide explicit configuration only where conventions are not
  enough.
- The default syntax should stay compact and readable.
- Parsing should produce strongly typed Java objects immediately.

## Initial Conventions

These conventions are not final decisions yet.

- `int` fields parse integer tokens.
- Multiple fields in one class are parsed in declaration order.
- The default separator between adjacent fields is a single space or a
  whitespace-like separator.
- `Parser.parse(SomeClass.class, input)` is the primary entry point.
- The class passed to `parse` is the target nonterminal / start symbol.
- Private fields should be supported, likely through reflection.
- Whitespace and comments can be represented as skip terminals with `@Skip`.
- Terminal classes do not have to extend `Token`.
- Extending `Token` is an optional convenience when the user wants classic
  lexer metadata such as source position, text, and token type.
- A terminal can be any class that is not interpreted as a nonterminal and has
  a lexical recognition rule.
- A lexical recognition rule can be provided explicitly with
  `@Terminal(regexp = "...")`.
- Keyword-like terminals can use `@Keyword`, where the class name defines the
  recognized text by convention.
- Some common Java value types are implicit built-in terminals, initially
  `Integer`, `Double`, and `Boolean`.
- There should be no separate `@Nonterminal` annotation. Grammar classes that
  are not terminals are nonterminals.
- For concrete nonterminal classes, constructors describe production rules.
- Multiple constructors in the same concrete class describe alternative
  production rules for that class.
- For interface nonterminals, implementing classes describe alternative
  production rules.
- Arrays, collections, and `Optional<T>` can describe EBNF-like repetition and
  optionality in constructor parameters.
- The grammar class universe can be provided explicitly, or inferred from
  nested classes inside an outer grammar class.

## Java Classes As Productions

Java type relationships can describe grammar production rules.

A class can describe a concrete production. An interface or abstract class can
describe a nonterminal with several possible derivations. Implementations or
subclasses then become alternatives for that nonterminal.

Interfaces are especially important in Java 8 because they give a compact way
to model a family of grammar symbols without extra annotations. An interface is
not only a Java abstraction; in Priluka it is also a nonterminal category.

Example:

```java
interface Operator {
    double doOperation(double a, double b);
}

class PlusOperator implements Operator {
    PlusOperator(Plus plus) {
    }

    double doOperation(double a, double b) {
        return a + b;
    }
}

class MinusOperator implements Operator {
    MinusOperator(Minus minus) {
    }

    double doOperation(double a, double b) {
        return a - b;
    }
}

@Terminal(regexp = "\\-")
class Minus extends Token {
}

@Terminal(regexp = "\\+")
class Plus extends Token {
}
```

This can be interpreted as production rules:

```text
Operator      => PlusOperator
PlusOperator  => "+"
Operator      => MinusOperator
MinusOperator => "-"
```

The expressive goal is that semantic behavior can live in the same Java type
that describes the grammar alternative. In the example, `PlusOperator` is both
a derivation of `Operator` and an executable operation.

## Grammar Boundary And Discovery

The target class passed to `parse` defines the start symbol:

```java
S s = Parser.parse(S.class, "xxxxx");
```

Here `S` is the target nonterminal. Its constructors are interpreted as
production rules. Any types referenced from those constructors can recursively
become grammar symbols.

For inheritance-based alternatives, Priluka needs a known universe of classes
to inspect. It should not scan the entire classpath by default.

One option is explicit registration:

```java
Class<?>[] classes = new Class<?>[] {
    S.class,
    Expression.class,
    IfStatement.class,
    ForStatement.class
};

S s = Parser.init(classes).parse(S.class, "xxxxx");
```

In this mode, interface alternatives are discovered inside the provided class
set:

```text
Statement => IfStatement
Statement => ForStatement
```

only if `IfStatement.class` and `ForStatement.class` are present in `classes`.

Another option is an outer grammar class:

```java
class Grammar {
    static class S {
    }

    static class Expression {
    }
}

Grammar.S s = Parser
    .initFromOuterClass(Grammar.class)
    .parse(Grammar.S.class, "xxxxx");
```

In this mode, all nested classes of `Grammar` form the grammar class universe.
This keeps small grammars self-contained and avoids global classpath scanning.

## Nonterminals

There should be no `@Nonterminal` annotation.

Within a grammar, every class that is not a terminal is interpreted as a
nonterminal. Terminals are identified by explicit lexical annotations such as
`@Terminal`, keyword annotations such as `@Keyword`, or built-in terminal types
such as `Integer`, `Double`, and `Boolean`.

For a concrete class, constructors are production rules. Constructor parameters
are the right-hand side of the production.

Example:

```java
class Point {
    Point(Integer x, Integer y) {
    }
}
```

Conceptually:

```text
Point => Integer Integer
```

Another example:

```java
class IfStatement {
    public IfStatement(
        If ifKeyword,
        OpenBracket openBracket,
        ConditionalExpression condition,
        CloseBracket closeBracket,
        Statement statement
    ) {
    }
}
```

Conceptually:

```text
IfStatement => If OpenBracket ConditionalExpression CloseBracket Statement
```

The parameter names can be useful for object construction and readability, but
the grammar symbols in the production are the parameter types.

Multiple constructors in one concrete class are multiple production rules for
the same nonterminal.

Example:

```java
class PrimaryExpression {
    public PrimaryExpression(Integer number) {
    }

    public PrimaryExpression(
        OpenBracket openBracket,
        Expression expression,
        CloseBracket closeBracket
    ) {
    }
}
```

Conceptually:

```text
PrimaryExpression => Integer
PrimaryExpression => OpenBracket Expression CloseBracket
```

This allows small alternatives to stay in one Java class when separate
implementation classes would add noise.

## Repetition And Optionality

Java container types can describe grammar operators:

```text
T[]                    => T*
Collection<T>         => T*
@OneOrMore T[]        => T+
@OneOrMore Collection<T> => T+
@Separator(S.class) T[]  => empty | T (S T)*
@OneOrMore @Separator(S.class) T[] => T (S T)*
@Separator(value = S.class, trailing = true) T[] => empty | T (S T)* S?
@OneOrMore @Separator(value = S.class, trailing = true) T[] => T (S T)* S?
Optional<T>             => T?
```

Arrays and collections mean zero-or-more (`*`) by default. `@OneOrMore` turns a
repeated parameter into one-or-more (`+`). `Optional<T>` means an optional
grammar symbol. `@Separator` defines a terminal that separates repeated
elements.

Example:

```java
class CompositeStatement implements Statement {
    public CompositeStatement(
        OpenBrace openBrace,
        Statement[] statements,
        CloseBrace closeBrace
    ) {
    }
}
```

Conceptually:

```text
CompositeStatement => OpenBrace Statement* CloseBrace
```

For non-empty repetitions:

```java
class NonEmptyCompositeStatement implements Statement {
    public NonEmptyCompositeStatement(
        OpenBrace openBrace,
        @OneOrMore Statement[] statements,
        CloseBrace closeBrace
    ) {
    }
}
```

Conceptually:

```text
NonEmptyCompositeStatement => OpenBrace Statement+ CloseBrace
```

For separated repetitions:

```java
@Terminal(regexp = ",")
class Comma {
}

class ArrayNumber {
    public ArrayNumber(@Separator(Comma.class) Number[] numbers) {
    }
}
```

Conceptually:

```text
ArrayNumber => empty | Number (Comma Number)*
```

`@Separator` should be used on array or collection parameters. The separator is
itself a terminal class. Trailing separators are forbidden by default.

Trailing separators can be enabled explicitly:

```java
class ArrayNumberWithTrailingComma {
    public ArrayNumberWithTrailingComma(
        @Separator(value = Comma.class, trailing = true) Number[] numbers
    ) {
    }
}
```

Conceptually:

```text
ArrayNumberWithTrailingComma => empty | Number (Comma Number)* Comma?
```

For a non-empty separated repetition:

```java
class NonEmptyArrayNumber {
    public NonEmptyArrayNumber(
        @OneOrMore @Separator(Comma.class) Number[] numbers
    ) {
    }
}
```

Conceptually:

```text
NonEmptyArrayNumber => Number (Comma Number)*
```

For a non-empty separated repetition with a trailing separator:

```java
class NonEmptyArrayNumberWithTrailingComma {
    public NonEmptyArrayNumberWithTrailingComma(
        @OneOrMore
        @Separator(value = Comma.class, trailing = true)
        Number[] numbers
    ) {
    }
}
```

Conceptually:

```text
NonEmptyArrayNumberWithTrailingComma => Number (Comma Number)* Comma?
```

Alternatives can still be expressed with multiple constructors:

```java
class PlusMinus {
    public PlusMinus(Plus plus) {
    }

    public PlusMinus(Minus minus) {
    }
}
```

Conceptually:

```text
PlusMinus => Plus
PlusMinus => Minus
```

And optional signs can be expressed with `Optional<T>`:

```java
class SignedNumber {
    public SignedNumber(Optional<PlusMinus> optionalSign, Number number) {
    }
}
```

Conceptually:

```text
SignedNumber => PlusMinus? Number
```

For an interface, each implementation is an alternative production rule.

Example:

```java
interface Statement {
}

class IfStatement implements Statement {
}

class ForStatement implements Statement {
}
```

Conceptually:

```text
Statement => IfStatement
Statement => ForStatement
```

Interfaces can also describe operator or symbol groups:

```java
interface AdditiveOperator {
}

class Plus implements AdditiveOperator {
}

class Minus implements AdditiveOperator {
}

class BinaryExpression {
    public BinaryExpression(
        Expression left,
        AdditiveOperator operator,
        Expression right
    ) {
    }
}
```

Conceptually:

```text
AdditiveOperator => Plus
AdditiveOperator => Minus
BinaryExpression => Expression AdditiveOperator Expression
```

## Terminals And Tokens

A terminal is represented by a Java class. A token is an instance of that
terminal class, created from a concrete fragment of the input string.

This means terminal classes are not just labels in the grammar. They can carry
typed values parsed from the source text.

Example:

```java
@Terminal(regexp = "[0-9]+")
@AllArgsConstructor
class IntNumber {
    int a;
}
```

Target usage:

```java
IntNumber n = Parser.parse(IntNumber.class, "4546");
```

Here `IntNumber` is a terminal in the grammar. The value `n` is a token
instance that contains the parsed value `4546`.

Conceptually:

```text
IntNumber => "[0-9]+"
```

And for a specific input:

```text
"4546" => new IntNumber(4546)
```

This keeps lexical structure strongly typed. Instead of returning a generic
token with a string value and a token kind, Priluka can return a domain-specific
Java object such as `IntNumber`.

Terminal value conversion should reuse the same built-in parsing rules that
exist for implicit terminals. If a terminal class has constructor parameters of
types such as `Integer`, `Double`, or `Boolean`, Priluka converts the matched
text to those parameter types using the usual built-in conversion rules.

Example:

```java
@Terminal(regexp = "[0-9]+")
class IntNumber {
    public IntNumber(Integer value) {
    }
}
```

For input `"4546"`:

```text
"4546" => new IntNumber(4546)
```

The same principle applies to fields inferred through Lombok-generated
constructors.

When a terminal regexp has capture groups, constructor parameters or fields can
choose a specific group with `@RegexGroup`.

Example:

```java
@Terminal(regexp = "([0-9])([0-9])")
class TwoDigit {
    public TwoDigit(
        @RegexGroup(index = 1) int d1,
        @RegexGroup(index = 2) int d2
    ) {
    }
}
```

For input `"42"`:

```text
"42" => new TwoDigit(4, 2)
```

Named regexp groups should also be supported:

```java
@Terminal(regexp = "(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2})")
class DateToken {
    public DateToken(
        @RegexGroup(name = "year") int year,
        @RegexGroup(name = "month") int month,
        @RegexGroup(name = "day") int day
    ) {
    }
}
```

`@RegexGroup` should support two addressing modes:

```java
@RegexGroup(index = 1)
@RegexGroup(name = "year")
```

The selected group text is then converted using the same built-in conversion
rules as normal terminal values.

There should also be an optional base `Token` class for terminal classes that
need classic lexical metadata:

```java
class Token {
    int start;
    int len;
    String text;
    Class<? extends Token>[] tokenTypes;
}
```

Terminal classes are not required to extend `Token`. If a terminal class does
extend `Token`, its instances contain both the typed value and the classic token
information from the source text.

Example:

```java
@Terminal(regexp = "[0-9]+")
@AllArgsConstructor
class IntNumber extends Token {
    int a;
}
```

For input `"4546"`, the produced token can contain:

```text
IntNumber.a       = 4546
IntNumber.start   = 0
IntNumber.len     = 4
IntNumber.text    = "4546"
IntNumber.tokenTypes = { IntNumber.class }
```

This gives two views of the same object:

- as a typed domain value: `IntNumber(4546)`
- as a classic lexer token: position, length, text, possible token types

A single text fragment can match several terminal regexps. In that case, the
token should keep all possible terminal types, and the parser can choose the
terminal that fits the current derivation.

Example:

```java
@Keyword(caseSensitive = true)
class IfKeyword extends Token {
}

@Terminal(regexp = "[a-zA-Z_][a-zA-Z0-9_]*")
class Id extends Token {
}
```

For input `"if"`, both terminals can match:

```text
Token.text       = "if"
Token.tokenTypes = { IfKeyword.class, Id.class }
```

The final decision can be delayed until parsing. In a position where the grammar
expects `IfKeyword`, the token can be consumed as `IfKeyword`; in a position
where the grammar expects `Id`, the same text can be consumed as `Id`.

## Terminal Declaration

A class is a terminal when it is not interpreted as a nonterminal and Priluka
can derive a regular expression for matching it.

The explicit form is `@Terminal`:

```java
@Terminal(regexp = "[0-9]+")
class IntNumber {
    int a;
}
```

Keyword-like terminals can use `@Keyword`. In this form, the class name is the
source for the recognized text:

```java
@Keyword(caseSensitive = false)
class If {
}
```

Conceptually, this means:

```text
If => "if"
```

With `caseSensitive = false`, inputs such as `if`, `IF`, and `If` should all
match the same terminal class.

## Skip Terminals

Whitespace and comments should be expressible as terminals that are recognized
by the lexer but skipped by the parser between grammar symbols.

Example:

```java
@Skip
@Terminal(regexp = "\\s+")
class Whitespace {
}

@Skip
@Terminal(regexp = "//[^\\n]*")
class LineComment {
}
```

Conceptually, skip terminals are part of lexical recognition but not part of
the visible derivation tree. They keep grammar constructors compact because
rules do not need to mention whitespace explicitly.

## Enum Terminals

Java enums can provide a compact way to declare a fixed family of keyword or
literal terminals.

Example:

```java
@Keywords(caseSensitive = false)
enum BooleanKeyword {
    TRUE,
    FALSE
}
```

Conceptually:

```text
BooleanKeyword => "true"
BooleanKeyword => "false"
```

Enums are especially useful when the grammar needs a closed set of simple
terminal values and a separate Java class for each value would be too verbose.

## Built-In Terminals

Priluka should provide a small set of implicit terminals for common Java value
types:

```text
Integer
Double
Boolean
```

These types can appear directly inside grammar classes without a custom
terminal class:

```java
class Point {
    Integer x;
    Integer y;
}
```

Conceptually:

```text
Integer => integer-number-regexp
Double  => floating-point-number-regexp
Boolean => boolean-literal-regexp
```

This keeps very small grammars compact while still allowing custom terminal
classes such as `IntNumber` when the user wants a named grammar symbol,
additional validation, source metadata, or domain behavior.

## Lexer

The lexer has an engine-neutral API:

```java
public interface Lexer {
    List<Lexeme> tokenize(String input);

    int countTokens(String input);
}
```

Current implementations:

- `JavaRegexLexer` uses the standard Java regular expression engine and one
  master regexp.
- `BricsLexer` uses `dk.brics.automaton.RunAutomaton` and currently keeps one
  automaton per master terminal.
- `Lexers` is the factory entry point for selecting the implementation.

The default implementation in the experimental branch is `BricsLexer`, but the
Java-regex implementation remains available as a reference backend and fallback.

The key Java-regex optimization is to build one master regexp from all terminal
regexps:

```text
(?<T0>regexp0)|(?<T1>regexp1)|(?<T2>regexp2)|...
```

or, if named groups are inconvenient for generated branch names:

```text
(regexp0)|(regexp1)|(regexp2)|...
```

At each token boundary, the Java-regex lexer applies the single compiled
`Pattern`. The branch group that matched identifies at least one terminal
candidate.

Conceptually:

```text
TerminalSymbol[]
    => master Pattern
    => one regex match per token boundary
    => Lexeme(start, len, text, possibleTerminalTypes)
```

This avoids trying every terminal regexp at every character position while
still relying on Java's standard regex implementation.

There is one important limitation: a master regexp branch tells which
alternative Java regex selected, but it does not automatically reveal every
terminal regexp that could match the same text.

Example:

```java
@Keyword
class If {
}

@Terminal(regexp = "[a-zA-Z_][a-zA-Z0-9_]*")
class Id {
}
```

For input `"if"`, both `If` and `Id` should be possible terminal types. A plain
regexp alternation may report only the branch that won according to regexp
alternation order.

Therefore the lexer should use a two-step strategy:

1. Use the master regexp to find the next lexeme quickly.
2. Re-check terminal regexps against only that lexeme text to collect all
   terminal types that match the same full span.

For input `"if"`:

```text
master regexp finds text "if"
full-span recheck finds { If.class, Id.class }
```

For input `"ifx"`:

```text
master regexp finds text "ifx"
full-span recheck finds { Id.class }
```

This preserves ambiguity between terminal types for the same lexeme while
avoiding all-regexps-at-all-positions scanning.

The first lexer can use maximal munch at token boundaries. If Java regexp
alternation order would otherwise choose a shorter branch, terminal branches in
the master regexp must be ordered or structured so the produced lexeme is the
longest match supported by the terminal set.

Branch ordering should use lexer priority, not grammar precedence. This is a
lexical concern only:

```java
@Terminal(regexp = "...", priority = 100)
class SomeToken {
}

@Keyword(value = "if", priority = 10)
class If {
}
```

The initial ordering strategy:

```text
priority descending
general regexp / built-in terminals before keywords
fixed text length descending for keywords
explicit class-array order when available
class name as final stable fallback
```

Keywords can also be optimized before master-regexp construction.

Most language keywords match the identifier regexp:

```java
@Keyword
class If {
}

@Terminal(regexp = "[A-Za-z_][A-Za-z0-9_]*")
class Id {
}
```

The lexer builder can test each keyword text against non-keyword terminal
regexps:

```text
"if" matches Id
"if" does not match Integer
```

If a keyword is covered by a carrier regexp such as `Id`, the keyword does not
need its own branch in the master regexp. Instead, when the lexer finds an `Id`
lexeme, it checks the lexeme text in a keyword table and adds matching keyword
terminal types.

Conceptually:

```text
master regexp branches: Id, Integer, Double, ...
keyword table for Id: "if" -> If.class, "else" -> Else.class
```

The keyword table should be split into exact and case-insensitive lookup maps:

```text
case-sensitive keywords:   Map<String, Terminal>
case-insensitive keywords: Map<lowercase String, Terminal>
```

When an `Id` lexeme is found, Priluka checks the exact map first and the
case-insensitive map with a normalized key. This avoids re-running keyword
regexps and preserves `caseSensitive = true` semantics.

For input `"if"`:

```text
master regexp finds Id("if")
keyword table adds If
tokenTypes = { Id.class, If.class }
```

For input `"123"`:

```text
master regexp finds Integer("123")
no Id keyword table is checked
tokenTypes = { Integer.class }
```

This keeps the master regexp smaller and avoids keyword bloat in the common
case where all keywords are special cases of identifiers.

## Parser Engines

Priluka should separate grammar discovery from parser execution.

The first version should focus on an NFA-based engine. The full general parser
engine is intentionally postponed.

The Java model should first be converted into a compact internal grammar model:

```text
Java classes / annotations / constructors
    => grammar model
    => NFA engine
    => object construction
```

This grammar model is the internal domain representation of the grammar:

```text
G = (N, T, S, R)
```

where:

- `N` is the set of nonterminals
- `T` is the set of terminals
- `S` is the start nonterminal
- `R` is the set of production rules

The model should stay small and practical. It is not a separate user-facing DSL;
it is an internal IR that normalizes reflection results before parser
construction.

Reasons to keep this layer:

- Reflection is a discovery mechanism, not a good execution model.
- Grammar validation becomes much clearer.
- The library can dump/debug the discovered grammar as BNF-like text.
- The NFA engine can work with normalized symbols and rules instead of Java
  constructors and annotations directly.
- Later parser engines can reuse the same grammar model.

The overhead is acceptable if the model is built once during `Parser.init(...)`
and then reused.

### NFA Engine

For simple grammars, Priluka should try to recognize an automaton-like subset
and parse it with a fast engine based on NFA simulation or a similar automata
technique.

This engine is intended for cases where the grammar is effectively regular or
can be flattened into a regular structure:

- terminals
- sequences
- alternatives
- optional parts
- repetitions
- separated repetitions
- no recursive nonterminal dependencies

Conceptually:

```text
S => A B? C*
```

can be compiled into an automaton and parsed by simulating that automaton over
the token stream.

The NFA engine should reject grammars outside its supported subset with a clear
diagnostic. It should not silently fall back in the first version.

### Parse Trace And Object Construction

The NFA engine must do more than answer whether the input belongs to the
language. It must preserve enough information to reconstruct the derivation and
build the target Java object graph.

The compiled NFA should therefore carry semantic actions on states or
transitions. During simulation, an accepting path produces a parse trace.

Possible trace events:

```text
beginProduction(Nonterminal, Production)
endProduction(Nonterminal, Production)
consumeTerminal(Terminal, Token)
beginRepeat(Parameter)
appendRepeatElement(Parameter)
endRepeat(Parameter)
beginOptional(Parameter)
endOptional(Parameter, present/absent)
```

After the NFA accepts, Priluka can replay the trace with an object builder:

```text
parse trace
    => derivation tree / construction stack
    => constructor calls
    => instance of S
```

Example:

```java
class Point {
    public Point(Integer x, Integer y) {
    }
}
```

For input `"1 2"`, the trace can represent:

```text
beginProduction(Point, Point(Integer, Integer))
consumeTerminal(Integer, "1")
consumeTerminal(Integer, "2")
endProduction(Point, Point(Integer, Integer))
```

The builder then calls:

```java
new Point(1, 2)
```

For alternatives, the accepting path determines the chosen production:

```java
class PlusMinus {
    public PlusMinus(Plus plus) {
    }

    public PlusMinus(Minus minus) {
    }
}
```

Input `"+"` produces a trace that chooses:

```text
PlusMinus => Plus
```

and therefore calls:

```java
new PlusMinus(new Plus(...))
```

This means the NFA representation should not erase production identity. Every
path that corresponds to a production must keep enough metadata to call the
right constructor and place parsed child values into the right parameters.

### First Version API

The public API can still look compact:

```java
S s = Parser.parse(S.class, input);
```

or use an initialized parser when the grammar class universe is needed:

```java
S s = Parser
    .init(classes)
    .parse(S.class, input);
```

Internally, `Parser.init(...)` should build and validate the grammar model once,
then compile the supported subset to the NFA representation.

## Lombok Integration

Priluka should work well with Lombok because Lombok can remove most constructor
boilerplate while still leaving a constructor-like grammar rule implied by the
field order.

Example:

```java
@AllArgsConstructor
class Point {
    Integer x;
    Integer y;
}
```

This should be equivalent to:

```java
class Point {
    Integer x;
    Integer y;

    public Point(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
}
```

Conceptually:

```text
Point => Integer Integer
```

The desired ergonomic rule is that for the common case, field order plus a
Lombok-generated constructor can define both the Java object construction and
the grammar production. This keeps most grammar classes close to plain data
classes.

## Open Questions

- Should the default separator be exactly one space, arbitrary whitespace, or a
  configurable token such as `SPACE`?
- Should field declaration order be trusted? Java reflection order is commonly
  stable in practice, but relying on it as a formal grammar rule needs care.
- Should private fields be set through reflection, constructors, setters, or a
  generated mapper?
- Should invalid input fail with exceptions, result objects, diagnostics, or a
  configurable error strategy?
- Which grammar features should be accepted by the first NFA engine, and which
  should be rejected with clear diagnostics?
- Should grammar metadata be read at runtime through reflection, generated at
  compile time with annotation processing, or both?
- How should Priluka discover all implementations of an interface or subclasses
  of an abstract class at runtime?
- Should `initFromOuterClass` include only direct nested classes, or nested
  classes recursively?
- Should explicitly provided class arrays be required for inheritance-based
  grammar alternatives outside an outer grammar class?
- Should fields also be allowed to define production rules, or should
  constructor parameters be the canonical source for concrete nonterminals?
- Should a terminal with no fields represent only the presence of a lexeme, such
  as `Plus` or `Minus`?
- How exactly should Java class names be converted into keyword text:
  `If -> "if"`, `ElseIf -> "else if"`, `ElseIf -> "elseIf"`, or
  `ElseIf -> "else_if"`?
- Should `@Keyword` support an explicit override for cases where the class name
  is not enough?
- Should `Token.tokenTypes` store only terminal classes that matched the same
  text span, or also parser-level interpretations chosen later?
- Should `len` count Java `char` values, Unicode code points, or bytes in the
  original input encoding?
- Should primitive types `int`, `double`, and `boolean` behave exactly like
  `Integer`, `Double`, and `Boolean`?
- What exact regular expressions should be used for built-in `Integer`,
  `Double`, and `Boolean`?
- Should built-in `Boolean` accept only `true`/`false`, or also values such as
  `TRUE`, `FALSE`, `yes`, `no`, `1`, and `0`?
- Should `List<T>`, `Set<T>`, arrays, and arbitrary `Collection<T>` all be
  supported, or should the first version support only arrays and `List<T>`?
- What exact grammar subset qualifies for the fast automaton/NFA engine?
- How should the NFA engine report that a grammar is outside the supported
  automaton-like subset?
- How should the master lexer regexp guarantee maximal munch when Java regexp
  alternation can prefer an earlier shorter alternative?
- Should same-span terminal ambiguity be collected by re-checking all terminal
  regexps, or only terminal regexps that could plausibly match based on a
  precomputed index?
- Should covered keywords be removed from the master regexp whenever they match
  a carrier terminal such as `Id`, or should this optimization be configurable?
- Should the NFA simulation store full traces for all active paths, or use a
  compact predecessor/backpointer structure and reconstruct only the accepting
  path?
- If several accepting paths exist, should the first version report ambiguity,
  choose one deterministically, or return all possible parses?
- Should object construction happen during NFA simulation, or only after an
  accepting trace is selected?
- How should skip terminals be registered: globally per grammar, discovered by
  classpath scanning, or only when reachable from a grammar module?
- Should enum terminals use enum constant names directly, lower-case names, or
  explicit values provided by annotations or constructor fields?
- How should Priluka read Lombok-generated constructors and field order in a
  way that stays predictable with Java 8 reflection?
- Should `@RegexGroup` be allowed on both constructor parameters and fields, or
  should constructor parameters be the canonical form?
- What should happen if both `index` and `name` are set on `@RegexGroup`?

## Potential Inconsistencies And Risks

- `separator` is easy to misspell as `seperator`; the public API should choose
  one correct spelling and keep it consistent.
- The example says `private int a,b;`, but the sketch contains a typo:
  `pruvate`. The canonical example uses normal Java syntax.
- If field order matters, reflection order must be specified by the library's
  contract or replaced with an explicit ordering mechanism such as
  `@Part(1)`.
- Private fields make the target model concise, but they raise questions around
  immutability, validation, modules, and Java access restrictions.
- Directly mapping grammar classes to result objects is ergonomic, but it may
  become limiting if the parsed syntax and desired object model diverge.
- Implicit defaults are convenient, but too many hidden rules can make grammar
  behavior hard to debug.
- Java can easily list implemented interfaces for a class, but it cannot
  reliably list every class that implements an interface without a known class
  universe. Priluka should use explicit class arrays or outer grammar classes
  instead of scanning the whole classpath by default.
- If inheritance means grammar alternatives, normal Java inheritance used only
  for code reuse may accidentally change the grammar.
- Constructors are attractive for immutable grammar nodes, but constructor
  parameter names require compiler metadata (`-parameters`) or annotations if
  names matter.
- Constructor syntax must stay valid Java: a constructor has the same name as
  the class and does not use the `class` keyword; every parameter also needs a
  name even if only its type matters for the grammar.
- The terminology must stay precise: `IntNumber` is a terminal class, while
  `new IntNumber(4546)` is the token instance produced from input.
- `Token.tokenTypes` intentionally allows lexical ambiguity, for example when
  `"if"` matches both `IfKeyword` and `Id`.
- `@Keyword` based on class names is compact, but it needs precise naming rules
  to avoid surprising matches.
- Built-in terminals are convenient, but their exact regexp and conversion
  rules must be stable and predictable.
- Treating all non-terminal grammar classes as nonterminals keeps the API small,
  but helper classes inside the configured grammar universe may accidentally
  become grammar symbols.
- Java generic type information is available for constructor parameters through
  reflection, but raw collections lose the element type and should probably be
  rejected for grammar definitions.
- Arrays preserve their component type clearly, which makes them a compact and
  reliable repetition syntax.
- The sketch `PlusMinual` is assumed to mean a second `PlusMinus` constructor;
  the canonical spelling should stay consistent in examples and APIs.
- `@Separator` must only apply to repeated parameters; applying it to a scalar
  parameter should be a grammar-definition error.
- Skip terminals must not accidentally hide meaningful syntax. For example,
  newlines can be insignificant in one language and significant in another.
- Enum terminals are compact, but they partially differ from the main
  "terminal is a class" model and need a precise mapping to token instances.
- Lombok improves readability, but field-order-based grammar rules must be
  specified carefully because Java source order, reflection order, and generated
  constructor order are related but should not be left as folklore.
- `@RegexGroup` must report clear grammar-definition errors for missing groups,
  invalid indexes, duplicate/conflicting group bindings, and failed type
  conversions.
- The fast engine must never accept a grammar by optimistic guess. It should be
  used only when the grammar analyzer can prove the grammar is inside its
  supported subset.
- A pure accept/reject NFA is insufficient for Priluka. The engine must preserve
  production identity and child values, otherwise it cannot reconstruct the Java
  object graph after parsing.
- A master lexer regexp is fast and uses the standard Java engine, but regexp
  alternation alone does not preserve all same-span terminal ambiguities. Priluka
  must explicitly re-check the chosen lexeme text when ambiguity matters.
- Keyword preprocessing can keep the master regexp small, but it depends on
  correctly identifying carrier terminals such as `Id`.

## Ideas To Explore

- Support a small set of annotations first:
  `@Grammar`, `@Terminal`, `@Keyword`, `@Keywords`, `@Skip`, `@Optional`,
  `@Many`, `@OneOrMore`, `@Separator`, `@RegexGroup`, `@Choice`, `@Part`.
- Treat simple classes as sequence parsers by default.
- Provide parser diagnostics that explain which field or rule failed.
- Consider a two-layer design:
  grammar model discovery first, parsing engine second.
- Keep a minimal runtime reflection mode, then optionally add annotation
  processing for speed and better compile-time validation.

## Build

```bash
mvn test
```

## Structure

- `src/main/java` - application/library code
- `src/test/java` - unit tests
- `src/main/resources` - application resources
- `src/test/resources` - test resources
