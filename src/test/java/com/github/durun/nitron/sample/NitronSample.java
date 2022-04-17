package com.github.durun.nitron.sample;

import com.github.durun.nitron.core.ast.node.AstNode;
import com.github.durun.nitron.core.ast.path.AstPath;
import com.github.durun.nitron.core.ast.processors.AstNormalizer;
import com.github.durun.nitron.core.ast.processors.AstProcessor;
import com.github.durun.nitron.core.ast.processors.AstSplitter;
import com.github.durun.nitron.core.ast.type.NodeTypePool;
import com.github.durun.nitron.core.config.LangConfig;
import com.github.durun.nitron.core.config.NitronConfig;
import com.github.durun.nitron.core.config.NormalizeConfig;
import com.github.durun.nitron.core.config.SplitConfig;
import com.github.durun.nitron.core.config.loader.NitronConfigLoader;
import com.github.durun.nitron.core.parser.NitronParser;
import com.github.durun.nitron.core.parser.antlr.AntlrParserKt;
import com.github.durun.nitron.core.parser.jdt.JdtParserKt;
import org.eclipse.jdt.core.JavaCore;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NitronSample {
  /**
   * 設定ファイルから読み込んだ Parser でパースする
   */
  static void ParserFromConfig(String src) {
    // 全体の設定 NitronConfig を読み込みます
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));

    // 言語毎の設定 LangConfig を取り出します
    // 言語一覧は config/nitron.json に定義されています
    // "java-jdt" は config/lang/java-jdt.json にあります
    LangConfig javaConfig = config.getLangConfig().get("java-jdt");

    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));
    System.out.println(ast);
  }

  /**
   * 設定ファイルから読み込んだ AstNormalizer で正規化する
   */
  static void NormalizeFromConfig(String src) {
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
    LangConfig javaConfig = config.getLangConfig().get("java-jdt");
    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    NodeTypePool types = parser.getNodeTypes();
    // AstNormalizer を初期化します
    // NormalizeConfig に正規化用の設定が格納されています
    NormalizeConfig normConfig = javaConfig.getProcessConfig().getNormalizeConfig();
    AstProcessor<AstNode> normalizer = normConfig.initNormalizer(types);

    // 正規化を実行します
    AstNode normalizedAst = normalizer.process(ast);
    // 除去規則により ASTの根 が除去された場合、nullが返ります
    if (normalizedAst != null) {
      System.out.println(normalizedAst.getText());
    } else {
      System.out.println("null");
    }
  }

  /**
   * 設定ファイルから読み込んだ AstSplitter で分割する
   */
  static void SplitFromConfig(String src) {
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
    LangConfig javaConfig = config.getLangConfig().get("java-jdt");
    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    NodeTypePool types = parser.getNodeTypes();
    // AstSplitter を初期化します
    // SplitConfig に正規化用の設定が格納されています
    SplitConfig splitConfig = javaConfig.getProcessConfig().getSplitConfig();
    AstProcessor<List<? extends AstNode>> splitter = splitConfig.initSplitter(types);

    // 分割を実行します
    List<? extends AstNode> statements = splitter.process(ast);
    System.out.println(statements);
  }


  /**
   * 直接 JDT Parser を設定してパースする
   */
  static void JDTParser(String src) {
    NitronParser parser = JdtParserKt.init(JavaCore.VERSION_16);
    AstNode ast = parser.parse(new StringReader(src));
    System.out.println(ast);
  }

  /**
   * 直接 ANTLR を設定してパースする
   */
  static void ANTLRParser(String src) {
    // 各種設定を入れてANTLRパーサを生成します
    NitronParser parser = AntlrParserKt.fromPaths(
            "java",
            "compilationUnit",  // 翻訳単位の非終端記号名
            List.of(  // 文法ファイル
                    Path.of("config/grammars/java/java8/Java8Lexer.g4"),
                    Path.of("config/grammars/java/java8/Java8Parser.g4")
            ),
            List.of() // 必要なjavaファイル (この例では無し)
    );
      AstNode ast = parser.parse(new StringReader(src));
    System.out.println(ast);
  }

  /**
   * AstNormalizer を直接設定してASTを正規化する
   */
  static void NormalizeAst(String src) {
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
    LangConfig javaConfig = config.getLangConfig().get("java");
    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    NodeTypePool types = parser.getNodeTypes();

    // AstNormalizer を設定します
    AstProcessor<AstNode> normalizer = new AstNormalizer(
            Map.of( // 置換規則
                    AstPath.of("IntegerLiteral", types), "N"
            ),
            Map.of( // 添字付き置換規則
                    AstPath.of("variableDeclaratorId", types), "V",
                    AstPath.of("//expressionName/Identifier", types), "V" // XPathで指定する場合、//は省略できません
            ),
            List.of(// 除去規則
                    AstPath.of("annotation", types)
            )
    );

    // 正規化を実行します
    AstNode normalizedAst = normalizer.process(ast);
    // 除去規則により ASTの根 が除去された場合、nullが返ります
    if (normalizedAst != null) {
      System.out.println(normalizedAst.getText());
    } else {
      System.out.println("null");
    }
  }

  /**
   * AstSplitter を直接設定してASTを分割する
   */
  static void SplitAst(String src) {
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
    LangConfig javaConfig = config.getLangConfig().get("java");
    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    NodeTypePool types = parser.getNodeTypes();

    // AstSplitter を設定します
    AstProcessor<List<? extends AstNode>> splitter = new AstSplitter(
            List.of(
                    types.getType("statement"),
                    types.getType("methodDeclaration")
            )
    );

    // 分割を実行します
    List<? extends AstNode> statements = splitter.process(ast);
    System.out.println(statements);
  }

  static void SplitAndNormalize(String src) {
    NitronConfig config = NitronConfigLoader.INSTANCE.load(Path.of("config/nitron.json"));
    LangConfig javaConfig = config.getLangConfig().get("java-jdt");
    NitronParser parser = javaConfig.getParserConfig().getParser();
    AstNode ast = parser.parse(new StringReader(src));

    // 設定には、非終端記号・終端記号の一覧 NodeTypePool が必要になります
    NodeTypePool types = parser.getNodeTypes();
    // AstNormalizer を初期化します
    NormalizeConfig normConfig = javaConfig.getProcessConfig().getNormalizeConfig();
    AstProcessor<AstNode> normalizer = normConfig.initNormalizer(types);
    // AstSplitter を初期化します
    SplitConfig splitConfig = javaConfig.getProcessConfig().getSplitConfig();
    AstProcessor<List<? extends AstNode>> splitter = splitConfig.initSplitter(types);

    // 分割・正規化を組み合わせる場合、通常は分割を先に行います。
    var statements = splitter.process(ast);
    var normalizedStatements = statements.stream()
            .map(s -> normalizer.process(s))
            .filter(s -> Objects.nonNull(s))
            .collect(Collectors.toList());
    System.out.println(normalizedStatements);
  }

  public static void main(String[] args) {
    String src =
            "public class NitronSample {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        int x = 1;\n" +
                    "        System.out.println(\"Hello!\");\n" +
                    "    }\n" +
                    "}";
    ParserFromConfig(src);
    NormalizeFromConfig(src);
    SplitFromConfig(src);
    JDTParser(src);
    ANTLRParser(src);
    NormalizeAst(src);
    SplitAst(src);
    SplitAndNormalize(src);
  }
}
