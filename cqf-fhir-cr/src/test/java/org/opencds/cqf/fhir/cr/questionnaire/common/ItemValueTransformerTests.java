package org.opencds.cqf.fhir.cr.questionnaire.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import org.junit.jupiter.api.Test;

public class ItemValueTransformerTests {

    @Test
    void testTransformValueDstu3() {
        var coding = new org.hl7.fhir.dstu3.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.dstu3.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValue(code);
        assertEquals(coding, transformValue);
    }

    @Test
    void testTransformValueR4() {
        var coding = new org.hl7.fhir.r4.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r4.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValue(code);
        assertEquals(coding, transformValue);
    }

    @Test
    void testTransformValueR5() {
        var coding = new org.hl7.fhir.r5.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r5.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValue(code);
        assertEquals(coding, transformValue);
    }
}
