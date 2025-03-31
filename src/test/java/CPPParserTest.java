import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.prop4j.And;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;
import org.prop4j.Or;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.cpp.CPPDiffLineFormulaExtractor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CPPParserTest {
    private static record TestCase(String formula, Node expected) {
    }

    private static record ThrowingTestCase(String formula) {
    }

    private static List<TestCase> testCases() {
        return List.of(
                new TestCase("#if A", new Literal("A")),
                new TestCase("#ifdef A", new Literal("defined(A)")),
                new TestCase("#ifndef A", new Literal("defined(A)", false)),
                new TestCase("#elif A", new Literal("A")),

                new TestCase("#if !A", new Not(new Literal("A"))),
                new TestCase("#if A && B", new And(new Literal("A"), new Literal("B"))),
                new TestCase("#if A || B", new Or(new Literal("A"), new Literal("B"))),
                new TestCase("#if A && (B || C)", new And(new Literal("A"), new Or(new Literal("B"), new Literal("C")))),
                new TestCase("#if A && B || C", new Or(new And(new Literal("A"), new Literal("B")), new Literal("C"))),

                new TestCase("#if 1 > -42", new Literal("1>-42")),
                new TestCase("#if 1 > +42", new Literal("1>+42")),
                new TestCase("#if 42 > A", new Literal("42>A")),
                new TestCase("#if 42 > ~A", new Literal("42>~A")),
                new TestCase("#if A + B > 42", new Literal("A+B>42")),
                new TestCase("#if A << B", new Literal("A<<B")),
                new TestCase("#if A ? B : C", new Literal("A?B:C")),
                new TestCase("#if A >= B && C > D", new And(new Literal("A>=B"), new Literal("C>D"))),
                new TestCase("#if A * (B + C)", new Literal("A*(B+C)")),
                new TestCase("#if defined(A) && (B * 2) > C", new And(new Literal("defined(A)"), new Literal("(B*2)>C"))),
                new TestCase("#if(STDC == 1) && (defined(LARGE) || defined(COMPACT))", new And(new Literal("STDC==1"), new Or(new Literal("defined(LARGE)"), new Literal("defined(COMPACT)")))),
                new TestCase("#if (('Z' - 'A') == 25)", new Literal("('Z'-'A')==25")),
                new TestCase("#if APR_CHARSET_EBCDIC && !(('Z' - 'A') == 25)", new And(new Literal("APR_CHARSET_EBCDIC"), new Not(new Literal("('Z'-'A')==25")))),
                new TestCase("# if ((GNUTLS_VERSION_MAJOR + (GNUTLS_VERSION_MINOR > 0 || GNUTLS_VERSION_PATCH >= 20)) > 3)",
                        new Literal("(GNUTLS_VERSION_MAJOR+(GNUTLS_VERSION_MINOR>0||GNUTLS_VERSION_PATCH>=20))>3")),

                new TestCase("#if A && (B > C)", new And(new Literal("A"), new Literal("B>C"))),
                new TestCase("#if (A && B) > C", new Literal("(A&&B)>C")),
                new TestCase("#if C == (A || B)", new Literal("C==(A||B)")),
                new TestCase("#if ((A && B) > C)", new Literal("(A&&B)>C")),
                new TestCase("#if A && ((B + 1) > (C || D))", new And(new Literal("A"), new Literal("(B+1)>(C||D)"))),

                new TestCase("#if __has_include", new Literal("__has_include")),
                new TestCase("#if defined __has_include", new Literal("defined(__has_include)")),
                new TestCase("#if __has_include(<nss3/nss.h>)", new Literal("__has_include(<nss3/nss.h>)")),
                new TestCase("#if __has_include(<nss.h>)", new Literal("__has_include(<nss.h>)")),
                new TestCase("#if __has_include(\"nss3/nss.h\")", new Literal("__has_include(\"nss3/nss.h\")")),
                new TestCase("#if __has_include(\"nss.h\")", new Literal("__has_include(\"nss.h\")")),

                new TestCase("#if __has_attribute", new Literal("__has_attribute")),
                new TestCase("#if defined __has_attribute", new Literal("defined(__has_attribute)")),
                new TestCase("#  if __has_attribute (nonnull)", new Literal("__has_attribute(nonnull)")),
                new TestCase("#if defined __has_attribute && __has_attribute (nonnull)", new And(new Literal("defined(__has_attribute)"), new Literal("__has_attribute(nonnull)"))),

                new TestCase("#if __has_cpp_attribute", new Literal("__has_cpp_attribute")),
                new TestCase("#if defined __has_cpp_attribute", new Literal("defined(__has_cpp_attribute)")),
                new TestCase("#if __has_cpp_attribute (nonnull)", new Literal("__has_cpp_attribute(nonnull)")),
                new TestCase("#if __has_cpp_attribute (nonnull) && A", new And(new Literal("__has_cpp_attribute(nonnull)"), new Literal("A"))),

                new TestCase("#if defined __has_c_attribute", new Literal("defined(__has_c_attribute)")),
                new TestCase("#if __has_c_attribute", new Literal("__has_c_attribute")),
                new TestCase("#if __has_c_attribute (nonnull)", new Literal("__has_c_attribute(nonnull)")),
                new TestCase("#if __has_c_attribute (nonnull) && A", new And(new Literal("__has_c_attribute(nonnull)"), new Literal("A"))),

                new TestCase("#if defined __has_builtin", new Literal("defined(__has_builtin)")),
                new TestCase("#if __has_builtin", new Literal("__has_builtin")),
                new TestCase("#if __has_builtin (__nonnull)", new Literal("__has_builtin(__nonnull)")),
                new TestCase("#if __has_builtin (nonnull) && A", new And(new Literal("__has_builtin(nonnull)"), new Literal("A"))),

                new TestCase("#if A // Comment && B", new Literal("A")),
                new TestCase("#if A /* Comment */ && B", new And(new Literal("A"), new Literal("B"))),
                new TestCase("#if A && B /* Multiline Comment", new And(new Literal("A"), new Literal("B"))),

                new TestCase("#if A == B", new Literal("A==B")),
                new TestCase("#if A == 1", new Literal("A==1")),

                new TestCase("#if defined A", new Literal("defined(A)")),
                new TestCase("#if defined(A)", new Literal("defined(A)")),
                new TestCase("#if defined (A)", new Literal("defined(A)")),
                new TestCase("#if defined ( A )", new Literal("defined(A)")),
                new TestCase("#if (defined A)", new Literal("defined(A)")),
                new TestCase("#if MACRO (A)", new Literal("MACRO(A)")),
                new TestCase("#if MACRO (A, B)", new Literal("MACRO(A,B)")),
                new TestCase("#if MACRO (A, B + C)", new Literal("MACRO(A,B+C)")),
                new TestCase("#if MACRO (A, B) == 1", new Literal("MACRO(A,B)==1")),

                new TestCase("#if ifndef", new Literal("ifndef")),

                new TestCase("#if __has_include_next(<some-header.h>)", new Literal("__has_include_next(<some-header.h>)")),
                new TestCase("#if __is_target_arch(x86)", new Literal("__is_target_arch(x86)")),
                new TestCase("#if A || (defined(NAME) && (NAME >= 199630))", new Or(new Literal("A"), new And(new Literal("defined(NAME)"), new Literal("NAME>=199630")))),
                new TestCase("#if MACRO(part:part)", new Literal("MACRO(part:part)")),
                new TestCase("#if MACRO(x=1)", new Literal("MACRO(x=1)")),
                new TestCase("#if A = 3", new Literal("A=3")),
                new TestCase("#if ' ' == 32", new Literal("' '==32")),
                new TestCase("#if (NAME<<1) > (1<<BITS)", new Literal("(NAME<<1)>(1<<BITS)")),
                new TestCase("#if #cpu(sparc)", new Literal("cpu(sparc)")),
                new TestCase("#ifdef \\U0001000", new Literal("defined(\\U0001000)")),
                new TestCase("#if (defined(NAME) && (NAME >= 199905) && (NAME < 1991011)) ||     (NAME >= 300000) || defined(NAME)",
                        new Or(new And(new Literal("defined(NAME)"), new Literal("NAME>=199905"), new Literal("NAME<1991011")), new Literal("NAME>=300000"), new Literal("defined(NAME)"))),
                new TestCase("#if __has_warning(\"-Wa-warning\"_foo)", new Literal("__has_warning(\"-Wa-warning\"_foo)")),

                new TestCase("#if A && (B - (C || D))", new And(new Literal("A"), new Literal("B-(C||D)"))),
                new TestCase("#if A == '1'", new Literal("A=='1'"))
        );
    }

    private static List<ThrowingTestCase> throwingTestCases() {
        return List.of(
                // Invalid macro
                new ThrowingTestCase(""),
                new ThrowingTestCase("#"),
                new ThrowingTestCase("ifdef A"),
                new ThrowingTestCase("#error A"),
                new ThrowingTestCase("#iferror A"),

                // Empty formula
                new ThrowingTestCase("#ifdef"),
                new ThrowingTestCase("#ifdef // Comment"),
                new ThrowingTestCase("#ifdef /* Comment */")
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCase(TestCase testCase) throws UnparseableFormulaException {
        assertEquals(
                testCase.expected,
                new CPPDiffLineFormulaExtractor().extractFormula(testCase.formula())
        );
    }

    @ParameterizedTest
    @MethodSource("throwingTestCases")
    public void throwingTestCase(ThrowingTestCase testCase) {
        assertThrows(UnparseableFormulaException.class, () ->
                new CPPDiffLineFormulaExtractor().extractFormula(testCase.formula)
        );
    }
}
