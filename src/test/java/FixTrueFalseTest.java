import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.variantsync.diffdetective.util.fide.FixTrueFalse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.variantsync.diffdetective.util.fide.FixTrueFalse.False;
import static org.variantsync.diffdetective.util.fide.FixTrueFalse.True;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.and;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.equivalent;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.implies;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.or;

public class FixTrueFalseTest {
    private record TestCase(Node formula, Node expectedResult) {}

    private final static Literal A = new Literal("A");
    private final static Literal B = new Literal("B");
    private final static Literal C = new Literal("C");
    private final static Node SomeIrreducible = and(A, implies(A, B));

    public static List<TestCase> testCases() {
        return List.of(
                new TestCase(and(True, A), A),
                new TestCase(or(False, A), A),
                new TestCase(and(False, A), False),
                new TestCase(or(True, A), True),

                new TestCase(implies(False, A), True),
                new TestCase(implies(A, False), negate(A)),
                new TestCase(implies(True, A), A),
                new TestCase(implies(A, True), True),

                new TestCase(equivalent(A, True), A),
                new TestCase(equivalent(True, A), A),
                new TestCase(equivalent(A, False), negate(A)),
                new TestCase(equivalent(False, A), negate(A)),

                new TestCase(
                        equivalent(
                                or(
                                        and(False, True, A),
                                        SomeIrreducible
                                ),
                                implies(
                                        or(False, C),
                                        negate(False)
                                )
                        ),
                        SomeIrreducible)
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void test(TestCase testCase) {
        assertEquals(FixTrueFalse.EliminateTrueAndFalse(testCase.formula).get(), testCase.expectedResult);
    }
}
