package org.variantsync.diffdetective.feature.jpp;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.diffdetective.error.UncheckedUnParseableFormulaException;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.DiffLineFormulaExtractor;
import org.variantsync.diffdetective.feature.ParseErrorListener;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionLexer;
import org.variantsync.diffdetective.feature.antlr.JPPExpressionParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the expression from a <a href="https://www.slashdev.ca/javapp/">JavaPP (Java PreProcessor)</a> statement.
 * For example, given the annotation {@code //#if defined(A) || B()}, the extractor would extract
 * {@code new Or(new Literal("defined(A)"), new Literal("B()"))}.
 * The extractor detects if and elif annotations (other annotations do not have expressions).
 * The given JPP statement might also be a line in a diff (i.e., preceeded by a - or +).
 *
 * @author Alexander Schultheiß
 */
public class JPPDiffLineFormulaExtractor implements DiffLineFormulaExtractor {
    private static final String JPP_ANNOTATION_REGEX = "^[+-]?\\s*//\\s*#\\s*(if|elif)([\\s(].*)$";
    private static final Pattern JPP_ANNOTATION_PATTERN = Pattern.compile(JPP_ANNOTATION_REGEX);

    /**
     * Abstract the given formula.
     * <p>
     * First, the formula is parsed using ANTLR and then transformed using {@link ControllingJPPExpressionVisitor}.
     * The visitor traverses the tree starting from the root, searching for subtrees that must be abstracted.
     * If such a subtree is found, the visitor abstracts the part of the formula in the subtree.
     * </p>
     *
     * @param line that contains the formula to be abstracted
     * @return the abstracted formula
     */
    @Override
    public Node extractFormula(final String line) throws UnparseableFormulaException {
        // Match the formula from the macro line
        final Matcher matcher = JPP_ANNOTATION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new UnparseableFormulaException("Could not extract formula from line \"" + line + "\".");
        }
        String formula = matcher.group(2);

        // abstract complex formulas (e.g., if they contain arithmetics or macro calls)
        try {
            JPPExpressionLexer lexer = new JPPExpressionLexer(CharStreams.fromString(formula));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JPPExpressionParser parser = new JPPExpressionParser(tokens);
            parser.addErrorListener(new ParseErrorListener(formula));
            ParseTree tree = parser.expression();
            return tree.accept(new ControllingJPPExpressionVisitor());
        } catch (UncheckedUnParseableFormulaException e) {
            throw e.inner();
        } catch (Exception e) {
            Logger.warn(e);
            throw new UnparseableFormulaException(e);
        }
    }
}
