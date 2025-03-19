package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

class ValueSetExpansionContainsAdapterTest {

    @Test
    void invalid_object_fails() {
        var expansion = new ValueSet.ValueSetExpansionComponent();
        assertThrows(IllegalArgumentException.class, () -> new ValueSetExpansionContainsAdapter(expansion));
    }

    @Test
    void test() {
        var contains = new ValueSet.ValueSetExpansionContainsComponent();
        var adapter = new ValueSetExpansionContainsAdapter(contains);
        assertNotNull(adapter);
        assertEquals(contains, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testCodeAndSystem() {
        var code = "test";
        var system = "test.com";
        var contains =
                new ValueSet.ValueSetExpansionContainsComponent().setCode(code).setSystem(system);
        var adapter = new ValueSetExpansionContainsAdapter(contains);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode());
        assertTrue(adapter.hasSystem());
        assertEquals(system, adapter.getSystem());
    }
}
