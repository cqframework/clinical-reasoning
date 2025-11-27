package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.ClasspathUtil;
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
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("UnstableApiUsage")
class CrDiscoveryServiceR4Test extends BaseCdsCrDiscoveryServiceTest {

    record CreateRequestUrlParams(DataRequirement dataRequirement, List<String> expectedUrls) {}

    record GetPrefetchUrlListParams(PlanDefinition planDefinition, PrefetchUrlList expectedPrefetchUrlList) {}

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

    private static final CanonicalType LIBRARY_CANONICAL_TYPE_1 = new CanonicalType(LIBRARY_CANONICAL_1);
    private static final CanonicalType LIBRARY_CANONICAL_TYPE_2 = new CanonicalType(LIBRARY_CANONICAL_2);
    private static final CanonicalType LIBRARY_CANONICAL_TYPE_3 = new CanonicalType(LIBRARY_CANONICAL_3);
    private static final CanonicalType LIBRARY_CANONICAL_TYPE_4 = new CanonicalType(LIBRARY_CANONICAL_4);
    private static final CanonicalType LIBRARY_CANONICAL_TYPE_5 = new CanonicalType(LIBRARY_CANONICAL_5);
    private static final CanonicalType LIBRARY_CANONICAL_TYPE_6 = new CanonicalType(LIBRARY_CANONICAL_6);

    private static final IdType LIBRARY_1_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib1");
    private static final IdType LIBRARY_2_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib2");
    private static final IdType LIBRARY_3_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib3");
    private static final IdType LIBRARY_4_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib4");
    private static final IdType LIBRARY_5_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib5");
    private static final IdType LIBRARY_6_ID_TYPE = new IdType(ResourceType.Library.toString(), "lib6");

    private static final Library LIBRARY_1 = buildLibrary(LIBRARY_1_ID_TYPE, LIBRARY_CANONICAL_TYPE_1);
    private static final Library LIBRARY_2 = buildLibrary(LIBRARY_2_ID_TYPE, LIBRARY_CANONICAL_TYPE_2);
    private static final Library LIBRARY_3 =
            buildLibrary(LIBRARY_3_ID_TYPE, LIBRARY_CANONICAL_TYPE_3, DATA_REQUIREMENT_PATIENT);
    private static final Library LIBRARY_4 =
            buildLibrary(LIBRARY_4_ID_TYPE, LIBRARY_CANONICAL_TYPE_4, DATA_REQUIREMENT_ENCOUNTER);
    private static final Library LIBRARY_5 = buildLibrary(
            LIBRARY_5_ID_TYPE, LIBRARY_CANONICAL_TYPE_5, DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_NON_EMPTY);
    private static final Library LIBRARY_6 = buildLibrary(
            LIBRARY_6_ID_TYPE, LIBRARY_CANONICAL_TYPE_6, DATA_REQUIREMENT_ENCOUNTER_CODE_FILTER_CODING_NON_EMPTY);

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

    private static Stream<CreateRequestUrlParams> createRequestUrlParams() {
        return Stream.of(
                new CreateRequestUrlParams(DATA_REQUIREMENT_EMPTY, List.of()),
                new CreateRequestUrlParams(DATA_REQUIREMENT_PATIENT, List.of(PREFETCH_URL_PATIENT)),
                new CreateRequestUrlParams(DATA_REQUIREMENT_PATIENT_CODE_FILTER_NO_CODING, List.of()),
                new CreateRequestUrlParams(DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_EMPTY, List.of()),
                new CreateRequestUrlParams(
                        DATA_REQUIREMENT_PATIENT_CODE_FILTER_CODING_NON_EMPTY,
                        List.of(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)),
                new CreateRequestUrlParams(
                        DATA_REQUIREMENT_ENCOUNTER_CODE_FILTER_CODING_NON_EMPTY,
                        List.of(PREFETCH_URL_ENCOUNTER_WITH_PATH_AND_CODE)));
    }

    @ParameterizedTest
    @MethodSource("createRequestUrlParams")
    void createRequestUrl(CreateRequestUrlParams params) {
        var adapter = params.dataRequirement() == null
                ? null
                : adapterFactory.createDataRequirement(params.dataRequirement());
        final List<String> requestUrls = testSubject.createRequestUrl(adapter);

        assertEquals(params.expectedUrls(), requestUrls);
    }

    private static Stream<GetPrefetchUrlListParams> getPrefetchUrlListParams() {
        return Stream.of(
                new GetPrefetchUrlListParams(null, PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(new PlanDefinition(), PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition().setType(new CodeableConcept()), PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of())),
                        PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of(new Coding()))),
                        PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition().setType(new CodeableConcept().setCoding(List.of(CODING_NON_ECA_RULE))),
                        PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_1)),
                        PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_2)),
                        PREFETCH_URL_LIST_EMPTY),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_3)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT)),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_4)),
                        getPrefetchUrlList(PREFETCH_URL_ENCOUNTER)),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_3, LIBRARY_CANONICAL_TYPE_4)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT)),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_5)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_6)),
                        getPrefetchUrlList(PREFETCH_URL_ENCOUNTER_WITH_PATH_AND_CODE)),
                new GetPrefetchUrlListParams(
                        new PlanDefinition()
                                .setType(new CodeableConcept().setCoding(List.of(CODING_ECA_RULE)))
                                .setLibrary(List.of(LIBRARY_CANONICAL_TYPE_5, LIBRARY_CANONICAL_TYPE_6)),
                        getPrefetchUrlList(PREFETCH_URL_PATIENT_WITH_PATH_AND_CODE)));
    }

    @ParameterizedTest
    @MethodSource("getPrefetchUrlListParams")
    void getPrefetchUrlList(GetPrefetchUrlListParams params) {
        var adapter =
                params.planDefinition() == null ? null : adapterFactory.createPlanDefinition(params.planDefinition());
        final PrefetchUrlList prefetchUrlList = testSubject.getPrefetchUrlList(adapter);

        assertEquals(params.expectedPrefetchUrlList(), prefetchUrlList);
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

    @Test
    void testResolveService() {
        var planDefinition = new PlanDefinition();
        planDefinition.setId("test");
        planDefinition.setUrl("test");
        repository.update(planDefinition);
        var fixture = new CrDiscoveryService(planDefinition.getIdElement(), repository);
        var response = fixture.resolveService();
        // PlanDefinition has no trigger so no service will be resolved
        assertNull(response);
        var library = new Library();
        var invalidResourceTypeResponse = fixture.resolveService(library);
        assertNull(invalidResourceTypeResponse);
    }

    @Test
    void testDiscoveryServiceWithEffectiveDataRequirementsFailsIfNotFound() {
        var planDefinition = new PlanDefinition();
        planDefinition.addExtension(CRMI_EFFECTIVE_DATA_REQUIREMENTS, new CanonicalType("#moduledefinition-example"));
        planDefinition.setId("ModuleDefinitionTest");
        planDefinition.setUrl("http://test.com/fhir/PlanDefinition/ModuleDefinitionTest");
        repository.update(planDefinition);
        var planDefAdapter = adapterFactory.createPlanDefinition(planDefinition);
        var fixture = new CrDiscoveryService(planDefinition.getIdElement(), repository);
        assertThrows(UnprocessableEntityException.class, () -> fixture.getPrefetchUrlList(planDefAdapter));
    }

    @Test
    void testDiscoveryServiceWithContainedEffectiveDataRequirements() {
        var planDefinition = new PlanDefinition();
        planDefinition.addExtension(CRMI_EFFECTIVE_DATA_REQUIREMENTS, new CanonicalType("#moduledefinition-example"));
        planDefinition.setId("ModuleDefinitionTest");
        planDefinition.setUrl("http://test.com/fhir/PlanDefinition/ModuleDefinitionTest");
        var library = ClasspathUtil.loadResource(
                fhirContext, Library.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/ModuleDefinitionExample.json");
        planDefinition.addContained(library);
        repository.update(planDefinition);
        var planDefAdapter = adapterFactory.createPlanDefinition(planDefinition);
        var fixture = new CrDiscoveryService(planDefinition.getIdElement(), repository);
        var actual = fixture.getPrefetchUrlList(planDefAdapter);
        assertNotNull(actual);
        var expected = new PrefetchUrlList();
        expected.addAll(
                List.of(
                        "Patient?_id={{context.patientId}}",
                        "Encounter?status=finished&subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292",
                        "Coverage?policy-holder=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591"));
        assertEquals(expected, actual);
    }

    @Test
    void testDiscoveryServiceWithEffectiveDataRequirements() {
        var planDefinition = new PlanDefinition();
        planDefinition.addExtension(
                CRMI_EFFECTIVE_DATA_REQUIREMENTS,
                new CanonicalType("http://hl7.org/fhir/uv/crmi/Library/moduledefinition-example"));
        planDefinition.setId("ModuleDefinitionTest");
        planDefinition.setUrl("http://test.com/fhir/PlanDefinition/ModuleDefinitionTest");
        repository.update(planDefinition);
        repository.update(ClasspathUtil.loadResource(
                fhirContext, Library.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/ModuleDefinitionExample.json"));
        var planDefAdapter = adapterFactory.createPlanDefinition(planDefinition);
        var fixture = new CrDiscoveryService(planDefinition.getIdElement(), repository);
        var actual = fixture.getPrefetchUrlList(planDefAdapter);
        assertNotNull(actual);
        var expected = new PrefetchUrlList();
        expected.addAll(
                List.of(
                        "Patient?_id={{context.patientId}}",
                        "Encounter?status=finished&subject=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.117.1.7.1.292",
                        "Coverage?policy-holder=Patient/{{context.patientId}}&type:in=http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.114222.4.11.3591"));
        assertEquals(expected, actual);
    }

    @Test
    void testResolveValueSetCodesWithComposeInclude() {
        var activeCondition = ClasspathUtil.loadResource(
                fhirContext, ValueSet.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/ValueSet-active-condition.json");
        repository.update(activeCondition);
        var response = testSubject.resolveValueSetCodes(activeCondition.getUrlElement());
        assertEquals(1, response.size());
        var codes = response.get(0).split(",");
        assertEquals(3, codes.length);
    }

    @Test
    void testResolveValueSetCodesWithExpansionContains() {
        var grouper = ClasspathUtil.loadResource(
                fhirContext,
                ValueSet.class,
                "org/opencds/cqf/fhir/cr/hapi/cdshooks/ValueSet-aslp-a1-de1-codes-grouper.json");
        repository.update(grouper);
        var response = testSubject.resolveValueSetCodes(grouper.getUrlElement());
        assertEquals(1, response.size());
        var codes = response.get(0).split(",");
        assertEquals(13, codes.length);
    }
}
