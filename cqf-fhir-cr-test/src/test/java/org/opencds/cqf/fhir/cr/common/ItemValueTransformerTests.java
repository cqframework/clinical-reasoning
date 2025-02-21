package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;

import org.junit.jupiter.api.Test;

class ItemValueTransformerTests {

    @Test
    void transformValueToItemDstu3() {
        var coding = new org.hl7.fhir.dstu3.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.dstu3.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.dstu3.model.Enumeration<>(new org.hl7.fhir.dstu3.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.dstu3.model.Patient.LinkType.REFER);
        var transformEnum = transformValueToItem(enumeration);
        var expected = org.hl7.fhir.dstu3.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.dstu3.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }

    @Test
    void transformValueToItemR4() {
        var coding = new org.hl7.fhir.r4.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r4.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r4.model.Enumeration<>(new org.hl7.fhir.r4.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r4.model.Patient.LinkType.REFER);
        var transformEnum = transformValueToItem(enumeration);
        var expected = org.hl7.fhir.r4.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r4.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }

    @Test
    void transformValueToItemR5() {
        var coding = new org.hl7.fhir.r5.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r5.model.CodeableConcept().addCoding(coding);
        var transformValue = transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r5.model.Enumeration<>(new org.hl7.fhir.r5.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r5.model.Patient.LinkType.REFER);
        var transformEnum = transformValueToItem(enumeration);
        var expected = org.hl7.fhir.r5.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r5.model.StringType) transformEnum).getValue();
        assertEquals(expected, actual);
    }
}
