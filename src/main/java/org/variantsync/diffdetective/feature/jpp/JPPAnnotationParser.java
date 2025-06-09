package org.variantsync.diffdetective.feature.jpp;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.diffdetective.error.UncheckedUnparseableFormulaException;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.ParseErrorListener;
import org.variantsync.diffdetective.feature.PreprocessorAnnotationParser;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionLexer;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionParser;

import java.util.regex.Pattern;

/**
 * Parses a <a href="https://www.slashdev.ca/javapp/">JavaPP (Java PreProcessor)</a> statement.
 * For example, given the annotation {@code //#if defined(A) || B()},
 * the class would parse {@code new Or(new Literal("defined(A)"), new Literal("B()"))}.
 * The parser detects if, elif, else and endif annotations.
 * The given JPP statement might also be a line in a diff (i.e., preceded by a - or +).
 *
 * @author Alexander Schultheiß
 */
public class JPPAnnotationParser extends PreprocessorAnnotationParser {
    private static final String JPP_ANNOTATION_REGEX = "^[+-]?\\s*//\\s*#\\s*(?<directive>if|elif|else|endif)(?<formula>[\\s(].*)?$";
    private static final Pattern JPP_ANNOTATION_PATTERN = Pattern.compile(JPP_ANNOTATION_REGEX);

    public JPPAnnotationParser() {
        super(JPP_ANNOTATION_PATTERN);
    }

    @Override
    public Node parseFormula(final String directive, final String formula) throws UnparseableFormulaException {
        // abstract complex formulas (e.g., if they contain arithmetics or macro calls)
        try {
            JPPExpressionLexer lexer = new JPPExpressionLexer(CharStreams.fromString(formula));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JPPExpressionParser parser = new JPPExpressionParser(tokens);
            parser.addErrorListener(new ParseErrorListener(formula));
            ParseTree tree = parser.expression();
            return tree.accept(new ControllingJPPExpressionVisitor());
        } catch (UncheckedUnparseableFormulaException e) {
            throw e.inner();
        } catch (Exception e) {
            Logger.warn(e);
            throw new UnparseableFormulaException(e);
        }
    }
}
