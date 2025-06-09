package org.variantsync.diffdetective.feature.cpp;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;
import org.tinylog.Logger;
import org.variantsync.diffdetective.error.UncheckedUnparseableFormulaException;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.ParseErrorListener;
import org.variantsync.diffdetective.feature.PreprocessorAnnotationParser;
import org.variantsync.diffdetective.feature.antlr.CExpressionLexer;
import org.variantsync.diffdetective.feature.antlr.CExpressionParser;

import java.util.regex.Pattern;

/**
 * Parses a C preprocessor statement.
 * For example, given the annotation {@code "#if defined(A) || B()"},
 * this class extracts the type {@link org.variantsync.diffdetective.feature.AnnotationType#If} with the formula {@code new Or(new Literal("A"), new Literal("B"))}.
 * The extractor detects if, ifdef, ifndef, elif, elifdef, elifndef, else and endif annotations.
 * All other annotations are considered source code.
 * The given CPP statement might also be a line in a diff (i.e., preceded by a - or +).
 *
 * @author Paul Bittner, Sören Viegener, Benjamin Moosherr, Alexander Schultheiß
 */
public class CPPAnnotationParser extends PreprocessorAnnotationParser {
    // Note that this pattern doesn't handle comments between {@code #} and the macro name.
    private static final String CPP_ANNOTATION_REGEX = "^[+-]?\\s*#\\s*(?<directive>if|ifdef|ifndef|elif|elifdef|elifndef|else|endif)(?<formula>[\\s(].*)?$";
    private static final Pattern CPP_ANNOTATION_PATTERN = Pattern.compile(CPP_ANNOTATION_REGEX);

    private ParseTreeVisitor<Node> formulaVisitor;

    public CPPAnnotationParser(ParseTreeVisitor<Node> formulaVisitor) {
        super(CPP_ANNOTATION_PATTERN);
        this.formulaVisitor = formulaVisitor;
    }

    public CPPAnnotationParser() {
        this(new ControllingCExpressionVisitor());
    }

    @Override
    public Node parseFormula(final String directive, final String formula) throws UnparseableFormulaException {
        Node parsedFormula;
        try {
            CExpressionLexer lexer = new CExpressionLexer(CharStreams.fromString(formula));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            CExpressionParser parser = new CExpressionParser(tokens);
            parser.addErrorListener(new ParseErrorListener(formula));

            parsedFormula = parser.expression().accept(formulaVisitor);
        } catch (UncheckedUnparseableFormulaException e) {
            throw e.inner();
        } catch (Exception e) {
            Logger.warn(e);
            throw new UnparseableFormulaException(e);
        }

        // treat {@code #ifdef id}, {@code #ifndef id}, {@code #elifdef id} and {@code #elifndef id}
        // like {@code defined(id)} and {@code !defined(id)}
        if (directive.endsWith("def")) {
            if (parsedFormula instanceof Literal literal) {
                literal.var = String.format("defined(%s)", literal.var);

                // negate for ifndef
                if (directive.endsWith("ndef")) {
                    literal.positive = false;
                }
            } else {
                throw new UnparseableFormulaException("When using #ifdef, #ifndef, #elifdef or #elifndef, only literals are allowed. Hence, \"" + formula + "\" is disallowed.");
            }
        }

        return parsedFormula;
    }
}
