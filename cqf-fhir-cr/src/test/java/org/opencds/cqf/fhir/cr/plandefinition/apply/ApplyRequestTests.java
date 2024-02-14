package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;

@ExtendWith(MockitoExtension.class)
public class ApplyRequestTests {

    @Mock
    private LibraryEngine libraryEngine;

    @Mock
    private IInputParameterResolver inputParameterResolver;

    @Test
    void testInvalidVersionReturnsNull() {
        var request = RequestHelpers.newPDApplyRequestForVersion(FhirVersionEnum.R4B);
        var activityDef = new org.hl7.fhir.r4.model.ActivityDefinition();
        assertNull(request.transformRequestParameters(activityDef));
    }

    @Test
    void testTransformRequestParametersDstu3() {
        var params = org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.DSTU3, libraryEngine, inputParameterResolver)
                .setBundle(bundle);
        var activityDef = new org.hl7.fhir.dstu3.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void testTransformRequestParametersR4() {
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, inputParameterResolver)
                .setBundle(bundle);
        var activityDef = new org.hl7.fhir.r4.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void testTransformRequestParametersR5() {
        var params = org.opencds.cqf.fhir.utility.r5.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R5, libraryEngine, inputParameterResolver)
                .setBundle(bundle);
        var activityDef = new org.hl7.fhir.r5.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }
}
