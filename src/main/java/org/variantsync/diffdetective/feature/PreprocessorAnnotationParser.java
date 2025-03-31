package org.variantsync.diffdetective.feature;

import org.prop4j.Node;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import org.variantsync.diffdetective.feature.cpp.CPPDiffLineFormulaExtractor;
import org.variantsync.diffdetective.feature.jpp.JPPDiffLineFormulaExtractor;

import java.util.regex.Pattern;

/**
 * A parser of preprocessor-like annotations.
 *
 * @author Paul Bittner, Alexander Schultheiß
 */
public class PreprocessorAnnotationParser implements AnnotationParser {
    /**
     * Matches the beginning or end of CPP conditional macros.
     * It doesn't match the whole macro name, for example for {@code #ifdef} only {@code "#if"} is
     * matched and only {@code "if"} is captured.
     * <p>
     * Note that this pattern doesn't handle comments between {@code #} and the macro name.
     */
    protected final static Pattern CPP_PATTERN =
            Pattern.compile("^[+-]?\\s*#\\s*(if|elif|else|endif)");

    /**
     * Matches the beginning or end of JPP conditional macros.
     * It doesn't match the whole macro name, for example for {@code //#if defined(x)} only {@code "//#if"} is
     * matched and only {@code "if"} is captured.
     * <p>
     */
    protected final static Pattern JPP_PATTERN =
            Pattern.compile("^[+-]?\\s*//\\s*#\\s*(if|elif|else|endif)");

    /**
     * Default parser for C preprocessor annotations.
     * Created by invoking {@link #PreprocessorAnnotationParser(Pattern, DiffLineFormulaExtractor)}.
     */
    public static final PreprocessorAnnotationParser CPPAnnotationParser =
            new PreprocessorAnnotationParser(CPP_PATTERN, new CPPDiffLineFormulaExtractor());

    /**
     * Default parser for <a href="https://www.slashdev.ca/javapp/">JavaPP (Java PreProcessor)</a> annotations.
     * Created by invoking {@link #PreprocessorAnnotationParser(Pattern, DiffLineFormulaExtractor)}.
     */
    public static final PreprocessorAnnotationParser JPPAnnotationParser =
            new PreprocessorAnnotationParser(JPP_PATTERN, new JPPDiffLineFormulaExtractor());

    // Pattern that is used to identify the AnnotationType of a given annotation.
    private final Pattern annotationPattern;
    private final DiffLineFormulaExtractor extractor;

    /**
     * Creates a new preprocessor annotation parser.
     *
     * @param annotationPattern Pattern that is used to identify the AnnotationType of a given annotation; {@link #CPP_PATTERN} provides an example
     * @param formulaExtractor  An extractor that extracts the formula part of a preprocessor annotation
     */
    public PreprocessorAnnotationParser(final Pattern annotationPattern, DiffLineFormulaExtractor formulaExtractor) {
        this.annotationPattern = annotationPattern;
        this.extractor = formulaExtractor;
    }

    /**
     * Creates a new preprocessor annotation parser for C preprocessor annotations.
     *
     * @param formulaExtractor An extractor that extracts the formula part of a preprocessor annotation
     */
    public static PreprocessorAnnotationParser CreateCppAnnotationParser(DiffLineFormulaExtractor formulaExtractor) {
        return new PreprocessorAnnotationParser(CPP_PATTERN, formulaExtractor);
    }

    /**
     * Creates a new preprocessor annotation parser for <a href="https://www.slashdev.ca/javapp/">JavaPP (Java PreProcessor)</a> annotations.
     *
     * @param formulaExtractor An extractor that extracts the formula part of a preprocessor annotation
     */
    public static PreprocessorAnnotationParser CreateJppAnnotationParser(DiffLineFormulaExtractor formulaExtractor) {
        return new PreprocessorAnnotationParser(JPP_PATTERN, formulaExtractor);
    }

    /**
     * Parses the condition of the given line of source code that contains a preprocessor macro (i.e., IF, IFDEF, ELIF).
     *
     * @param line The line of code of a preprocessor annotation.
     * @return The formula of the macro in the given line.
     * If no such formula could be parsed, returns a Literal with the line's condition as name.
     * @throws UnparseableFormulaException when {@link DiffLineFormulaExtractor#extractFormula(String)} throws.
     */
    @Override
    public Node parseAnnotation(String line) throws UnparseableFormulaException {
        return extractor.extractFormula(line);
    }

    @Override
    public AnnotationType determineAnnotationType(String text) {
        var matcher = annotationPattern.matcher(text);
        int nameId = 1;
        if (matcher.find()) {
            return AnnotationType.fromName(matcher.group(nameId));
        } else {
            return AnnotationType.None;
        }
    }
}
