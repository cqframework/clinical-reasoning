package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CrDiscoveryServiceDstu3Test extends BaseCdsCrDiscoveryServiceTest {
    private static final IdType PLAN_DEF_ID_TYPE = new IdType(PLAN_DEF_ID);

    private CrDiscoveryServiceDstu3 testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forDstu3Cached();
        repository = getRepository();
        restfulServer = getRestfulServer();

        testSubject = new CrDiscoveryServiceDstu3(PLAN_DEF_ID_TYPE, repository);
    }

    private static Stream<Arguments> createRequestUrlParams() {
        return Stream.of(
                Arguments.of(new DataRequirement(), List.of()),
                Arguments.of(
                        new DataRequirement().setType(ResourceType.Patient.toString()),
                        List.of("Patient?_id=Patient/{{context.patientId}}")),
                Arguments.of(
                        new DataRequirement()
                                .setType(ResourceType.Patient.toString())
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent().setPath("123"))),
                        List.of()),
                Arguments.of(
                        new DataRequirement()
                                .setType(ResourceType.Patient.toString())
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent()
                                        .setPath("123")
                                        .setValueCoding(List.of(new Coding())))),
                        List.of()),
                Arguments.of(
                        new DataRequirement()
                                .setType(ResourceType.Patient.toString())
                                .setCodeFilter(List.of(new DataRequirementCodeFilterComponent()
                                        .setPath("123")
                                        .setValueCoding(List.of(new Coding("system", "code", "display"))))),
                        List.of("Patient?_id=Patient/{{context.patientId}}&123=system|code")));
    }

    @ParameterizedTest
    @MethodSource("createRequestUrlParams")
    void createRequestUrl(DataRequirement dataRequirement, List<String> expectedUrls) {
        final List<String> requestUrls = testSubject.createRequestUrl(dataRequirement);

        assertEquals(expectedUrls, requestUrls);
    }
}
