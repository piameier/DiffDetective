package org.variantsync.diffdetective.feature;

import org.prop4j.Node;
import org.variantsync.diffdetective.error.UnparseableFormulaException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser of preprocessor-like annotations.
 *
 * @author Paul Bittner, Alexander Schultheiß
 * @see org.variantsync.diffdetective.feature.cpp.CPPAnnotationParser
 * @see org.variantsync.diffdetective.feature.jpp.JPPAnnotationParser
 */
public abstract class PreprocessorAnnotationParser implements AnnotationParser {
    /**
     * Pattern that is used to extract the {@link AnnotationType} and the associated {@link org.prop4j.Node formula}.
     * <p>
     * The pattern needs to contain at least the two named capture groups {@code directive} and {@code formula}.
     * The {@code directive} group must match a string that can be processed by {@link #parseAnnotationType}
     * and whenever the resulting annotation type {@link AnnotationType#requiresFormula requires a formula},
     * the capture group {@code formula} needs to match the formula that should be processed by {@link parseFormula}.
     */
    protected final Pattern annotationPattern;

    /**
     * Creates a new preprocessor annotation parser.
     *
     * @param annotationPattern pattern that identifies the {@link AnnotationType} and the associated {@link org.prop4j.Node formula} of an annotation
     * @see #annotationPattern
     */
    public PreprocessorAnnotationParser(Pattern annotationPattern) {
        this.annotationPattern = annotationPattern;
    }

    @Override
    public Annotation parseAnnotation(final String line) throws UnparseableFormulaException {
        // Match the formula from the macro line
        final Matcher matcher = annotationPattern.matcher(line);
        if (!matcher.find()) {
            return new Annotation(AnnotationType.None);
        }
        String directive = matcher.group("directive");
        String formula = matcher.group("formula");
        AnnotationType annotationType = parseAnnotationType(directive);

        if (!annotationType.requiresFormula) {
            return new Annotation(annotationType);
        }

        if (annotationType.requiresFormula && formula == null) {
            throw new UnparseableFormulaException("Annotations of type " + annotationType.name + " require a formula but none was given");
        }

        return new Annotation(annotationType, parseFormula(directive, formula));
    }

    /**
     * Converts the string captured by the named capture group {@code directive} of {@link #annotationPattern} into an {@link AnnotationType}.
     */
    protected AnnotationType parseAnnotationType(String directive) {
        if (directive.startsWith("if")) {
            return AnnotationType.If;
        } else if (directive.startsWith("elif")) {
            return AnnotationType.Elif;
        } else if (directive.equals("else")) {
            return AnnotationType.Else;
        } else if (directive.equals("endif")) {
            return AnnotationType.Endif;
        }

        throw new IllegalArgumentException("The directive " + directive + " is not a valid conditional compilation directive");
    }

    /**
     * Parses the feature formula of a preprocessor annotation line.
     * It should abstract complex formulas (e.g., if they contain arithmetics or macro calls) as desired.
     * For example, for the line {@code "#if A && B == C"},
     * this method is should be called like {@code parseFormula("if", "A && B == C")}
     * (the exact arguments are determined by {@link annotationPattern}
     * and it should return something like {@code and(var("A"), var("B==C"))}.
     * <p>
     * This method is only called if {@code directive} actually requires a formula as determined by {@link #parseAnnotationType}.
     *
     * @param directive as matched by the named capture group {@code directive} of {@link annotationPattern}
     * @param formula as matched by the named capture group {@code formula} of {@link annotationPattern}
     * @return the feature mapping
     * @throws UnparseableFormulaException if {@code formula} is ill-formed.
     */
    protected abstract Node parseFormula(String directive, String formula) throws UnparseableFormulaException;
}
