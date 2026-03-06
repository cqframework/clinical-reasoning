package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TryTest {

    @Test
    void tryHasException() {
        var exception = new Exception();
        var t = Tries.ofException(exception);
        assertTrue(t.hasException());
        assertFalse(t.hasResult());
        assertEquals(exception, t.exception());
    }

    @Test
    void flatMap() {
        var t = Tries.of(1);
        var result = t.flatMap(i -> Tries.of(i + 1));
        assertTrue(result.hasResult());
        assertEquals(2, result.getOrThrow());

        var e = new Exception();
        t = Tries.<Integer>ofException(e);
        result = t.flatMap(i -> Tries.of(i + 1));
        assertFalse(result.hasResult());
        assertTrue(result.hasException());
        assertEquals(e, result.exception());

        t = Tries.of(1);
        result = t.flatMap(i -> {
            throw new RuntimeException("runtime exception");
        });
        assertFalse(result.hasResult());
        assertTrue(result.hasException());
        assertInstanceOf(RuntimeException.class, result.exception());
    }

    @Test
    void map() {
        var t = Tries.of(1);
        var result = t.map(i -> i + 1);
        assertTrue(result.hasResult());
        assertEquals(2, result.getOrThrow());

        var e = new Exception();
        t = Tries.<Integer>ofException(e);
        result = t.map(i -> i + 1);
        assertFalse(result.hasResult());
        assertTrue(result.hasException());
        assertEquals(e, result.exception());

        t = Tries.of(1);
        result = t.map(i -> {
            throw new RuntimeException("runtime exception");
        });
        assertFalse(result.hasResult());
        assertTrue(result.hasException());
        assertInstanceOf(RuntimeException.class, result.exception());
    }
}
