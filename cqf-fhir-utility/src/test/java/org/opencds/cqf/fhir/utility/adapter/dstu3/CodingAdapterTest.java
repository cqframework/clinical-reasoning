package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
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
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
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
        var newCode = "test2";
        adapter.setCode(newCode);
        assertEquals(newCode, adapter.getCode());
        assertTrue(adapter.hasDisplay());
        assertEquals(display, adapter.getDisplay());
        var newDisplay = "Test2";
        adapter.setDisplay(newDisplay);
        assertEquals(newDisplay, adapter.getDisplay());
        assertTrue(adapter.hasSystem());
        assertEquals(system, adapter.getSystem());
        var newSystem = "test2.com";
        adapter.setSystem(newSystem);
        assertEquals(newSystem, adapter.getSystem());
    }
}
