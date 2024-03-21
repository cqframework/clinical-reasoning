package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.Ids;

@ExtendWith(MockitoExtension.class)
public class ItemGeneratorTests {
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
    IElementProcessor elementProcessor;

    @Mock
    LibraryEngine libraryEngine;

    @InjectMocks
    @Spy
    ItemGenerator fixture;

    @Test
    void generateShouldCatchAndNotFailOnFeatureExpressionException() throws ResolveExpressionException {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(fhirContextR4).when(repository).fhirContext();
        var profile = new StructureDefinition();
        var request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine, profile);
        var groupItem = new QuestionnaireItemComponent();
        doReturn(groupItem).when(fixture).createQuestionnaireItem(request, null);
        var cqfExpression = new CqfExpression();
        doReturn(cqfExpression).when(fixture).getFeatureExpression(request);
        var resultException = new ResolveExpressionException("Expression exception");
        doThrow(resultException).when(fixture).getFeatureExpressionResults(request, cqfExpression, null);
        fixture.generate(request);
    }

    @Test
    void generateShouldReturnErrorItemOnException() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(fhirContextR4).when(repository).fhirContext();
        var profile = new StructureDefinition();
        var request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine, profile);
        var result = (QuestionnaireItemComponent) fixture.generate(request);
        assertNotNull(result);
        assertEquals("DISPLAY", result.getType().name());
        assertTrue(result.getText().contains("An error occurred during item creation: "));
    }

    /* Tests using TestItemGenerator class */

    @Test
    void testGenerateItemDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new StringType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(4)
                .itemHasInitialValue();
    }

    @Test
    void testGenerateItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(4)
                .itemHasInitialValue();
    }

    @Test
    void testGenerateItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(4)
                .itemHasInitialValue();
    }

    @Test
    void testSleepStudyOrderR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .profileUrl(new CanonicalType(SLEEP_STUDY_PROFILE))
                .subjectId(SLEEP_STUDY_PATIENT)
                .then()
                .hasItemCount(2)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void testSleepStudyOrderR5() {
        given().repositoryFor(fhirContextR5, "r5/pa-aslp")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(SLEEP_STUDY_PROFILE))
                .subjectId(SLEEP_STUDY_PATIENT)
                .then()
                .hasItemCount(2)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void testGenerateQuestionnaire() {
        given().repositoryFor(fhirContextR4, "r4").when().id("test").then().hasId("test");
    }

    @Test
    void testGenerateItemForElementWithChildren() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(2);
    }

    @Test
    void testGenerateHiddenItem() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition2"))
                .subjectId(ROUTE_ONE_PATIENT)
                .then()
                .hasItemCount(2);
    }
}
