import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.diffdetective.analysis.logic.SAT;
import org.variantsync.diffdetective.variation.DiffLinesLabel;
import org.variantsync.diffdetective.variation.diff.VariationDiff;
import org.variantsync.diffdetective.diff.result.DiffParseException;
import org.variantsync.diffdetective.variation.diff.parse.VariationDiffParseOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.and;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.var;
import static org.variantsync.diffdetective.variation.diff.Time.AFTER;
import static org.variantsync.diffdetective.variation.diff.Time.BEFORE;

public class PCTest {
    private static final Node A = var("A");
    private static final Node B = var("B");
    private static final Node C = var("C");
    private static final Node D = var("D");
    private static final Node E = var("E");
    record ExpectedPC(Node before, Node after) {}
    record TestCase(Path file, Map<String, ExpectedPC> expectedResult) {
        @Override
        public String toString() {
            return file.toString();
        }
    }

    private final static Path testDir = Constants.RESOURCE_DIR.resolve("pctest");
    private final static TestCase a = new TestCase(
            Path.of("a.diff"),
            Map.of(
                    "1", new ExpectedPC(A, and(A, B)),
                    "2", new ExpectedPC(A, and(A, C, negate(B))),
                    "3", new ExpectedPC(and(A, D, E), and(A, D)),
                    "4", new ExpectedPC(A, A)
            ));
    private final static TestCase elif = new TestCase(
            Path.of("elif.diff"),
            Map.of(
                    "1", new ExpectedPC(A, A),
                    "2", new ExpectedPC(and(negate(A), B), and(negate(A), B)),
                    "3", new ExpectedPC(and(negate(A), negate(B), C), and(negate(A), B)),
                    "4", new ExpectedPC(and(negate(A), negate(B), C), and(negate(A), negate(B), D)),
                    "5", new ExpectedPC(and(negate(A), negate(B), negate(C)), and(negate(A), negate(B), negate(D)))
            ));
    private final static TestCase elze = new TestCase(
            Path.of("else.diff"),
            Map.of(
                    "1", new ExpectedPC(A, and(A, B)),
                    "2", new ExpectedPC(and(negate(A), C), and(A, negate(B), C)),
                    "3", new ExpectedPC(and(negate(A), C), and(A, negate(B), negate(C))),
                    "4", new ExpectedPC(and(negate(A), negate(C)), negate(A))
            ));

    private static String errorAt(final String node, String time, Node is, Node should) {
        return time + " PC of node \"" + node + "\" is \"" + is + "\" but expected \"" + should + "\"!";
    }

    public static List<TestCase> testCases() {
        return List.of(a, elif, elze);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void test(final TestCase testCase) throws IOException, DiffParseException {
        final Path path = testDir.resolve(testCase.file);
        final VariationDiff<DiffLinesLabel> t = VariationDiff.fromFile(path, new VariationDiffParseOptions(false, true));
        t.forAll(node -> {
           if (node.isArtifact()) {
               final String text = node.getLabel().toString().trim();
               final ExpectedPC expectedPC = testCase.expectedResult.getOrDefault(text, null);
               if (expectedPC != null) {
                   Node pc = node.getPresenceCondition(BEFORE);
                   assertTrue(
                           SAT.equivalent(pc, expectedPC.before),
                           errorAt(text, "before", pc, expectedPC.before));
                   pc = node.getPresenceCondition(AFTER);
                   assertTrue(
                           SAT.equivalent(pc, expectedPC.after),
                           errorAt(text, "after", pc, expectedPC.after));
               } else {
                   Logger.warn("No expected PC specified for node '{}'!", text);
               }
           }
        });
    }
}
