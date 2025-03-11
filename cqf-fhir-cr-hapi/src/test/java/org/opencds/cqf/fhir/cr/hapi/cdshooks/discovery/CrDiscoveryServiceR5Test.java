package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CrDiscoveryServiceR5Test extends BaseCdsCrDiscoveryServiceTest {
    private static final IdType PLAN_DEF_ID_TYPE = new IdType(PLAN_DEF_ID);

    private CrDiscoveryServiceR5 testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR5Cached();
        repository = getRepository();
        restfulServer = getRestfulServer();

        testSubject = new CrDiscoveryServiceR5(PLAN_DEF_ID_TYPE, repository);
    }

    private static Stream<Arguments> createRequestUrlHappyPathParams() {
        return Stream.of(
                Arguments.of(
                        new DataRequirement().setType(FHIRTypes.PATIENT),
                        List.of("PATIENT?_id=Patient/{{context.patientId}}")),
                Arguments.of(
                        new DataRequirement()
                                .setType(FHIRTypes.PATIENT)
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent().setPath("123"))),
                        List.of()),
                Arguments.of(
                        new DataRequirement()
                                .setType(FHIRTypes.PATIENT)
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent()
                                        .setPath("123")
                                        .setCode(List.of(new Coding())))),
                        List.of()),
                Arguments.of(
                        new DataRequirement()
                                .setType(FHIRTypes.PATIENT)
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent()
                                        .setPath("123")
                                        .setCode(List.of(new Coding("system", "code", "display"))))),
                        List.of("PATIENT?_id=Patient/{{context.patientId}}&123=system|code")));
    }

    @ParameterizedTest
    @MethodSource("createRequestUrlHappyPathParams")
    void createRequestUrlHappyPath(DataRequirement dataRequirement, List<String> expectedUrls) {
        final List<String> requestUrls = testSubject.createRequestUrl(dataRequirement);

        assertEquals(expectedUrls, requestUrls);
    }
}
