package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.AbstractMap.SimpleEntry;
import org.junit.jupiter.api.Test;

class ExtensionBuildersTests {

    @Test
    void testBuildReferenceExt() {
        SimpleEntry<String, String> entry = new SimpleEntry<>("url", "value");
        var dstu3Ext = (org.hl7.fhir.dstu3.model.Extension)
                ExtensionBuilders.buildReferenceExt(FhirVersionEnum.DSTU3, entry, false);
        assertTrue(dstu3Ext.getValue() instanceof org.hl7.fhir.dstu3.model.Reference);

        var r4Ext =
                (org.hl7.fhir.r4.model.Extension) ExtensionBuilders.buildReferenceExt(FhirVersionEnum.R4, entry, false);
        assertTrue(r4Ext.getValue() instanceof org.hl7.fhir.r4.model.Reference);

        var r4bExt = (org.hl7.fhir.r4b.model.Extension)
                ExtensionBuilders.buildReferenceExt(FhirVersionEnum.R4B, entry, false);
        assertNull(r4bExt);

        var r5Ext =
                (org.hl7.fhir.r5.model.Extension) ExtensionBuilders.buildReferenceExt(FhirVersionEnum.R5, entry, false);
        assertTrue(r5Ext.getValue() instanceof org.hl7.fhir.r5.model.Reference);
    }

    @Test
    void testBuildBooleanExt() {
        SimpleEntry<String, Boolean> entry = new SimpleEntry<>("url", true);
        var dstu3Ext =
                (org.hl7.fhir.dstu3.model.Extension) ExtensionBuilders.buildBooleanExt(FhirVersionEnum.DSTU3, entry);
        assertTrue(dstu3Ext.getValue() instanceof org.hl7.fhir.dstu3.model.BooleanType);

        var r4Ext = (org.hl7.fhir.r4.model.Extension) ExtensionBuilders.buildBooleanExt(FhirVersionEnum.R4, entry);
        assertTrue(r4Ext.getValue() instanceof org.hl7.fhir.r4.model.BooleanType);

        var r4bExt = (org.hl7.fhir.r4b.model.Extension) ExtensionBuilders.buildBooleanExt(FhirVersionEnum.R4B, entry);
        assertNull(r4bExt);

        var r5Ext = (org.hl7.fhir.r5.model.Extension) ExtensionBuilders.buildBooleanExt(FhirVersionEnum.R5, entry);
        assertTrue(r5Ext.getValue() instanceof org.hl7.fhir.r5.model.BooleanType);
    }
}
