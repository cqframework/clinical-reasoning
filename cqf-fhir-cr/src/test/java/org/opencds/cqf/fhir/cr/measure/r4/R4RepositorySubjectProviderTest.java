package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import ca.uhn.fhir.context.FhirContext;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class R4RepositorySubjectProviderTest {

    private static final String PAT_ID_1 = "pat1";
    private static final String PAT_ID_2 = "pat2";
    private static final String ORG_ID_1 = "org1";
    private static final String ORG_ID_2 = "org2";

    private final Repository repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
    private final R4RepositorySubjectProvider testSubject = new R4RepositorySubjectProvider();

    @BeforeEach
    void beforeEach() {
        final Organization org1 =
                (Organization) new Organization().setId(new IdType(ResourceType.Organization.toString(), ORG_ID_1));
        final IIdType orgId1 = repository.update(org1).getId().toUnqualifiedVersionless();

        final Organization org2 = (Organization) new Organization()
                .setPartOf(new Reference(orgId1.toUnqualifiedVersionless().getValue()))
                .setId(new IdType(ResourceType.Organization.toString(), ORG_ID_2));

        final IIdType orgId2 = repository.update(org2).getId().toUnqualifiedVersionless();

        final Patient patient1 = new Patient();
        patient1.setId(PAT_ID_1);
        patient1.setManagingOrganization(
                new Reference(orgId1.toUnqualifiedVersionless().getValue()));

        final Patient patient2 = new Patient();
        patient2.setId(PAT_ID_2);
        patient2.setManagingOrganization(
                new Reference(orgId2.toUnqualifiedVersionless().getValue()));

        final IIdType patientId1 = repository.update(patient1).getId().toUnqualifiedVersionless();
        final IIdType patientId2 = repository.update(patient2).getId().toUnqualifiedVersionless();
    }

    public static Stream<Arguments> getSubjectsParams() {
        return Stream.of(Arguments.of(
                MeasureEvalType.SUBJECT,
                Collections.singletonList(resourcify(ResourceType.Organization, ORG_ID_1)),
                Stream.of(PAT_ID_1, PAT_ID_2)
                        .map(id -> resourcify(ResourceType.Patient, id))
                        .collect(Collectors.toList())));
    }

    @ParameterizedTest
    @MethodSource("getSubjectsParams")
    void getSubjects(MeasureEvalType measureEvalType, List<String> subjectIds, List<String> expectedSubjects) {
        final List<String> actualSubjects =
                testSubject.getSubjects(repository, measureEvalType, subjectIds).collect(Collectors.toList());

        assertThat(actualSubjects, equalTo(expectedSubjects));
    }

    private static String resourcify(ResourceType resourceType, String rawId) {
        return String.format("%s/%s", resourceType.toString(), rawId);
    }
}
