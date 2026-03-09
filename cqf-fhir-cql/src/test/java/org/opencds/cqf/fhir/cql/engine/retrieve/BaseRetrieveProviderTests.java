package org.opencds.cqf.fhir.cql.engine.retrieve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE;

class BaseRetrieveProviderTests {
    final String PROFILE_PARAM = "_profile";
    final String CONDITION = "Condition";
    final String FHIR_CONDITION = "http://hl7.org/fhir/StructureDefinition/Condition";
    final String US_CORE_CONDITION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition";

    /**
     * Creates a concrete BaseRetrieveProvider backed by a real FhirContext and SearchParameterResolver.
     * This is needed for tests that exercise methods using the resolver (e.g. populateContextSearchParams)
     * because SearchParameterResolver is a Kotlin final class and cannot be mocked.
     */
    private static BaseRetrieveProvider createRealProvider() {
        FhirContext fhirContext = FhirContext.forR4Cached();
        TerminologyProvider terminologyProvider = Mockito.mock(TerminologyProvider.class);
        return new BaseRetrieveProvider(fhirContext, terminologyProvider, new RetrieveSettings()) {
            @Override
            public Iterable<Object> retrieve(
                    String context,
                    String contextPath,
                    Object contextValue,
                    @Nonnull String dataType,
                    String templateId,
                    String codePath,
                    Iterable<org.opencds.cqf.cql.engine.runtime.Code> codes,
                    String valueSet,
                    String datePath,
                    String dateLowPath,
                    String dateHighPath,
                    Interval dateRange) {
                return List.of();
            }
        };
    }

    @Test
    void testProfileParameterTypeIsCorrectForVersion() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.DECLARED);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Multimap<String, List<IQueryParameterType>> searchParamsR4 = HashMultimap.create();
        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, US_CORE_CONDITION);
        assertInstanceOf(
                UriParam.class,
                searchParamsR4.get(PROFILE_PARAM).stream()
                        .findFirst()
                        .orElseThrow()
                        .get(0));

        doReturn(FhirVersionEnum.R5).when(fixture).getFhirVersion();
        Multimap<String, List<IQueryParameterType>> searchParamsR5 = HashMultimap.create();
        fixture.populateTemplateSearchParams(searchParamsR5, CONDITION, US_CORE_CONDITION);
        assertInstanceOf(
                ReferenceParam.class,
                searchParamsR5.get(PROFILE_PARAM).stream()
                        .findFirst()
                        .orElseThrow()
                        .get(0));
    }

    @Test
    void testProfileParameterIsNotAddedForDefaultProfile() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.DECLARED);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Multimap<String, List<IQueryParameterType>> searchParamsR4 = HashMultimap.create();
        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, FHIR_CONDITION);
        assertTrue(searchParamsR4.isEmpty());
    }

    @Test
    void testProfileParameterIsNotAddedWhenProfileModeOff() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.OFF);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Multimap<String, List<IQueryParameterType>> searchParamsR4 = HashMultimap.create();
        fixture.populateTemplateSearchParams(searchParamsR4, CONDITION, US_CORE_CONDITION);
        assertTrue(searchParamsR4.isEmpty());
    }

    @Test
    void testPopulateTemplateSearchParams_producesMutableOrList() {
        var retrieveSettings = new RetrieveSettings().setProfileMode(PROFILE_MODE.DECLARED);
        var fixture = Mockito.mock(BaseRetrieveProvider.class, Mockito.CALLS_REAL_METHODS);
        doReturn(retrieveSettings).when(fixture).getRetrieveSettings();
        doReturn(FhirVersionEnum.R4).when(fixture).getFhirVersion();

        Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();
        fixture.populateTemplateSearchParams(searchParams, CONDITION, US_CORE_CONDITION);

        List<IQueryParameterType> orList =
                searchParams.get(PROFILE_PARAM).stream().findFirst().orElseThrow();

        assertDoesNotThrow(
                () -> {
                    orList.add(new UriParam("http://example.com/test"));
                    orList.remove(orList.size() - 1);
                },
                "orList from populateTemplateSearchParams must be mutable for downstream interceptors");
    }

    @Test
    void testPopulateContextSearchParams_referenceParam_producesMutableOrList() {
        var fixture = createRealProvider();

        Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();
        fixture.populateContextSearchParams(searchParams, "Observation", "Patient", "subject", "123");

        List<IQueryParameterType> orList =
                searchParams.get("subject").stream().findFirst().orElseThrow();

        assertInstanceOf(ReferenceParam.class, orList.get(0));
        assertDoesNotThrow(
                () -> {
                    orList.add(new ReferenceParam("Patient/456"));
                    orList.remove(orList.size() - 1);
                },
                "orList from populateContextSearchParams (reference) must be mutable for downstream interceptors");
    }

    @Test
    void testPopulateContextSearchParams_idParam_producesMutableOrList() {
        var fixture = createRealProvider();

        Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();
        fixture.populateContextSearchParams(searchParams, "Patient", null, "_id", "123");

        List<IQueryParameterType> orList =
                searchParams.get("_id").stream().findFirst().orElseThrow();

        assertInstanceOf(TokenParam.class, orList.get(0));
        assertDoesNotThrow(
                () -> {
                    orList.add(new TokenParam("456"));
                    orList.remove(orList.size() - 1);
                },
                "orList from populateContextSearchParams (_id) must be mutable for downstream interceptors");
    }

    @Test
    void testPopulateTerminologySearchParams_valueSetInModifier_producesMutableOrList() {
        var fixture = createRealProvider();

        Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();
        fixture.populateTerminologySearchParams(
                searchParams, "Observation", "code", null, "http://example.com/ValueSet/test");

        List<IQueryParameterType> orList =
                searchParams.get("code").stream().findFirst().orElseThrow();

        assertInstanceOf(TokenParam.class, orList.get(0));
        assertDoesNotThrow(
                () -> {
                    orList.add(new TokenParam("test-system", "test-code"));
                    orList.remove(orList.size() - 1);
                },
                "orList from populateTerminologySearchParams (valueSet IN modifier) must be mutable"
                        + " for downstream interceptors");
    }
}
