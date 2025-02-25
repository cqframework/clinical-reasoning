package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.junit.jupiter.api.Test;

class CodeableConceptAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var element = new ElementDefinition();
        assertThrows(IllegalArgumentException.class, () -> new CodeableConceptAdapter(element));
    }

    @Test
    void test() {
        var codeableConcept = new CodeableConcept();
        var adapter = adapterFactory.createCodeableConcept(codeableConcept);
        assertNotNull(adapter);
        assertEquals(codeableConcept, adapter.get());
        assertEquals(FhirVersionEnum.R5, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testCoding() {
        var codeableConcept = new CodeableConcept().addCoding(new Coding("test.com", "test", "Test"));
        var adapter = adapterFactory.createCodeableConcept(codeableConcept);
        assertTrue(adapter.hasCoding("test"));
        assertEquals(1, adapter.getCoding().size());
    }
}
