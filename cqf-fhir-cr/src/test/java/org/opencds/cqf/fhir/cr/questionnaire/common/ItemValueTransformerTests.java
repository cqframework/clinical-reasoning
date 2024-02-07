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

        var enumeration =
                new org.hl7.fhir.dstu3.model.Enumeration<>(new org.hl7.fhir.dstu3.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.dstu3.model.Patient.LinkType.REFER);
        var transformEnum = transformValue(enumeration);
        var expected = org.hl7.fhir.dstu3.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.dstu3.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }

    @Test
    void testTransformValueR4() {
        var coding = new org.hl7.fhir.r4.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r4.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValue(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r4.model.Enumeration<>(new org.hl7.fhir.r4.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r4.model.Patient.LinkType.REFER);
        var transformEnum = transformValue(enumeration);
        var expected = org.hl7.fhir.r4.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r4.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }

    @Test
    void testTransformValueR5() {
        var coding = new org.hl7.fhir.r5.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r5.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValue(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r5.model.Enumeration<>(new org.hl7.fhir.r5.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r5.model.Patient.LinkType.REFER);
        var transformEnum = transformValue(enumeration);
        var expected = org.hl7.fhir.r5.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r5.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }
}
