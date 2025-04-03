import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.prop4j.Node;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.cpp.CPPDiffLineFormulaExtractor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.and;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.or;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.var;
import static org.variantsync.diffdetective.util.fide.FormulaUtils.negate;

public class CPPParserTest {
    private static record TestCase(String formula, Node expected) {
    }

    private static record ThrowingTestCase(String formula) {
    }

    private static List<TestCase> testCases() {
        return List.of(
                new TestCase("#if A", var("A")),
                new TestCase("#ifdef A", var("defined(A)")),
                new TestCase("#ifndef A", negate(var("defined(A)"))),
                new TestCase("#elif A", var("A")),

                new TestCase("#if !A", negate(var("A"))),
                new TestCase("#if A && B", and(var("A"), var("B"))),
                new TestCase("#if A || B", or(var("A"), var("B"))),
                new TestCase("#if A && (B || C)", and(var("A"), or(var("B"), var("C")))),
                new TestCase("#if A && B || C", or(and(var("A"), var("B")), var("C"))),

                new TestCase("#if 1 > -42", var("1>-42")),
                new TestCase("#if 1 > +42", var("1>+42")),
                new TestCase("#if 42 > A", var("42>A")),
                new TestCase("#if 42 > ~A", var("42>~A")),
                new TestCase("#if A + B > 42", var("A+B>42")),
                new TestCase("#if A << B", var("A<<B")),
                new TestCase("#if A ? B : C", var("A?B:C")),
                new TestCase("#if A >= B && C > D", and(var("A>=B"), var("C>D"))),
                new TestCase("#if A * (B + C)", var("A*(B+C)")),
                new TestCase("#if defined(A) && (B * 2) > C", and(var("defined(A)"), var("(B*2)>C"))),
                new TestCase("#if(STDC == 1) && (defined(LARGE) || defined(COMPACT))", and(var("STDC==1"), or(var("defined(LARGE)"), var("defined(COMPACT)")))),
                new TestCase("#if (('Z' - 'A') == 25)", var("('Z'-'A')==25")),
                new TestCase("#if APR_CHARSET_EBCDIC && !(('Z' - 'A') == 25)", and(var("APR_CHARSET_EBCDIC"), negate(var("('Z'-'A')==25")))),
                new TestCase("# if ((GNUTLS_VERSION_MAJOR + (GNUTLS_VERSION_MINOR > 0 || GNUTLS_VERSION_PATCH >= 20)) > 3)",
                        var("(GNUTLS_VERSION_MAJOR+(GNUTLS_VERSION_MINOR>0||GNUTLS_VERSION_PATCH>=20))>3")),

                new TestCase("#if A && (B > C)", and(var("A"), var("B>C"))),
                new TestCase("#if (A && B) > C", var("(A&&B)>C")),
                new TestCase("#if C == (A || B)", var("C==(A||B)")),
                new TestCase("#if ((A && B) > C)", var("(A&&B)>C")),
                new TestCase("#if A && ((B + 1) > (C || D))", and(var("A"), var("(B+1)>(C||D)"))),

                new TestCase("#if __has_include", var("__has_include")),
                new TestCase("#if defined __has_include", var("defined(__has_include)")),
                new TestCase("#if __has_include(<nss3/nss.h>)", var("__has_include(<nss3/nss.h>)")),
                new TestCase("#if __has_include(<nss.h>)", var("__has_include(<nss.h>)")),
                new TestCase("#if __has_include(\"nss3/nss.h\")", var("__has_include(\"nss3/nss.h\")")),
                new TestCase("#if __has_include(\"nss.h\")", var("__has_include(\"nss.h\")")),

                new TestCase("#if __has_attribute", var("__has_attribute")),
                new TestCase("#if defined __has_attribute", var("defined(__has_attribute)")),
                new TestCase("#  if __has_attribute (nonnull)", var("__has_attribute(nonnull)")),
                new TestCase("#if defined __has_attribute && __has_attribute (nonnull)", and(var("defined(__has_attribute)"), var("__has_attribute(nonnull)"))),

                new TestCase("#if __has_cpp_attribute", var("__has_cpp_attribute")),
                new TestCase("#if defined __has_cpp_attribute", var("defined(__has_cpp_attribute)")),
                new TestCase("#if __has_cpp_attribute (nonnull)", var("__has_cpp_attribute(nonnull)")),
                new TestCase("#if __has_cpp_attribute (nonnull) && A", and(var("__has_cpp_attribute(nonnull)"), var("A"))),

                new TestCase("#if defined __has_c_attribute", var("defined(__has_c_attribute)")),
                new TestCase("#if __has_c_attribute", var("__has_c_attribute")),
                new TestCase("#if __has_c_attribute (nonnull)", var("__has_c_attribute(nonnull)")),
                new TestCase("#if __has_c_attribute (nonnull) && A", and(var("__has_c_attribute(nonnull)"), var("A"))),

                new TestCase("#if defined __has_builtin", var("defined(__has_builtin)")),
                new TestCase("#if __has_builtin", var("__has_builtin")),
                new TestCase("#if __has_builtin (__nonnull)", var("__has_builtin(__nonnull)")),
                new TestCase("#if __has_builtin (nonnull) && A", and(var("__has_builtin(nonnull)"), var("A"))),

                new TestCase("#if A // Comment && B", var("A")),
                new TestCase("#if A /* Comment */ && B", and(var("A"), var("B"))),
                new TestCase("#if A && B /* Multiline Comment", and(var("A"), var("B"))),

                new TestCase("#if A == B", var("A==B")),
                new TestCase("#if A == 1", var("A==1")),

                new TestCase("#if defined A", var("defined(A)")),
                new TestCase("#if defined(A)", var("defined(A)")),
                new TestCase("#if defined (A)", var("defined(A)")),
                new TestCase("#if defined ( A )", var("defined(A)")),
                new TestCase("#if (defined A)", var("defined(A)")),
                new TestCase("#if MACRO (A)", var("MACRO(A)")),
                new TestCase("#if MACRO (A, B)", var("MACRO(A,B)")),
                new TestCase("#if MACRO (A, B + C)", var("MACRO(A,B+C)")),
                new TestCase("#if MACRO (A, B) == 1", var("MACRO(A,B)==1")),

                new TestCase("#if ifndef", var("ifndef")),

                new TestCase("#if __has_include_next(<some-header.h>)", var("__has_include_next(<some-header.h>)")),
                new TestCase("#if __is_target_arch(x86)", var("__is_target_arch(x86)")),
                new TestCase("#if A || (defined(NAME) && (NAME >= 199630))", or(var("A"), and(var("defined(NAME)"), var("NAME>=199630")))),
                new TestCase("#if MACRO(part:part)", var("MACRO(part:part)")),
                new TestCase("#if MACRO(x=1)", var("MACRO(x=1)")),
                new TestCase("#if A = 3", var("A=3")),
                new TestCase("#if ' ' == 32", var("' '==32")),
                new TestCase("#if (NAME<<1) > (1<<BITS)", var("(NAME<<1)>(1<<BITS)")),
                new TestCase("#if #cpu(sparc)", var("cpu(sparc)")),
                new TestCase("#ifdef \\U0001000", var("defined(\\U0001000)")),
                new TestCase("#if (defined(NAME) && (NAME >= 199905) && (NAME < 1991011)) ||     (NAME >= 300000) || defined(NAME)",
                        or(and(var("defined(NAME)"), var("NAME>=199905"), var("NAME<1991011")), var("NAME>=300000"), var("defined(NAME)"))),
                new TestCase("#if __has_warning(\"-Wa-warning\"_foo)", var("__has_warning(\"-Wa-warning\"_foo)")),

                new TestCase("#if A && (B - (C || D))", and(var("A"), var("B-(C||D)"))),
                new TestCase("#if A == '1'", var("A=='1'"))
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
