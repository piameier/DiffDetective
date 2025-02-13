package org.variantsync.diffdetective.feature.cpp;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.prop4j.Node;
import org.prop4j.Not;
import org.tinylog.Logger;
import org.variantsync.diffdetective.error.UncheckedUnParseableFormulaException;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.DiffLineFormulaExtractor;
import org.variantsync.diffdetective.feature.ParseErrorListener;
import org.variantsync.diffdetective.feature.antlr.CExpressionLexer;
import org.variantsync.diffdetective.feature.antlr.CExpressionParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the expression from a C preprocessor statement.
 * For example, given the annotation {@code "#if defined(A) || B()"}, the extractor would extract
 * {@code new Or(new Literal("A"), new Literal("B"))}. The extractor detects if, ifdef, ifndef and
 * elif annotations. (Other annotations do not have expressions.)
 * The given pre-processor statement might also be a line in a diff (i.e., preceeded by a - or +).
 *
 * @author Paul Bittner, Sören Viegener, Benjamin Moosherr, Alexander Schultheiß
 */
public class CPPDiffLineFormulaExtractor implements DiffLineFormulaExtractor {
    // ^[+-]?\s*#\s*(if|ifdef|ifndef|elif)(\s+(.*)|\((.*)\))$
    private static final String CPP_ANNOTATION_REGEX = "^[+-]?\\s*#\\s*(if|ifdef|ifndef|elif)([\\s(].*)$";
    private static final Pattern CPP_ANNOTATION_PATTERN = Pattern.compile(CPP_ANNOTATION_REGEX);

    /**
     * Extracts and parses the feature formula from a macro line (possibly within a diff).
     *
     * @param line The line of which to get the feature mapping
     * @return The feature mapping
     */
    @Override
    public Node extractFormula(final String line) throws UnparseableFormulaException {
        // Match the formula from the macro line
        final Matcher matcher = CPP_ANNOTATION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new UnparseableFormulaException("Could not extract formula from line \"" + line + "\".");
        }
        String annotationType = matcher.group(1);
        String formula = matcher.group(2);

        // abstract complex formulas (e.g., if they contain arithmetics or macro calls)
        Node parsedFormula;
        try {
            CExpressionLexer lexer = new CExpressionLexer(CharStreams.fromString(formula));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            CExpressionParser parser = new CExpressionParser(tokens);
            parser.addErrorListener(new ParseErrorListener(formula));

            parsedFormula = parser.expression().accept(new ControllingCExpressionVisitor());
        } catch (UncheckedUnParseableFormulaException e) {
            throw e.inner();
        } catch (Exception e) {
            Logger.warn(e);
            throw new UnparseableFormulaException(e);
        }

        // negate for ifndef
        if ("ifndef".equals(annotationType)) {
            parsedFormula = new Not(parsedFormula);
        }

        return parsedFormula;
    }
}
