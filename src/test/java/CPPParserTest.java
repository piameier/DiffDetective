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
                new TestCase("#ifdef A", new Literal("A")),
                new TestCase("#ifndef A", new Not(new Literal("A"))),
                new TestCase("#elif A", new Literal("A")),

                new TestCase("#if !A", new Literal("A", false)),
                new TestCase("#if A && B", new And(new Literal("A"), new Literal("B"))),
                new TestCase("#if A || B", new Or(new Literal("A"), new Literal("B"))),
                new TestCase("#if A && (B || C)", new And(new Literal("A"), new Or(new Literal("B"), new Literal("C")))),
                new TestCase("#if A && B || C", new Or(new And(new Literal("A"), new Literal("B")), new Literal("C"))),

                new TestCase("#if 1 > -42", new Literal("1__GT____U_MINUS__42")),
                new TestCase("#if 1 > +42", new Literal("1__GT____U_PLUS__42")),
                new TestCase("#if 42 > A", new Literal("42__GT__A")),
                new TestCase("#if 42 > ~A", new Literal("42__GT____U_TILDE__A")),
                new TestCase("#if A + B > 42", new Literal("A__ADD__B__GT__42")),
                new TestCase("#if A << B", new Literal("A__LSHIFT__B")),
                new TestCase("#if A ? B : C", new Literal("A__THEN__B__COLON__C")),
                new TestCase("#if A >= B && C > D", new And(new Literal("A__GEQ__B"), new Literal("C__GT__D"))),
                new TestCase("#if A * (B + C)", new Literal("A__MUL____LB__B__ADD__C__RB__")),
                new TestCase("#if defined(A) && (B * 2) > C", new And(new Literal("DEFINED___LB__A__RB__"), new Literal("__LB__B__MUL__2__RB____GT__C"))),
                new TestCase("#if(STDC == 1) && (defined(LARGE) || defined(COMPACT))", new And(new Literal("STDC__EQ__1"), new Or(new Literal("DEFINED___LB__LARGE__RB__"), new Literal("DEFINED___LB__COMPACT__RB__")))),
                new TestCase("#if (('Z' - 'A') == 25)", new Literal("__LB____SQUOTE__Z__SQUOTE____SUB____SQUOTE__A__SQUOTE____RB____EQ__25")),
                new TestCase("#if APR_CHARSET_EBCDIC && !(('Z' - 'A') == 25)", new And(new Literal("APR_CHARSET_EBCDIC"), new Literal("__LB____SQUOTE__Z__SQUOTE____SUB____SQUOTE__A__SQUOTE____RB____EQ__25", false))),
                new TestCase("# if ((GNUTLS_VERSION_MAJOR + (GNUTLS_VERSION_MINOR > 0 || GNUTLS_VERSION_PATCH >= 20)) > 3)",
                        new Literal("__LB__GNUTLS_VERSION_MAJOR__ADD____LB__GNUTLS_VERSION_MINOR__GT__0__L_OR__GNUTLS_VERSION_PATCH__GEQ__20__RB____RB____GT__3")),

                new TestCase("#if A && (B > C)", new And(new Literal("A"), new Literal("B__GT__C"))),
                new TestCase("#if (A && B) > C", new Literal("__LB__A__L_AND__B__RB____GT__C")),
                new TestCase("#if C == (A || B)", new Literal("C__EQ____LB__A__L_OR__B__RB__")),
                new TestCase("#if ((A && B) > C)", new Literal("__LB__A__L_AND__B__RB____GT__C")),
                new TestCase("#if A && ((B + 1) > (C || D))", new And(new Literal("A"), new Literal("__LB__B__ADD__1__RB____GT____LB__C__L_OR__D__RB__"))),

                new TestCase("#if __has_include", new Literal("HAS_INCLUDE_")),
                new TestCase("#if defined __has_include", new Literal("DEFINED_HAS_INCLUDE_")),
                new TestCase("#if __has_include(<nss3/nss.h>)", new Literal("HAS_INCLUDE___LB____LT__nss3__DIV__nss__DOT__h__GT____RB__")),
                new TestCase("#if __has_include(<nss.h>)", new Literal("HAS_INCLUDE___LB____LT__nss__DOT__h__GT____RB__")),
                new TestCase("#if __has_include(\"nss3/nss.h\")", new Literal("HAS_INCLUDE___LB____QUOTE__nss3__DIV__nss__DOT__h__QUOTE____RB__")),
                new TestCase("#if __has_include(\"nss.h\")", new Literal("HAS_INCLUDE___LB____QUOTE__nss__DOT__h__QUOTE____RB__")),

                new TestCase("#if __has_attribute", new Literal("HAS_ATTRIBUTE_")),
                new TestCase("#if defined __has_attribute", new Literal("DEFINED_HAS_ATTRIBUTE_")),
                new TestCase("#  if __has_attribute (nonnull)", new Literal("HAS_ATTRIBUTE___LB__nonnull__RB__")),
                new TestCase("#if defined __has_attribute && __has_attribute (nonnull)", new And(new Literal("DEFINED_HAS_ATTRIBUTE_"), new Literal("HAS_ATTRIBUTE___LB__nonnull__RB__"))),

                new TestCase("#if __has_cpp_attribute", new Literal("HAS_CPP_ATTRIBUTE_")),
                new TestCase("#if defined __has_cpp_attribute", new Literal("DEFINED_HAS_CPP_ATTRIBUTE_")),
                new TestCase("#if __has_cpp_attribute (nonnull)", new Literal("HAS_CPP_ATTRIBUTE___LB__nonnull__RB__")),
                new TestCase("#if __has_cpp_attribute (nonnull) && A", new And(new Literal("HAS_CPP_ATTRIBUTE___LB__nonnull__RB__"), new Literal("A"))),

                new TestCase("#if defined __has_c_attribute", new Literal("DEFINED_HAS_C_ATTRIBUTE_")),
                new TestCase("#if __has_c_attribute", new Literal("HAS_C_ATTRIBUTE_")),
                new TestCase("#if __has_c_attribute (nonnull)", new Literal("HAS_C_ATTRIBUTE___LB__nonnull__RB__")),
                new TestCase("#if __has_c_attribute (nonnull) && A", new And(new Literal("HAS_C_ATTRIBUTE___LB__nonnull__RB__"), new Literal("A"))),

                new TestCase("#if defined __has_builtin", new Literal("DEFINED_HAS_BUILTIN_")),
                new TestCase("#if __has_builtin", new Literal("HAS_BUILTIN_")),
                new TestCase("#if __has_builtin (__nonnull)", new Literal("HAS_BUILTIN___LB____nonnull__RB__")),
                new TestCase("#if __has_builtin (nonnull) && A", new And(new Literal("HAS_BUILTIN___LB__nonnull__RB__"), new Literal("A"))),

                new TestCase("#if A // Comment && B", new Literal("A")),
                new TestCase("#if A /* Comment */ && B", new And(new Literal("A"), new Literal("B"))),
                new TestCase("#if A && B /* Multiline Comment", new And(new Literal("A"), new Literal("B"))),

                new TestCase("#if A == B", new Literal("A__EQ__B")),
                new TestCase("#if A == 1", new Literal("A__EQ__1")),

                new TestCase("#if defined A", new Literal("DEFINED_A")),
                new TestCase("#if defined(A)", new Literal("DEFINED___LB__A__RB__")),
                new TestCase("#if defined (A)", new Literal("DEFINED___LB__A__RB__")),
                new TestCase("#if defined ( A )", new Literal("DEFINED___LB__A__RB__")),
                new TestCase("#if (defined A)", new Literal("DEFINED_A")),
                new TestCase("#if MACRO (A)", new Literal("MACRO___LB__A__RB__")),
                new TestCase("#if MACRO (A, B)", new Literal("MACRO___LB__A__B__RB__")),
                new TestCase("#if MACRO (A, B + C)", new Literal("MACRO___LB__A__B__ADD__C__RB__")),
                new TestCase("#if MACRO (A, B) == 1", new Literal("MACRO___LB__A__B__RB____EQ__1")),

                new TestCase("#if ifndef", new Literal("ifndef")),

                new TestCase("#if __has_include_next(<some-header.h>)", new Literal("__HAS_INCLUDE_NEXT___LB____LT__some__SUB__header__DOT__h__GT____RB__")),
                new TestCase("#if __is_target_arch(x86)", new Literal("__IS_TARGET_ARCH___LB__x86__RB__")),
                new TestCase("#if A || (defined(NAME) && (NAME >= 199630))", new Or(new Literal("A"), new And(new Literal("DEFINED___LB__NAME__RB__"), new Literal("NAME__GEQ__199630")))),
                new TestCase("#if MACRO(part:part)", new Literal("MACRO___LB__part__COLON__part__RB__")),
                new TestCase("#if MACRO(x=1)", new Literal("MACRO___LB__x__ASSIGN__1__RB__")),
                new TestCase("#if A = 3", new Literal("A__ASSIGN__3")),
                new TestCase("#if ' ' == 32", new Literal("__SQUOTE_____SQUOTE____EQ__32")),
                new TestCase("#if (NAME<<1) > (1<<BITS)", new Literal("__LB__NAME__LSHIFT__1__RB____GT____LB__1__LSHIFT__BITS__RB__")),
                new TestCase("#if #cpu(sparc)", new Literal("CPU___LB__sparc__RB__")),
                new TestCase("#ifdef \\U0001000", new Literal("__B_SLASH__U0001000")),
                new TestCase("#if (defined(NAME) && (NAME >= 199905) && (NAME < 1991011)) ||     (NAME >= 300000) || defined(NAME)", new Or(new And(new Literal("DEFINED___LB__NAME__RB__"), new And(new Literal("NAME__GEQ__199905"), new Literal("NAME__LT__1991011"))), new Or(new Literal("NAME__GEQ__300000"), new Literal("DEFINED___LB__NAME__RB__")))),
                new TestCase("#if __has_warning(\"-Wa-warning\"_foo)",
                        new Literal("__HAS_WARNING___LB____QUOTE____SUB__Wa__SUB__warning__QUOTE_____foo__RB__")),

                new TestCase("#if A && (B - (C || D))", new And(new Literal("A"), new Literal("B__SUB____LB__C__L_OR__D__RB__"))),
                new TestCase("#if A == '1'", new Literal("A__EQ____SQUOTE__1__SQUOTE__"))
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
