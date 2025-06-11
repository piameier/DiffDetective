package org.variantsync.diffdetective.feature;

import org.variantsync.diffdetective.error.UnparseableFormulaException;

/**
 * Interface for a parser that analyzes annotations in parsed text. The parser is responsible for determining the type
 * of the annotation (see {@link AnnotationType}), and parsing the annotation into a {@link org.prop4j.Node}.
 * <p>
 * See {@link PreprocessorAnnotationParser} for an example of how an implementation of AnnotationParser could look like.
 * </p>
 */
public interface AnnotationParser {
    /**
     * Parse the given line as an annotation.
     * Note that {@code line} might also be a line in a diff (i.e., preceded by {@code -} or {@code +}).
     *
     * @param line that might contain an annotation
     * @return the annotation type and the associated formula.
     * If {@code line} doesn't contain an annotation, returns {@code Annotation(AnnotationType.NONE)}.
     * @throws UnparseableFormulaException if an annotation is detected but it is malformed
     */
    Annotation parseAnnotation(String line) throws UnparseableFormulaException;
}
