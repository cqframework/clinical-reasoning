package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
