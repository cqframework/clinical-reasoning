package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TriesTest {

    @Test
    void ofWithError() {
        var t = Tries.of(new RuntimeException("error"), null);
        assertTrue(t.hasException());
    }

    @Test
    void ofWithValue() {
        var t = Tries.of(null, 42);
        assertTrue(t.hasResult());
        assertEquals(42, t.getOrThrow());
    }

    @Test
    void ofSupplierSuccess() {
        var t = Tries.of(() -> "hello");
        assertTrue(t.hasResult());
        assertEquals("hello", t.getOrThrow());
    }

    @Test
    void ofSupplierFailure() {
        var t = Tries.of(() -> {
            throw new RuntimeException("fail");
        });
        assertTrue(t.hasException());
    }

    @Test
    void ofExceptionDirectly() {
        var t = Tries.<String>ofException(new RuntimeException("fail"));
        assertTrue(t.hasException());
    }

    @Test
    void ofValueDirectly() {
        var t = Tries.of("hello");
        assertTrue(t.hasResult());
    }
}
