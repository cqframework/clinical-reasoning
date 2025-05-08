package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;

public class PackageHelperTest {

    @Test
    void testPackageParameters() {
        var actual = packageParameters(FhirVersionEnum.R4, null, false);
        assertInstanceOf(org.hl7.fhir.r4.model.Parameters.class, actual);
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("isPut"));
    }
}
