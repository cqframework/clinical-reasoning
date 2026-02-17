package org.opencds.cqf.fhir.cql.engine.retrieve;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.UriParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE;

class BaseRetrieveProviderTests {
    final String PROFILE_PARAM = "_profile";
    final String CONDITION = "Condition";
    final String FHIR_CONDITION = "http://hl7.org/fhir/StructureDefinition/Condition";
    final String US_CORE_CONDITION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition";

    @Test
    void testProfileParameterTypeIsCorrectForVersion() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.DECLARED);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Map<String, List<IQueryParameterType>> searchParamsR4 = new HashMap<>();
//        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, US_CORE_CONDITION);
        assertInstanceOf(UriParam.class, searchParamsR4.get(PROFILE_PARAM).get(0));

        doReturn(FhirVersionEnum.R5).when(fixture).getFhirVersion();
        Map<String, List<IQueryParameterType>> searchParamsR5 = new HashMap<>();
//        fixture.populateTemplateSearchParams(searchParamsR5, CONDITION, US_CORE_CONDITION);
        assertInstanceOf(ReferenceParam.class, searchParamsR5.get(PROFILE_PARAM).get(0));
    }

    @Test
    void testProfileParameterIsNotAddedForDefaultProfile() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.DECLARED);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Map<String, List<IQueryParameterType>> searchParamsR4 = new HashMap<>();
//        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, FHIR_CONDITION);
        assertTrue(searchParamsR4.isEmpty());
    }

    @Test
    void testProfileParameterIsNotAddedWhenProfileModeOff() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.OFF);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Map<String, List<IQueryParameterType>> searchParamsR4 = new HashMap<>();
//        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, US_CORE_CONDITION);
        assertTrue(searchParamsR4.isEmpty());
    }
}
