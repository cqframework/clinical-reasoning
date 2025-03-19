package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrefetchUrlListTest {

    @Test
    void testAddIgnoresDuplicates() {
        var fixture = new PrefetchUrlList();
        var patientContext = "Patient?_id={{context.patientId}}";
        fixture.add(patientContext);
        assertEquals(1, fixture.size());
        fixture.add(patientContext);
        assertEquals(1, fixture.size());
    }

    @Test
    void testAddIgnoresDuplicateResourceTypes() {
        var fixture = new PrefetchUrlList();
        var encounterContext = "Encounter?status=finished&subject=Patient/{{context.patientId}}";
        fixture.add(encounterContext);
        assertEquals(1, fixture.size());
        fixture.add(encounterContext + "&type:in=http://test.com/fhir/ValueSet/test");
        assertEquals(1, fixture.size());
    }

    @Test
    void testAddAllIgnoresNulls() {
        var fixture = new PrefetchUrlList();
        assertTrue(fixture.addAll(null));
    }
}
