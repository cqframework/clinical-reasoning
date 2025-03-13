package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.junit.jupiter.api.Test;

class DataRequirementAdapterTest {

    @Test
    void invalid_object_fails() {
        var coding = new Coding();
        assertThrows(IllegalArgumentException.class, () -> new DataRequirementAdapter(coding));
    }

    @Test
    void test() {
        var dataReq = new DataRequirement();
        var adapter = new DataRequirementAdapter(dataReq);
        assertNotNull(adapter);
        assertEquals(dataReq, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testType() {
        var type = "test";
        var dataReq = new DataRequirement().setType(type);
        var adapter = new DataRequirementAdapter(dataReq);
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType());
    }

    @Test
    void testCodeFilter() {
        var code = "test";
        var codeFilter = new DataRequirementCodeFilterComponent().addValueCoding(new Coding().setCode(code));
        var dataReq = new DataRequirement().addCodeFilter(codeFilter);
        var adapter = new DataRequirementAdapter(dataReq);
        assertTrue(adapter.hasCodeFilter());
        assertEquals(codeFilter, adapter.getCodeFilter().get(0).get());
    }
}
