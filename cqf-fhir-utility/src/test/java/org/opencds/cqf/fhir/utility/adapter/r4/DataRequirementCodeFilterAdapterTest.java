package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementDateFilterComponent;
import org.junit.jupiter.api.Test;

class DataRequirementCodeFilterAdapterTest {

    @Test
    void invalid_object_fails() {
        var filter = new DataRequirementDateFilterComponent();
        assertThrows(IllegalArgumentException.class, () -> new DataRequirementCodeFilterAdapter(filter));
    }

    @Test
    void test() {
        var codeFilter = new DataRequirementCodeFilterComponent();
        var adapter = new DataRequirementCodeFilterAdapter(codeFilter);
        assertNotNull(adapter);
        assertEquals(codeFilter, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testCodeAndPath() {
        var code = "test";
        var path = "testPath";
        var codeFilter = new DataRequirementCodeFilterComponent()
                .addCode(new Coding().setCode(code))
                .setPath(path);
        var adapter = new DataRequirementCodeFilterAdapter(codeFilter);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode().get(0).getCode());
        assertTrue(adapter.hasPath());
        assertEquals(path, adapter.getPath());
    }

    @Test
    void testValueSet() {
        var valueSet = "ValueSet/Test";
        var codeFilter = new DataRequirementCodeFilterComponent().setValueSet(valueSet);
        var adapter = new DataRequirementCodeFilterAdapter(codeFilter);
        assertTrue(adapter.hasValueSet());
        assertEquals(valueSet, adapter.getValueSet().getValueAsString());
    }
}
