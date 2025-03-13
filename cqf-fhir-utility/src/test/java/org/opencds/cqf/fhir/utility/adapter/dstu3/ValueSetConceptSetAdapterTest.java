package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.junit.jupiter.api.Test;

class ValueSetConceptSetAdapterTest {

    @Test
    void invalid_object_fails() {
        var expansion = new ValueSet.ValueSetExpansionComponent();
        assertThrows(IllegalArgumentException.class, () -> new ValueSetConceptSetAdapter(expansion));
    }

    @Test
    void test() {
        var conceptSet = new ValueSet.ConceptSetComponent();
        var adapter = new ValueSetConceptSetAdapter(conceptSet);
        assertNotNull(adapter);
        assertEquals(conceptSet, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testConceptAndSystem() {
        var code1 = "test1";
        var code2 = "test2";
        var system = "test.com";
        var concept1 = new ValueSet.ConceptReferenceComponent().setCode(code1);
        var concept2 = new ValueSet.ConceptReferenceComponent().setCode(code2);
        var conceptSet = new ValueSet.ConceptSetComponent().setSystem(system).setConcept(List.of(concept1, concept2));
        var adapter = new ValueSetConceptSetAdapter(conceptSet);
        assertTrue(adapter.hasConcept());
        assertEquals(2, adapter.getConcept().size());
        assertTrue(adapter.hasSystem());
        assertEquals(system, adapter.getSystem());
    }
}
