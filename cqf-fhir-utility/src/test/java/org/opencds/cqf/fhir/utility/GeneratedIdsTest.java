package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GeneratedIdsTest {
    @Test
    void preservesLegalLogicalIdsAndNormalizesCompleteComposites() {
        var originals = new String[] {
            "A",
            "A".repeat(64),
            "A".repeat(65),
            "a".repeat(90) + "A",
            "a".repeat(90) + "B",
            "A B",
            "A/B",
            "AB",
            "cafÃ©/æ¼¢å­—",
            "æ¼¢å­—",
            "",
            "A.b-C.D",
            "ðŸ˜ƒ",
            ".-"
        };
        for (var original : originals) {
            var result = GeneratedIds.fromComposite(original);
            assertTrue(result.matches("[A-Za-z0-9.-]{1,64}"));
            assertEquals(result, GeneratedIds.fromComposite(original));
            if (original.matches("[A-Za-z0-9.-]{1,64}")) {
                assertEquals(original, result);
            }
        }
        assertEquals("e3b0c44298fc", GeneratedIds.fromComposite(""));
        assertNotEquals(
                GeneratedIds.fromComposite("a".repeat(90) + "A"), GeneratedIds.fromComposite("a".repeat(90) + "B"));
        assertNotEquals(GeneratedIds.fromComposite("A B"), GeneratedIds.fromComposite("A/B"));
        assertNotEquals(GeneratedIds.fromComposite("AB"), GeneratedIds.fromComposite("A/B"));
        assertTrue(GeneratedIds.fromComposite("a".repeat(50) + "-" + "z".repeat(30))
                .startsWith("a".repeat(50) + "--"));
        assertThrows(NullPointerException.class, () -> GeneratedIds.fromComposite(null));
    }
}
