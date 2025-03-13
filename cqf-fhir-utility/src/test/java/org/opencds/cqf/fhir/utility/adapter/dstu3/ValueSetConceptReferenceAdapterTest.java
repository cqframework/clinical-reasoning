package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.junit.jupiter.api.Test;

class ValueSetConceptReferenceAdapterTest {

    @Test
    void invalid_object_fails() {
        var expansion = new ValueSet.ValueSetExpansionComponent();
        assertThrows(IllegalArgumentException.class, () -> new ValueSetConceptReferenceAdapter(expansion));
    }

    @Test
    void test() {
        var conceptRef = new ValueSet.ConceptReferenceComponent();
        var adapter = new ValueSetConceptReferenceAdapter(conceptRef);
        assertNotNull(adapter);
        assertEquals(conceptRef, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testCode() {
        var code = "test";
        var conceptRef = new ValueSet.ConceptReferenceComponent().setCode(code);
        var adapter = new ValueSetConceptReferenceAdapter(conceptRef);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode());
    }
}
