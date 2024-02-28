package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;

class ResourcesTest {

    @Test
    void testClone() {
        var lib = new Library();
        lib.setUsage("example usage");

        var c = Resources.clone(lib);

        assertNotSame(c, lib);
        assertTrue(c.equalsDeep(lib));
    }
}
