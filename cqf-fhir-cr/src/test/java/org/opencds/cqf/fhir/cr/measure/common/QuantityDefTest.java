package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Updated by Claude Sonnet 4.5 on 2025-12-02
class QuantityDefTest {

    @Test
    void testQuantityDefCreation() {
        QuantityDef qd = new QuantityDef(42.5);

        assertEquals(42.5, qd.value());
    }

    @Test
    void testQuantityDefWithNullValue() {
        QuantityDef qd = new QuantityDef(null);

        assertNull(qd.value());
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-28
    @Test
    void testInstanceEqualityDifferentObjects() {
        // Two QuantityDefs with identical values are NOT equal (instance equality)
        QuantityDef qd1 = new QuantityDef(42.5);
        QuantityDef qd2 = new QuantityDef(42.5);

        assertNotEquals(qd1, qd2, "QuantityDefs with identical values should NOT be equal (instance equality)");
        assertNotSame(qd1, qd2, "QuantityDefs are different instances");
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-28
    @Test
    void testInstanceEqualitySameReference() {
        // Same reference IS equal
        QuantityDef qd1 = new QuantityDef(42.5);
        QuantityDef qd2 = qd1;

        assertEquals(qd1, qd2, "Same reference should be equal");
        assertSame(qd1, qd2, "Same reference");
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-28
    @Test
    void testCollectionProcessing() {
        // Two QuantityDefs with identical values should be treated as different in collections
        QuantityDef qd1 = new QuantityDef(42.5);
        QuantityDef qd2 = new QuantityDef(42.5);

        Set<QuantityDef> set = new HashSet<>();
        set.add(qd1);
        set.add(qd2);

        assertEquals(2, set.size(), "Set should contain 2 distinct instances despite identical values");
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-28
    @Test
    void testToString() {
        QuantityDef qd = new QuantityDef(42.5);

        String result = qd.toString();

        assertNotNull(result);
        assertTrue(result.contains("42.5"));
    }
}
