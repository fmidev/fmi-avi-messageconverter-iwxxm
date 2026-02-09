package fi.fmi.avi.converter.iwxxm.generic;

import java.util.Optional;

/**
 * Represents the result of evaluating an XPath expression.
 * Can be one of:
 * <ul>
 *   <li>Success with a value (optionally with a warning)</li>
 *   <li>Empty (expression matched but no value found)</li>
 *   <li>Failure with an exception</li>
 * </ul>
 *
 * @param <T> the type of the value
 */
public final class XPathEvaluationResult<T> {

    private final T value;
    private final Exception exception;

    private XPathEvaluationResult(final T value, final Exception exception) {
        this.value = value;
        this.exception = exception;
    }

    public static <T> XPathEvaluationResult<T> of(final T value) {
        return new XPathEvaluationResult<>(value, null);
    }

    public static <T> XPathEvaluationResult<T> empty() {
        return new XPathEvaluationResult<>(null, null);
    }

    public static <T> XPathEvaluationResult<T> fail(final Exception exception) {
        return new XPathEvaluationResult<>(null, exception);
    }

    /**
     * Returns the value or throws if this result doesn't contain one.
     *
     * @throws IllegalStateException if this result is empty or failed
     * @return the value if present
     */
    public T getOrThrow() {
        if (value == null) {
            throw new IllegalStateException("No value present");
        }
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

}
