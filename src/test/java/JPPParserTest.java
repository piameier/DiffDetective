import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.prop4j.And;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Or;
import org.variantsync.diffdetective.diff.result.DiffParseException;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.PreprocessorAnnotationParser;
import org.variantsync.diffdetective.feature.jpp.JPPDiffLineFormulaExtractor;
import org.variantsync.diffdetective.util.IO;
import org.variantsync.diffdetective.variation.DiffLinesLabel;
import org.variantsync.diffdetective.variation.diff.VariationDiff;
import org.variantsync.diffdetective.variation.diff.parse.VariationDiffParseOptions;
import org.variantsync.diffdetective.variation.diff.parse.VariationDiffParser;
import org.variantsync.diffdetective.variation.diff.serialize.Format;
import org.variantsync.diffdetective.variation.diff.serialize.LineGraphExporter;
import org.variantsync.diffdetective.variation.diff.serialize.edgeformat.ChildOrderEdgeFormat;
import org.variantsync.diffdetective.variation.diff.serialize.nodeformat.FullNodeFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.variantsync.diffdetective.util.Assert.fail;

// Test cases for a parser of https://www.slashdev.ca/javapp/
public class JPPParserTest {
    private record TestCase<Input, Expected>(Input input, Expected expected) {
    }

    private record ThrowingTestCase(String formula) {
    }

    private static List<JPPParserTest.TestCase<String, Node>> abstractionTests() {
        return List.of(
                /// #if expression
                // expression := <operand> <operator> <operand> | [!]defined(name)
                // expression := operand == operand
                new JPPParserTest.TestCase<>("//#if 1 == -42", new Literal("1==-42")),
                // expression := operand != operand
                new JPPParserTest.TestCase<>("// #if 1 != 0", new Literal("1!=0")),
                // expression := operand <= operand
                new JPPParserTest.TestCase<>("//#if -1 <= 0", new Literal("-1<=0")),
                // expression := operand < operand
                new JPPParserTest.TestCase<>("//#if \"str\" < 0", new Literal("\"str\"<0")),
                // expression := operand >= operand
                new JPPParserTest.TestCase<>("//   #if \"str\" >= \"str\"", new Literal("\"str\">=\"str\"")),
                // expression := operand > operand
                new JPPParserTest.TestCase<>("//  #if 1.2 > 0", new Literal("1.2>0")),
                // expression := defined(name)
                new JPPParserTest.TestCase<>("//#if defined(property)", new Literal("defined(property)")),
                // expression := !defined(name)
                new JPPParserTest.TestCase<>("//#if !defined(property)", new Literal("defined(property)", false)),
                // operand := ${property}
                new JPPParserTest.TestCase<>("//#if ${os_version} == 4.1", new Literal("${os_version}==4.1")),

                /// #if expression and expression
                new JPPParserTest.TestCase<>("//#if 1 > 2 and defined( FEAT_A  )", new And(new Literal("1>2"), new Literal("defined(FEAT_A)"))),

                /// #if expression or expression
                new JPPParserTest.TestCase<>("//#if !defined(left) or defined(right)", new Or(new Literal("defined(left)", false), new Literal("defined(right)"))),

                /// #if expression and expression or expression
                new JPPParserTest.TestCase<>("//#if ${os_version} == 4.1 and 1 > -42 or defined(ALL)", new Or(new And(new Literal("${os_version}==4.1"), new Literal("1>-42")), new Literal("defined(ALL)"))),

                /// #if "string with whitespace"
                new JPPParserTest.TestCase<>("//#if ${ test } == \"a b\"", new Literal("${test}==\"a b\""))
        );
    }

    private static List<JPPParserTest.ThrowingTestCase> throwingTestCases() {
        return List.of(
                // Invalid macro
                new JPPParserTest.ThrowingTestCase(""),
                new JPPParserTest.ThrowingTestCase("#"),
                new JPPParserTest.ThrowingTestCase("ifdef A"),
                new JPPParserTest.ThrowingTestCase("#error A"),
                new JPPParserTest.ThrowingTestCase("#iferror A"),

                // Empty formula
                new JPPParserTest.ThrowingTestCase("//#if"),
                new JPPParserTest.ThrowingTestCase("#if defined()"),
                new JPPParserTest.ThrowingTestCase("#if ${} > 0"),

                // incomplete expressions
                new JPPParserTest.ThrowingTestCase("#if 1 >"),
                new JPPParserTest.ThrowingTestCase("#if  == 2"),
                new JPPParserTest.ThrowingTestCase("#if  ${version} > ")
        );
    }

    private static List<JPPParserTest.TestCase<Path, Path>> fullDiffTests() {
        final Path basePath = Path.of("src", "test", "resources", "diffs", "jpp");
        return List.of(
                new JPPParserTest.TestCase<>(basePath.resolve("basic_jpp.diff"), basePath.resolve("basic_jpp_expected.lg"))
        );
    }

    @ParameterizedTest
    @MethodSource("abstractionTests")
    public void testCase(JPPParserTest.TestCase<String, String> testCase) throws UnparseableFormulaException {
        assertEquals(
                testCase.expected,
                new JPPDiffLineFormulaExtractor().extractFormula(testCase.input())
        );
    }

    @ParameterizedTest
    @MethodSource("throwingTestCases")
    public void throwingTestCase(JPPParserTest.ThrowingTestCase testCase) {
        assertThrows(UnparseableFormulaException.class, () ->
                new JPPDiffLineFormulaExtractor().extractFormula(testCase.formula)
        );
    }

    @ParameterizedTest
    @MethodSource("fullDiffTests")
    public void fullDiffTestCase(JPPParserTest.TestCase<Path, Path> testCase) throws IOException, DiffParseException {
        VariationDiff<DiffLinesLabel> variationDiff;
        try (var inputFile = Files.newBufferedReader(testCase.input)) {
            variationDiff = VariationDiffParser.createVariationDiff(
                    inputFile,
                    new VariationDiffParseOptions(
                            false,
                            false
                    ).withAnnotationParser(PreprocessorAnnotationParser.JPPAnnotationParser)
            );
        }

        Path actualPath = testCase.input.getParent().resolve(testCase.input.getFileName() + "_actual");
        try (var output = IO.newBufferedOutputStream(actualPath)) {
            new LineGraphExporter<>(new Format<>(new FullNodeFormat(), new ChildOrderEdgeFormat<>()))
                    .exportVariationDiff(variationDiff, output);
        }

        try (
                var expectedFile = Files.newBufferedReader(testCase.expected);
                var actualFile = Files.newBufferedReader(actualPath);
        ) {
            if (IOUtils.contentEqualsIgnoreEOL(expectedFile, actualFile)) {
                // Delete output files if the test succeeded
                Files.delete(actualPath);
            } else {
                // Keep output files if the test failed
                fail("The VariationDiff in file " + testCase.input + " didn't parse correctly. "
                        + "Expected the content of " + testCase.expected + " but got the content of " + actualPath + ". ");
            }
        }
    }

}
