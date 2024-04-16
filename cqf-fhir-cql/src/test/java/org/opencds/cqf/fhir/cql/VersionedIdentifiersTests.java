package org.opencds.cqf.fhir.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VersionedIdentifiersTests {
    @Test
    void testExceptions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VersionedIdentifiers.forUrl("http://fhir.org/guides/cdc/opioid-cds/Binary/HelloWorld"));

        assertThrows(
                IllegalArgumentException.class,
                () -> VersionedIdentifiers.forUrl(
                        "http://fhir.org/guides/cdc/opioid-cds/Library/HelloWorld/Library/HelloWorld"));
    }

    @Test
    void testUrl() {
        var url = "http://fhir.org/guides/cdc/opioid-cds/Library/HelloWorld";
        var id = VersionedIdentifiers.forUrl(url);
        assertEquals("HelloWorld", id.getId());
        assertNull(id.getVersion());
    }

    @Test
    void testCanonical() {
        var url = "http://fhir.org/guides/cdc/opioid-cds/Library/HelloWorld|1.0.0";
        var id = VersionedIdentifiers.forUrl(url);
        assertEquals("HelloWorld", id.getId());
        assertEquals("1.0.0", id.getVersion());
    }

    @Test
    void testReference() {
        var reference = "Library/HelloWorld";
        var id = VersionedIdentifiers.forUrl(reference);
        assertEquals("HelloWorld", id.getId());
        assertNull(id.getVersion());
    }
}