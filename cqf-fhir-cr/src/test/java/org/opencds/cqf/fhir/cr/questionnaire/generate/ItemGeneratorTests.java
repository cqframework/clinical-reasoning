package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.Ids;

@ExtendWith(MockitoExtension.class)
class ItemGeneratorTests {
    private final String ROUTE_ONE_PATIENT = "OPA-Patient1";
    private final String ROUTE_ONE_PATIENT_PROFILE =
            "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient";
    private final String SLEEP_STUDY_PATIENT = "positive";
    private final String SLEEP_STUDY_PROFILE =
            "http://example.org/sdh/dtr/aslp/StructureDefinition/aslp-sleep-study-order";
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    ItemGenerator fixture;

    @Test
    void generateShouldCatchAndNotFailOnFeatureExpressionException() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(fhirContextR4).when(repository).fhirContext();
        fixture = spy(new ItemGenerator(repository));
        var profile = new StructureDefinition();
        var request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine, profile);
        var groupItem = new QuestionnaireItemComponent();
        doReturn(groupItem).when(fixture).createQuestionnaireItem(request, null);
        var cqfExpression = new CqfExpression();
        doReturn(cqfExpression).when(fixture).getFeatureExpression(request);
        var resultException = new UnprocessableEntityException("Expression exception");
        doThrow(resultException).when(fixture).getFeatureExpressionResults(request, cqfExpression, null);
        fixture.generate(request);
    }

    /* Tests using TestItemGenerator class */

    @Test
    void generateItemDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new StringType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(6);
    }

    @Test
    void generateItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(9)
                .itemHasInitialValue("1.4.1")
                .itemHasHiddenValueExtension("1.4.1")
                .itemHasInitialExpression("1.1.1");
    }

    @Test
    void generateItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(9)
                .itemHasInitialValue("1.4.1")
                .itemHasHiddenValueExtension("1.4.1")
                .itemHasInitialExpression("1.1.1");
    }

    @Test
    void sleepStudyOrderR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .profileUrl(new CanonicalType(SLEEP_STUDY_PROFILE))
                .subjectId(SLEEP_STUDY_PATIENT)
                .then()
                .hasItemCount(3)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void sleepStudyOrderR5() {
        given().repositoryFor(fhirContextR5, "r5/pa-aslp")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(SLEEP_STUDY_PROFILE))
                .subjectId(SLEEP_STUDY_PATIENT)
                .then()
                .hasItemCount(3)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void generateQuestionnaire() {
        given().repositoryFor(fhirContextR4, "r4").when().id("test").then().hasId("test");
    }

    @Test
    void generateItemForElementWithChildren() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(6);
    }

    @Test
    void generateHiddenItem() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition2"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(3);
    }

    @Test
    void generate() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "chf-bodyweight-change"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(11);
    }

    @Test
    void generateWithLaunchContexts() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "LaunchContexts"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(3)
                .hasLaunchContextExtension(5);
    }
}
