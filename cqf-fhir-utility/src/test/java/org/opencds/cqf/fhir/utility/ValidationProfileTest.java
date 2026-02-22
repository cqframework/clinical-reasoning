package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationProfileTest {

    @Test
    void defaultConstructor() {
        var vp = new ValidationProfile();
        assertNotNull(vp);
    }

    @Test
    void constructorWithArgs() {
        var vp = new ValidationProfile("test-profile", new ArrayList<>(List.of("key1")));
        assertEquals("test-profile", vp.getName());
        assertEquals(1, vp.getIgnoreKeys().size());
    }

    @Test
    void settersAndGetters() {
        var vp = new ValidationProfile();
        vp.setName("profile-1");
        assertEquals("profile-1", vp.getName());
        vp.setIgnoreKeys(new ArrayList<>(List.of("a", "b")));
        assertEquals(2, vp.getIgnoreKeys().size());
        vp.addIgnoreKey("c");
        assertEquals(3, vp.getIgnoreKeys().size());
    }
}
