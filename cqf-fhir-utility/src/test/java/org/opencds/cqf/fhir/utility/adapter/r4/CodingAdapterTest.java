package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;

class CodingAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var codeableConcept = new CodeableConcept();
        assertThrows(IllegalArgumentException.class, () -> new CodingAdapter(codeableConcept));
    }

    @Test
    void test() {
        var coding = new Coding();
        var adapter = adapterFactory.createCoding(coding);
        assertNotNull(adapter);
        assertEquals(coding, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testCode() {
        var code = "test";
        var display = "Test";
        var system = "test.com";
        var coding = new Coding(system, code, display);
        var adapter = adapterFactory.createCoding(coding);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode());
        assertTrue(adapter.hasDisplay());
        assertEquals(display, adapter.getDisplay());
        assertTrue(adapter.hasSystem());
        assertEquals(system, adapter.getSystem());
    }
}
