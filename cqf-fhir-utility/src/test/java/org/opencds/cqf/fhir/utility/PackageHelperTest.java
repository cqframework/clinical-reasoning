package org.opencds.cqf.fhir.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r4.model.IntegerType;
import org.junit.jupiter.api.Test;

class PackageHelperTest {

    @Test
    void testPackageParameters() {
        var actual = packageParameters(
                FhirVersionEnum.R4, new IntegerType(1), new IntegerType(1), "searchset", null, null, false);
        assertInstanceOf(org.hl7.fhir.r4.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("count"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("offset"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("bundleType"));
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("include"));
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("isPut"));
    }

    @Test
    void testR4PackageParametersIncludeOnly() {
        var actual = packageParameters(
                FhirVersionEnum.R4, null, null, null, List.of("PlanDefinition", "ValueSet"), null, false);
        assertInstanceOf(org.hl7.fhir.r4.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("include"));
        assertEquals(
                2,
                ((org.hl7.fhir.r4.model.Parameters) actual)
                        .getParameters("include")
                        .size());
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("isPut"));
    }

    @Test
    void testR5PackageParametersIncludeOnly() {
        var actual = packageParameters(
                FhirVersionEnum.R5, null, null, null, List.of("PlanDefinition", "ValueSet"), null, false);
        assertInstanceOf(org.hl7.fhir.r5.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("include"));
        assertEquals(
                2,
                ((org.hl7.fhir.r5.model.Parameters) actual)
                        .getParameters("include")
                        .size());
        assertFalse(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("isPut"));
    }
}
