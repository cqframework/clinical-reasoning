// Created by claude-opus-4-6
package org.opencds.cqf.fhir.cr.hapi.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.junit.jupiter.api.Test;

class CrExceptionTranslatorTest {

    @Test
    void execute_returnsResultOnSuccess() {
        var result = CrExceptionTranslator.execute(() -> "hello");
        assertThat(result, is(equalTo("hello")));
    }

    @Test
    void execute_translatesIllegalArgumentToInvalidRequest() {
        var cause = new IllegalArgumentException("bad input");
        var thrown = assertThrows(
                InvalidRequestException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown.getMessage(), containsString("bad input"));
        assertThat(thrown.getCause(), is(sameInstance(cause)));
    }

    @Test
    void execute_translatesUnsupportedOperationToNotImplemented() {
        var cause = new UnsupportedOperationException("not supported");
        var thrown = assertThrows(
                NotImplementedOperationException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown.getMessage(), containsString("not supported"));
        assertThat(thrown.getCause(), is(sameInstance(cause)));
    }

    @Test
    void execute_doesNotCatchIllegalState() {
        var cause = new IllegalStateException("internal error");
        var thrown = assertThrows(
                IllegalStateException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown, is(sameInstance(cause)));
    }

    @Test
    void execute_doesNotCatchNullPointer() {
        var cause = new NullPointerException("null ref");
        var thrown = assertThrows(
                NullPointerException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown, is(sameInstance(cause)));
    }

    @Test
    void execute_doesNotCatchBaseException() {
        var cause = new RuntimeException("generic");
        var thrown = assertThrows(
                RuntimeException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown, is(sameInstance(cause)));
    }

    @Test
    void execute_doesNotTranslateExistingRestExceptions() {
        var cause = new InvalidRequestException("already translated");
        var thrown = assertThrows(
                InvalidRequestException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown, is(sameInstance(cause)));
    }

    @Test
    void executeVoid_completesOnSuccess() {
        CrExceptionTranslator.executeVoid(() -> {});
    }

    @Test
    void executeVoid_translatesIllegalArgumentToInvalidRequest() {
        var cause = new IllegalArgumentException("bad void input");
        var thrown = assertThrows(
                InvalidRequestException.class,
                () -> CrExceptionTranslator.executeVoid(() -> {
                    throw cause;
                }));
        assertThat(thrown.getMessage(), containsString("bad void input"));
        assertThat(thrown.getCause(), is(sameInstance(cause)));
    }

    @Test
    void executeVoid_translatesUnsupportedOperationToNotImplemented() {
        var cause = new UnsupportedOperationException("not supported void");
        var thrown = assertThrows(
                NotImplementedOperationException.class,
                () -> CrExceptionTranslator.executeVoid(() -> {
                    throw cause;
                }));
        assertThat(thrown.getMessage(), containsString("not supported void"));
        assertThat(thrown.getCause(), is(sameInstance(cause)));
    }

    @Test
    void execute_preservesCauseType() {
        var cause = new IllegalArgumentException("test");
        var thrown = assertThrows(
                InvalidRequestException.class,
                () -> CrExceptionTranslator.execute(() -> {
                    throw cause;
                }));
        assertThat(thrown.getCause(), is(instanceOf(IllegalArgumentException.class)));
    }
}
