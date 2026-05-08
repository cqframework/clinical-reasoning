package org.opencds.cqf.fhir.cr.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.group.TestGroup.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.group.TestGroup.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.DeleteProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ReleaseProcessor;
import org.opencds.cqf.fhir.cr.common.RetireProcessor;
import org.opencds.cqf.fhir.cr.common.ReviseProcessor;
import org.opencds.cqf.fhir.cr.common.WithdrawProcessor;
import org.opencds.cqf.fhir.cr.group.r4.EvaluateProcessor;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2699")
class GroupProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Mock
    LibraryEngine libraryEngine;

    @Test
    void defaultSettings() {
        var repository = new IgRepository(
                fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4/group-sr"));
        var processor = new GroupProcessor(repository);
        assertNotNull(processor.settings());
    }

    @Test
    void testRequest() {
        var group = new Group();
        var request = RequestHelpers.newGroupEvaluateRequestForVersion(FhirVersionEnum.R4, libraryEngine, group);
        assertEquals("evaluate", request.getOperationName());
        assertEquals("patientId", request.getSubjectId().getIdPart());
        assertNull(request.getContextVariable());
    }

    @Test
    void processor() {
        var repository = new IgRepository(
                fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4/group-sr"));
        var processor = new GroupProcessor(
                repository,
                CrSettings.getDefault(),
                List.of(
                        new PackageProcessor(repository),
                        new ReleaseProcessor(repository),
                        new DataRequirementsProcessor(repository),
                        new EvaluateProcessor(repository, EvaluationSettings.getDefault()),
                        new DeleteProcessor(repository),
                        new RetireProcessor(repository),
                        new WithdrawProcessor(repository),
                        new ReviseProcessor(repository),
                        new ArtifactDiffProcessor()));

        assertNotNull(processor.settings());
        var result = processor.resolveGroup(
                Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "Group", "DefaultSearchParameter")));
        assertNotNull(result);
    }

    /*
    @Test
    void packageR4() {
        given().repositoryFor(fhirContextR4, "r4/group-sr")
            .when()
            .groupId("DefaultSearchParameter")
            .thenPackage()
            .hasEntry(4);
    }
    */

    /*
    @Test
    void packageR5() {
        given().repositoryFor(fhirContextR5, "r5")
            .when()
            .libraryId("OutpatientPriorAuthorizationPrepopulation")
            .isPut(Boolean.TRUE)
            .thenPackage()
            .hasEntry(2);
    }
    */

    /*
    @Test
    void dataRequirementsR4() {
        given().repositoryFor(fhirContextR4, "r4/group-sr")
            .when()
            .groupId("DefaultSearchParameter")
            .thenDataRequirements()
            .hasDataRequirements(2);
    }
    */

    /*
    @Test
    void dataRequirementsR5() {
        given().repositoryFor(fhirContextR5, "r5")
            .when()
            .libraryId("OutpatientPriorAuthorizationPrepopulation")
            .thenDataRequirements()
            .hasDataRequirements(19);
    }
    */

    /*
    @Test
    void evaluateException() {
        given().repositoryFor(fhirContextR4, "r4/group-sr")
            .when()
            .groupId("BadGroup")
            .thenEvaluate()
            .hasOperationOutcome();
    }
    */

    @Test
    void evaluateR4() {
        given().repositoryFor(fhirContextR4, "r4/group-sr")
                .when()
                .groupId("DefaultSearchParameter")
                .thenEvaluate()
                .hasResults(0);
    }

    /*
    @Test
    void evaluateR5() {
        given().repositoryFor(fhirContextR5, "r5")
            .when()
            .libraryId("OutpatientPriorAuthorizationPrepopulation")
            .subjectId("OPA-Patient1")
            .thenEvaluate()
            .hasResults(49);
    }
    */
}
