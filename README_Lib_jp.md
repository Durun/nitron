# nitron as a library Guide

## API Usages

全体的なサンプルコードは以下です。

- [Usage(Kotlin)](src/test/kotlin/com/github/durun/nitron/sample/sample.kt)
- [Usage(Java)](src/test/java/com/github/durun/nitron/sample/NitronSample.java)

### Parsing

ソースコードからASTを得るには`NitronParser`を使います。

```java
// NitronParser parser;
AstNode ast = parser.parse(reader);
```

`NitronParser`には以下の実装があります。

```
NitronParser
├ AntlrParser
└ JdtParser
```

各Parserを生成するには2通りの方法があります。

- configファイルから生成する
- 直接生成する

#### configファイルからNitronParserを生成する

```java
NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
LangConfig javaConfig = config.getLangConfig().get("java-jdt");
NitronParser parser = javaConfig.getParserConfig().getParser();
```

1. まず`NitronConfigLoader`を使って、設定全体([nitron.json](config/nitron.json))を読み込みます。
2. nitron.jsonには各言語の設定が辞書形式で記述されています。今回は`"java-jdt"`というエントリを読み込みます。(詳細は[java-jdt.json](config/lang/java-jdt.json))
3. `LangConfig`から`ParserConfig`を取り出し、`getParser()`メソッドでNitronParserを生成します。 設定内容に応じて`AntlrParser`,`JdtParser`が適切に選択されます。

#### 直接 AntlrParser を生成する

`AntlrParserKt.init()`メソッドで生成します。

```java
NitronParser parser = AntlrParserKt.init(
        "java",             // 言語の名前
        "compilationUnit",  // 翻訳単位の非終端記号名
        List.of(            // 文法ファイル
                Path.of("config/grammars/java/java8/Java8Lexer.g4"),
                Path.of("config/grammars/java/java8/Java8Parser.g4")
        ),
        List.of()           // utility javaファイル (この例では無し)
);
```

#### 直接 JdtParser を生成する

`JdtParserKt.init()`メソッドで生成します。

```java
NitronParser parser = JdtParserKt.init(JavaCore.VERSION_16);
```

### AST processing

ASTを分割・正規化するには`AstProcessor<T>`インターフェースを使います。
`AstProcessor<T>`は`process()`メソッドを介してASTの変換を行います。 以下の実装があります。

```
AstProcessor
├ AstNormalizer
└ AstSplitter
```

`AstNormalizer`はASTを正規化し`AstNode`または`null`を返します。

```java
// AstProcessor<AstNode> normalizer;
// AstNode ast;
AstNode normalizedAst = normalizer.process(ast);
// 除去規則により ASTの根 が除去された場合、nullが返ります
if (normalizedAst != null) {
  System.out.println(normalizedAst.getText());
} else {
  System.out.println("null");
}
```

`AstSplitter`はASTを分割し`List<AstNode>`を返します。

```java
// AstProcessor<List<? extends AstNode>> splitter;
// AstNode ast;
List<? extends AstNode> statements = splitter.process(ast);
```

分割と正規化を組み合わせる場合、通常は分割を先に行います。

```java
// AstProcessor<List<? extends AstNode>> splitter;
// AstProcessor<AstNode> normalizer;
// AstNode ast;
var statements = splitter.process(ast);
var normalizedStatements = statements.stream()
        .map(s -> normalizer.process(s))
        .filter(s -> Objects.nonNull(s))
        .collect(Collectors.toList());
```

各AstProcessorを生成するには2通りの方法があります。

- configファイルから生成する
- 直接生成する

#### configファイルから AstNormalizer, AstSplitter を生成する

configを読み込んだ後、`initNormalizer()`, `initSplitter()`メソッドを呼ぶことで生成できます。 ただし、これには`NodeTypePool`を渡す必要があります。 `NodeTypePool`
はParserが持つ非終端記号・終端記号の一覧です。

```java
// LangConfig javaConfig;
// NitronParser parser;
NodeTypePool types = parser.getNodeTypes();

NormalizeConfig normConfig = javaConfig.getProcessConfig().getNormalizeConfig();
AstProcessor<AstNode> normalizer = normConfig.initNormalizer(types);

SplitConfig splitConfig = javaConfig.getProcessConfig().getSplitConfig();
AstProcessor<List<? extends AstNode>> splitter = splitConfig.initSplitter(types);
```

#### 直接 AstNormalizer, AstSplitter を生成する

```java
// NitronParser parser;
NodeTypePool types = parser.getNodeTypes();

AstProcessor<AstNode> normalizer = new AstNormalizer(
    Map.of(   // 置換規則
        AstPath.of("IntegerLiteral", types), "N"
    ),
    Map.of(   // 添字付き置換規則
        AstPath.of("variableDeclaratorId", types), "V",
        AstPath.of("//expressionName/Identifier", types), "V" // XPathで指定する場合 // は省略できません
    ),
    List.of(  // 除去規則
        AstPath.of("annotation", types)
    )
);

AstProcessor<List<? extends AstNode>> splitter = new AstSplitter(
    List.of(
        types.getType("statement"),
        types.getType("methodDeclaration")
    )
);
```
