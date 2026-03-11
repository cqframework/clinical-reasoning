// Created by claude-opus-4-6
package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import java.util.function.Supplier;

/**
 * Translates domain exceptions from core modules into HAPI FHIR REST exceptions at the
 * boundary layer. Core modules ({@code cqf-fhir-cr}, {@code cqf-fhir-cql},
 * {@code cqf-fhir-utility}) correctly throw standard Java exceptions; this utility catches
 * them and re-throws the appropriate REST exception so clients receive proper HTTP status
 * codes and OperationOutcome responses.
 *
 * <p>{@link IllegalStateException} and other {@link RuntimeException} subclasses are
 * intentionally not caught — HAPI's {@code RestfulServer} already wraps them as
 * {@code InternalErrorException} (HTTP 500).
 */
public final class CrExceptionTranslator {
    private CrExceptionTranslator() {}

    /**
     * Execute a supplier and translate domain exceptions to REST exceptions.
     *
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws InvalidRequestException if the operation throws {@link IllegalArgumentException}
     * @throws NotImplementedOperationException if the operation throws {@link UnsupportedOperationException}
     */
    public static <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        } catch (UnsupportedOperationException e) {
            var translated = new NotImplementedOperationException(e.getMessage());
            translated.initCause(e);
            throw translated;
        }
    }

    /**
     * Execute a void operation and translate domain exceptions to REST exceptions.
     *
     * @param operation the operation to execute
     * @throws InvalidRequestException if the operation throws {@link IllegalArgumentException}
     * @throws NotImplementedOperationException if the operation throws {@link UnsupportedOperationException}
     */
    public static void executeVoid(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        } catch (UnsupportedOperationException e) {
            var translated = new NotImplementedOperationException(e.getMessage());
            translated.initCause(e);
            throw translated;
        }
    }
}
