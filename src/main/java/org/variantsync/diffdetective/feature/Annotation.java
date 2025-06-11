package org.variantsync.diffdetective.feature;

import org.prop4j.Node;

/**
 * Represents a new annotation (i.e., a change in the presence condition).
 * This includes, {@link AnnotationType#If adding to the presence condition},
 * {@link AnnotationType#Endif removing the most recent formula}
 * and {@link AnnotationType others}.
 *
 * @param type the type of this annotation
 * @param formula the formula associated to {@code type}.
 * Non-null iff {@link AnnotationType#requiresFormula type.requiresFormula}.
 */
public record Annotation(
    AnnotationType type,
    Node formula
) {
    public Annotation(AnnotationType type, Node formula) {
        this.type = type;
        this.formula = formula;

        if (type.requiresFormula && formula == null) {
            throw new IllegalArgumentException("Annotations of type " + type.name + " but got null");
        }
        if (!type.requiresFormula && formula != null) {
            throw new IllegalArgumentException("Annotations of type " + type.name + " do not accept a formula but it was given " + formula);
        }
    }

    /**
     * Equivalent to {@link #Annotation(AnnotationType, Node) Annotation(type, null)}.
     * Hence, only useable iff {@link AnnotationType#requiresFormula !type.requiresFormula}.
     */
    public Annotation(AnnotationType type) {
        this(type, null);
    }
}
