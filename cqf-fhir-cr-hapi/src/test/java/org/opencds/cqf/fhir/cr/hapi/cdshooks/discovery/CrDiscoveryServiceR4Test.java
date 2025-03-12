package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CrDiscoveryServiceR4Test extends BaseCdsCrDiscoveryServiceTest {
    private static final IdType PLAN_DEF_ID_TYPE = new IdType(PLAN_DEF_ID);

    private static final PrefetchUrlList PREFETCH_URL_LIST_EMPTY = new PrefetchUrlList();

    private static final DataRequirement DATA_REQUIREMENT_EMPTY = new DataRequirement();
    private static final DataRequirement DATA_REQUIREMENT_PATIENT =
            new DataRequirement().setType(ResourceType.Patient.toString());
    private static final DataRequirement DATA_REQUIREMENT_ENCOUNTER =
            new DataRequirement().setType(ResourceType.Encounter.toString());
    private static final DataRequirement DATA_REQUIREMENT_PATIENT_CODE_FILTER_NO_CODING = new DataRequirement()
            .setType(ResourceType.Patient.toString())
            .setCodeFilter(List.of(new DataRequirement.DataRequirementCodeFilterComponent().setPath("123")));
    private static final DataRequirement DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_EMPTY = new DataRequirement()
            .setType(ResourceType.Patient.toString())
            .setCodeFilter(List.of(new DataRequirement.DataRequirementCodeFilterComponent()
                    .setPath("123")
                    .setCode(List.of(new Coding()))));
    private static final Coding CODING_NON_ECA_RULE = new Coding("system", "code", "display");
    private static final DataRequirement DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_NON_EMPTY = new DataRequirement()
            .setType(ResourceType.Patient.toString())
            .setCodeFilter(List.of(new DataRequirement.DataRequirementCodeFilterComponent()
                    .setPath("123")
                    .setCode(List.of(CODING_NON_ECA_RULE))));
    private static final DataRequirement DATA_REQUIREMENT_ENCOUNTER_CODE_FILTER_CODING_NON_EMPTY = new DataRequirement()
            .setType(ResourceType.Encounter.toString())
            .setCodeFilter(List.of(new DataRequirement.DataRequirementCodeFilterComponent()
                    .setPath("123")
                    .setCode(List.of(CODING_NON_ECA_RULE))));

    private static final CanonicalType LIBRARY_CANONICAL_1 = new CanonicalType("Library/lib1");
    private static final CanonicalType LIBRARY_CANONICAL_2 = new CanonicalType("Library/lib2");
    private static final CanonicalType LIBRARY_CANONICAL_3 = new CanonicalType("Library/lib3");
    private static final CanonicalType LIBRARY_CANONICAL_4 = new CanonicalType("Library/lib4");
    private static final CanonicalType LIBRARY_CANONICAL_5 = new CanonicalType("Library/lib5");
    private static final CanonicalType LIBRARY_CANONICAL_6 = new CanonicalType("Library/lib6");

    private static final IdType LIBRARY_1_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib1");
    private static final IdType LIBRARY_2_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib2");
    private static final IdType LIBRARY_3_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib3");
    private static final IdType LIBRARY_4_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib4");
    private static final IdType LIBRARY_5_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib5");
    private static final IdType LIBRARY_6_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib6");

    private static final Library LIBRARY_1 = buildLibrary(LIBRARY_1_ID_TYPE, LIBRARY_CANONICAL_1);
    private static final Library LIBRARY_2 = buildLibrary(LIBRARY_2_ID_TYPE, LIBRARY_CANONICAL_2);
    private static final Library LIBRARY_3 =
            buildLibrary(LIBRARY_3_ID_TYPE, LIBRARY_CANONICAL_3, DATA_REQUIREMENT_PATIENT);
    private static final Library LIBRARY_4 =
            buildLibrary(LIBRARY_4_ID_TYPE, LIBRARY_CANONICAL_4, DATA_REQUIREMENT_ENCOUNTER);
    private static final Library LIBRARY_5 =
            buildLibrary(LIBRARY_5_ID_TYPE, LIBRARY_CANONICAL_5, DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_NON_EMPTY);
    private static final Library LIBRARY_6 = buildLibrary(
            LIBRARY_6_ID_TYPE, LIBRARY_CANONICAL_6, DATA_REQUIREMENT_ENCOUNTER_CODE_FILTER_CODING_NON_EMPTY);

    private static final Coding CODING_ECA_RULE = new Coding("system", "eca-rule", "display");

    private static final String PREFETCH_URL_PATIENT = "Patient?_id=Patient/{{context.patientId}}";
    private static final String URL_PART_PATH_AND_CODE = "&123=system|code";
    private static final String PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE = PREFETCH_URL_PATIENT + URL_PART_PATH_AND_CODE;
    private static final String PREFETCH_URL_ENCOUNTER = "Encounter?patient=Patient/{{context.patientId}}";
    private static final String PREFETCH_URL_ENCOUNTER_WITH_PATH_AND_CODE =
            PREFETCH_URL_ENCOUNTER + URL_PART_PATH_AND_CODE;

    private CrDiscoveryService testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        repository = getRepository();
        restfulServer = getRestfulServer();
        adapterFactory = getAdapterFactory();

        repository.update(LIBRARY_1);
        repository.update(LIBRARY_2);
        repository.update(LIBRARY_3);
        repository.update(LIBRARY_4);
        repository.update(LIBRARY_5);
        repository.update(LIBRARY_6);

        repository.update(new Patient().setId("pat1"));
        testSubject = new CrDiscoveryService(PLAN_DEF_ID_TYPE, repository);
    }

    private static Stream<Arguments> createRequestUrlParams() {
        return Stream.of(
                Arguments.of(DATA_REQUIREMENT_EMPTY, List.of()),
                Arguments.of(DATA_REQUIREMENT_PATIENT, List.of(PREFETCH_URL_PATIENT)),
                Arguments.of(DATA_REQUIREMENT_PATIENT_CODE_FILTER_NO_CODING, List.of()),
                Arguments.of(DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_EMPTY, List.of()),
                Arguments.of(
                        DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_NON_EMPTY,
                        List.of(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)),
                Arguments.of(
                        DATA_REQUIREMENT_ENCOUNTER_CODE_FILTER_CODING_NON_EMPTY,
                        List.of(PREFETCH_URL_ENCOUNTER_WITH_PATH_AND_CODE)));
    }

    @ParameterizedTest
    @MethodSource("createRequestUrlParams")
    void createRequestUrl(DataRequirement dataRequirement, List<String> expectedUrls) {
        var adapter = dataRequirement == null ? null : adapterFactory.createDataRequirement(dataRequirement);
        final List<String> requestUrls = testSubject.createRequestUrl(adapter);

        assertEquals(expectedUrls, requestUrls);
    }

    private static Stream<Arguments> getPrefetchUrlListParams() {
        return Stream.of(
                Arguments.of(null, PREFETCH_URL_LIST_EMPTY),
                Arguments.of(new PlanDefinition(), PREFETCH_URL_LIST_EMPTY),
                Arguments.of(new PlanDefinition().setType(new CodeableConcept()), PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of())),
                        PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of(new Coding()))),
                        PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of(CODING_NON_ECA_RULE))),
                        PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_1)),
                        PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_2)),
                        PREFETCH_URL_LIST_EMPTY),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_3)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT)),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_4)),
                        getPrefetchUrlList(PREFETCH_URL_ENCOUNTER)),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_3, LIBRARY_CANONICAL_4)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT)),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_5)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_6)),
                        getPrefetchUrlList(PREFETCH_URL_ENCOUNTER_WITH_PATH_AND_CODE)),
                Arguments.of(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_5, LIBRARY_CANONICAL_6)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)));
    }

    @ParameterizedTest
    @MethodSource("getPrefetchUrlListParams")
    void getPrefetchUrlList(PlanDefinition planDefinition, PrefetchUrlList expectedPrefetchUrlList) {
        var adapter = planDefinition == null ? null : adapterFactory.createPlanDefinition(planDefinition);
        final PrefetchUrlList prefetchUrlList = testSubject.getPrefetchUrlList(adapter);

        assertEquals(expectedPrefetchUrlList, prefetchUrlList);
    }

    private static PrefetchUrlList getPrefetchUrlList(String... urls) {
        final PrefetchUrlList prefetchUrlList = new PrefetchUrlList();

        prefetchUrlList.addAll(Arrays.asList(urls));

        return prefetchUrlList;
    }

    private static Library buildLibrary(IdType libraryId, CanonicalType url, DataRequirement... dataRequirement) {
        return (Library) new Library()
                .setUrl(url.getValue())
                .setDataRequirement(dataRequirement.length > 0 ? Arrays.asList(dataRequirement) : null)
                .setId(libraryId);
    }
}
