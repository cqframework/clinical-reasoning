package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToResource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ItemValueTransformerTests {

    @Test
    void transformValueToItemDstu3() {
        var stringType = new org.hl7.fhir.dstu3.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToItem(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.dstu3.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.dstu3.model.CodeableConcept().addCoding(coding);
        var transformValue = ItemValueTransformer.transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.dstu3.model.Enumeration<>(new org.hl7.fhir.dstu3.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.dstu3.model.Patient.LinkType.REFER);
        var transformEnum = ItemValueTransformer.transformValueToItem(enumeration);
        var expected = org.hl7.fhir.dstu3.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.dstu3.model.StringType) transformEnum).getValue();
        Assertions.assertEquals(expected, actual);

        var idType = new org.hl7.fhir.dstu3.model.IdType("test");
        var transformIdType = ItemValueTransformer.transformValueToItem(idType);
        Assertions.assertEquals(idType.getValueAsString(), ((org.hl7.fhir.dstu3.model.StringType) transformIdType).getValue());

        var identifier = new org.hl7.fhir.dstu3.model.Identifier().setValue("test");
        var transformIdentifier = ItemValueTransformer.transformValueToItem(identifier);
        Assertions.assertEquals(identifier.getValue(), ((org.hl7.fhir.dstu3.model.StringType) transformIdentifier).getValue());
    }

    @Test
    void transformValueToItemR4() {
        var stringType = new org.hl7.fhir.r4.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToItem(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.r4.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r4.model.CodeableConcept().addCoding(coding);
        var transformValue = ItemValueTransformer.transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r4.model.Enumeration<>(new org.hl7.fhir.r4.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r4.model.Patient.LinkType.REFER);
        var transformEnum = ItemValueTransformer.transformValueToItem(enumeration);
        var expected = org.hl7.fhir.r4.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r4.model.StringType) transformEnum).getValue();
        Assertions.assertEquals(expected, actual);

        var idType = new org.hl7.fhir.r4.model.IdType("test");
        var transformIdType = ItemValueTransformer.transformValueToItem(idType);
        Assertions.assertEquals(idType.getValueAsString(), ((org.hl7.fhir.r4.model.StringType) transformIdType).getValue());

        var identifier = new org.hl7.fhir.r4.model.Identifier().setValue("test");
        var transformIdentifier = ItemValueTransformer.transformValueToItem(identifier);
        Assertions.assertEquals(identifier.getValue(), ((org.hl7.fhir.r4.model.StringType) transformIdentifier).getValue());
    }

    @Test
    void transformValueToItemR5() {
        var stringType = new org.hl7.fhir.r5.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToItem(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.r5.model.Coding("test", "test", "test");
        var code = new org.hl7.fhir.r5.model.CodeableConcept().addCoding(coding);
        var transformValue = ItemValueTransformer.transformValueToItem(code);
        assertEquals(coding, transformValue);

        var enumeration =
                new org.hl7.fhir.r5.model.Enumeration<>(new org.hl7.fhir.r5.model.Patient.LinkTypeEnumFactory());
        enumeration.setValue(org.hl7.fhir.r5.model.Patient.LinkType.REFER);
        var transformEnum = ItemValueTransformer.transformValueToItem(enumeration);
        var expected = org.hl7.fhir.r5.model.Patient.LinkType.REFER.toCode();
        var actual = ((org.hl7.fhir.r5.model.StringType) transformEnum).getValue();
        Assertions.assertEquals(expected, actual);

        var idType = new org.hl7.fhir.r5.model.IdType("test");
        var transformIdType = ItemValueTransformer.transformValueToItem(idType);
        Assertions.assertEquals(idType.getValueAsString(), ((org.hl7.fhir.r5.model.StringType) transformIdType).getValue());

        var identifier = new org.hl7.fhir.r5.model.Identifier().setValue("test");
        var transformIdentifier = ItemValueTransformer.transformValueToItem(identifier);
        Assertions.assertEquals(identifier.getValue(), ((org.hl7.fhir.r5.model.StringType) transformIdentifier).getValue());
    }

    @Test
    void transformValueToResourceDstu3() {
        var stringType = new org.hl7.fhir.dstu3.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToResource(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.dstu3.model.Coding("test", "test", "test");
        var transformCoding = ItemValueTransformer.transformValueToResource(coding);
        assertInstanceOf(org.hl7.fhir.dstu3.model.CodeableConcept.class, transformCoding);
        Assertions.assertEquals(((org.hl7.fhir.dstu3.model.CodeableConcept) transformCoding).getCodingFirstRep(), coding);
    }

    @Test
    void transformValueToResourceR4() {
        var stringType = new org.hl7.fhir.r4.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToResource(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.r4.model.Coding("test", "test", "test");
        var transformCoding = ItemValueTransformer.transformValueToResource(coding);
        assertInstanceOf(org.hl7.fhir.r4.model.CodeableConcept.class, transformCoding);
        Assertions.assertEquals(((org.hl7.fhir.r4.model.CodeableConcept) transformCoding).getCodingFirstRep(), coding);
    }

    @Test
    void transformValueToResourceR5() {
        var stringType = new org.hl7.fhir.r5.model.StringType("test");
        var transformStringType = ItemValueTransformer.transformValueToResource(stringType);
        assertEquals(stringType, transformStringType);

        var coding = new org.hl7.fhir.r5.model.Coding("test", "test", "test");
        var transformCoding = ItemValueTransformer.transformValueToResource(coding);
        assertInstanceOf(org.hl7.fhir.r5.model.CodeableConcept.class, transformCoding);
        Assertions.assertEquals(((org.hl7.fhir.r5.model.CodeableConcept) transformCoding).getCodingFirstRep(), coding);
    }
}
