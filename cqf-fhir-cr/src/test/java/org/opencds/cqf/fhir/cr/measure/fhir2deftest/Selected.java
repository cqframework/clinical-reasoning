package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

/**
 * Base class for fluent assertion API providing navigation and value access.
 * <p>
 * This class implements the Selected pattern used throughout the test DSL,
 * allowing fluent navigation between assertion contexts with .up() and direct
 * value access with .value().
 * </p>
 *
 * @param <T> the type of value being selected
 * @param <P> the type of parent context
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public abstract class Selected<T, P> {
    private final P parent;
    private final T value;

    protected Selected(T value, P parent) {
        this.parent = parent;
        this.value = value;
    }

    /**
     * Get the selected value for direct access or advanced assertions.
     *
     * @return the selected value
     */
    public T value() {
        return value;
    }

    /**
     * Navigate up to the parent context in the assertion chain.
     *
     * @return the parent context
     */
    public P up() {
        return parent;
    }
}
