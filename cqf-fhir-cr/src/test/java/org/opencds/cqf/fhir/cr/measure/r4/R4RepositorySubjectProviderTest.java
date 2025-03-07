package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.Group.GroupType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.SubjectProviderOptions;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("squid:S125")
class R4RepositorySubjectProviderTest {

    private static final String PAT_ID_1 = "pat1";
    private static final String PAT_ID_2 = "pat2";
    private static final String PAT_ID_3 = "pat3";

    private static final String ORG_ID_1 = "org1";
    private static final String ORG_ID_2 = "org2";

    private static final String PRACTITIONER_ID_1 = "pra1";
    private static final String PRACTITIONER_ID_2 = "pra2";

    private static final String GROUP_1 = "grp1";
    private static final String GROUP_2 = "grp2";

    private static final List<String> LIST_SINGLE_NULL = Collections.singletonList(null);

    private static final R4RepositorySubjectProvider TEST_SUBJECT_ENABLE_PART_OF =
            new R4RepositorySubjectProvider(new SubjectProviderOptions().setPartOfEnabled(true));
    private static final R4RepositorySubjectProvider TEST_SUBJECT_DISABLE_PART_OF =
            new R4RepositorySubjectProvider(new SubjectProviderOptions().setPartOfEnabled(false));

    private final Repository repository = new InMemoryFhirRepository(FhirContext.forR4Cached());

    @BeforeEach
    void beforeEach() {
        final Practitioner practitioner1 = (Practitioner)
                new Practitioner().setId(new IdType(ResourceType.Practitioner.toString(), PRACTITIONER_ID_1));

        final Practitioner practitioner2 = (Practitioner)
                new Practitioner().setId(new IdType(ResourceType.Practitioner.toString(), PRACTITIONER_ID_2));

        repository.update(practitioner1);
        repository.update(practitioner2);

        final Organization org1 = (Organization) new Organization().setId(idify(ResourceType.Organization, ORG_ID_1));
        final IIdType orgId1 = repository.update(org1).getId().toUnqualifiedVersionless();

        final Organization org2 = (Organization) new Organization()
                .setPartOf(new Reference(orgId1.toUnqualifiedVersionless().getValue()))
                .setId(idify(ResourceType.Organization, ORG_ID_2));

        final IIdType orgId2 = repository.update(org2).getId().toUnqualifiedVersionless();

        final Patient patient1 = (Patient) new Patient()
                .addGeneralPractitioner(new Reference(resourcify(ResourceType.Practitioner, PRACTITIONER_ID_1)))
                .setManagingOrganization(
                        new Reference(orgId1.toUnqualifiedVersionless().getValue()))
                .setId(idify(ResourceType.Patient, PAT_ID_1));
        repository.update(patient1);

        final Patient patient2 = (Patient) new Patient()
                .addGeneralPractitioner(new Reference(resourcify(ResourceType.Practitioner, PRACTITIONER_ID_2)))
                .setManagingOrganization(
                        new Reference(orgId2.toUnqualifiedVersionless().getValue()))
                .setId(idify(ResourceType.Patient, PAT_ID_2));
        repository.update(patient2);

        // Not associated with any other resource
        final Patient patient3 = (Patient) new Patient().setId(idify(ResourceType.Patient, PAT_ID_3));
        repository.update(patient3);

        final Group group1 = (Group) new Group()
                .setType(GroupType.PRACTITIONER)
                .addMember(new GroupMemberComponent(referencifiy(ResourceType.Practitioner, PRACTITIONER_ID_1)))
                .addMember(new GroupMemberComponent(referencifiy(ResourceType.Practitioner, PRACTITIONER_ID_2)))
                .setId(idify(ResourceType.Group, GROUP_1));

        repository.update(group1);

        final Group group2 = (Group) new Group()
                .setType(GroupType.PERSON)
                .addMember(new GroupMemberComponent(referencifiy(ResourceType.Patient, PAT_ID_1)))
                .addMember(new GroupMemberComponent(referencifiy(ResourceType.Patient, PAT_ID_2)))
                .setId(idify(ResourceType.Group, GROUP_2));

        repository.update(group2);
    }

    public static Stream<Arguments> getSubjectsParams() {
        return Stream.of(

                // Null subject ID:  all patients
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        LIST_SINGLE_NULL,
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2, PAT_ID_3)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        LIST_SINGLE_NULL,
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2, PAT_ID_3)),

                // subject ID:  Organization/{orgid}
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Organization, ORG_ID_1),
                        // TODO:  LD:  this is technically incorrect:  it should be PAT_ID_1, PAT_ID_2
                        // However, due to the fact that both InMemoryFhirRepository and IgRepository
                        // do NOT support chained searches, the results can only be accurately verified
                        // with a repository that supports DAOs.
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Organization, ORG_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),

                // subject ID:  {patid}
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF, List.of(PAT_ID_1), resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        List.of(PAT_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF, List.of(PAT_ID_2), resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        List.of(PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        List.of(PAT_ID_1, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        List.of(PAT_ID_1, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),

                // subject ID:  Patient/{patid}
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),

                // subject ID:  Practitioner/{praid}
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_1, PRACTITIONER_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Practitioner, PRACTITIONER_ID_1, PRACTITIONER_ID_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),

                // Group: Practitioner
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Group, GROUP_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Group, GROUP_1),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),

                // Group: Person
                Arguments.of(
                        TEST_SUBJECT_ENABLE_PART_OF,
                        resourcifyList(ResourceType.Group, GROUP_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)),
                Arguments.of(
                        TEST_SUBJECT_DISABLE_PART_OF,
                        resourcifyList(ResourceType.Group, GROUP_2),
                        resourcifyList(ResourceType.Patient, PAT_ID_1, PAT_ID_2)));
    }

    @ParameterizedTest
    @MethodSource("getSubjectsParams")
    void getSubjects(R4RepositorySubjectProvider testSubject, List<String> subjectIds, List<String> expectedSubjects) {
        final List<String> actualSubjects =
                testSubject.getSubjects(repository, subjectIds).toList();

        assertThat(actualSubjects, containsInAnyOrder(expectedSubjects.toArray()));
    }

    private static Reference referencifiy(ResourceType resourceType, String rawId) {
        return new Reference(resourcify(resourceType, rawId));
    }

    private static IdType idify(ResourceType resourceType, String rawId) {
        return new IdType(resourceType.toString(), rawId);
    }

    private static List<String> resourcifyList(ResourceType resourceType, String... rawIds) {
        return Arrays.stream(rawIds)
                .map(rawId -> resourcify(resourceType, rawId))
                .toList();
    }

    private static String resourcify(ResourceType resourceType, String rawId) {
        return String.format("%s/%s", resourceType.toString(), rawId);
    }
}
