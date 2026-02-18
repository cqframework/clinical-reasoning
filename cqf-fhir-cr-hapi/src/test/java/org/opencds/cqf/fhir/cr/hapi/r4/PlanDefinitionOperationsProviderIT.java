package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;
import static org.opencds.cqf.fhir.utility.Parameters.newUrlPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.repository.FhirResourceLoader;

class PlanDefinitionOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testGenerateQuestionnaire() {
        // This test is duplicating test data from the cr-test package.  Ideally it should be reusing the test resources
        // from that package
        var resourceLoader = new FhirResourceLoader(getFhirContext(), this.getClass(), List.of("pa-aslp"), true);
        resourceLoader.getResources().forEach(this::loadResource);

        var planDef = read(new IdType("PlanDefinition/ASLPA1"));
        assertNotNull(planDef);
        var sleepStudy = read(new IdType("ServiceRequest/SleepStudy"));
        var sleepStudy2 = read(new IdType("ServiceRequest/SleepStudy2"));
        var coverage = read(new IdType("Coverage/positive"));
        var expectedCodes = List.of("ASLP.A1.DE2", "ASLP.A1.DE14");
        var url = "http://example.org/sdh/dtr/aslp/PlanDefinition/ASLPA1";
        var version = "1.0.0";
        var patientID = "Patient/positive";
        var parameters = newParameters(
                getFhirContext(),
                newUrlPart(getFhirContext(), "url", url),
                newStringPart(getFhirContext(), "version", version),
                newPart(getFhirContext(), Reference.class, "subject", patientID),
                newPart(
                        getFhirContext(),
                        "parameters",
                        newParameters(
                                getFhirContext(),
                                newPart(getFhirContext(), "ServiceRequest", sleepStudy),
                                newPart(getFhirContext(), "ServiceRequest", sleepStudy2),
                                newPart(getFhirContext(), "Coverage", coverage))));

        var result = ourClient
                .operation()
                .onType(planDef.getClass())
                .named(ProviderConstants.CR_OPERATION_APPLY)
                .withParameters(parameters)
                .returnResourceType(CarePlan.class)
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getContained().size());

        var resultR5 = ourClient
                .operation()
                .onType(planDef.getClass())
                .named(ProviderConstants.CR_OPERATION_R5_APPLY)
                .withParameters(parameters)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(resultR5);
        var bundle = (Bundle) resultR5.getParameter().get(0).getResource();
        // Has generated Questionnaire
        var questionnaires = bundle.getEntry().stream()
                .filter(e -> e.hasResource() && e.getResource().fhirType().equals("Questionnaire"))
                .toList();
        assertEquals(1, questionnaires.size());
        var questionnaire = (Questionnaire) questionnaires.get(0).getResource();
        assertNotNull(questionnaire);
        assertThat(questionnaire.getItem()).hasSize(1);
        // Generated Item is correct
        var questionnaireItem = questionnaire.getItemFirstRep();
        assertTrue(questionnaireItem.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        assertThat(questionnaireItem.getText()).isEqualTo("Input Text Test");
        assertThat(questionnaireItem.getItem().get(0).getText()).isEqualTo("Sleep Study");
        // Has QuestionnaireResponse for Patient
        var questionnaireResponses = bundle.getEntry().stream()
                .filter(e -> e.hasResource() && e.getResource().fhirType().equals("QuestionnaireResponse"))
                .toList();
        assertEquals(1, questionnaireResponses.size());
        var questionnaireResponse =
                (QuestionnaireResponse) questionnaireResponses.get(0).getResource();
        assertNotNull(questionnaireResponse);
        assertEquals(patientID, questionnaireResponse.getSubject().getReference());
        // First response Item is correct
        var responseItem1 = questionnaireResponse.getItem().get(0);
        assertNotNull(responseItem1);
        assertThat(responseItem1.getText()).isEqualTo("Input Text Test");
        assertTrue(responseItem1.getItem().get(0).hasAnswer());
        // First response Item has first child item with answer
        var codingItem1 =
                (Coding) responseItem1.getItem().get(0).getAnswerFirstRep().getValue();
        assertTrue(expectedCodes.contains(codingItem1.getCode()));
        // First response Item has second child item with answer
        assertTrue(responseItem1.getItem().get(1).hasAnswer());

        assertThat(questionnaireResponse.getItem()).hasSize(2);
        // Second response Item is correct
        var responseItem2 = questionnaireResponse.getItem().get(1);
        assertThat(responseItem2.getText()).isEqualTo("Input Text Test");
        assertTrue(responseItem2.getItem().get(0).hasAnswer());
        // Second response Item has first child item with answer
        var codingItem2 =
                (Coding) responseItem2.getItem().get(0).getAnswerFirstRep().getValue();
        assertTrue(expectedCodes.contains(codingItem2.getCode()));
        // Second response Item has second child item with answer
        assertTrue(responseItem2.getItem().get(1).hasAnswer());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("PlanDefinition", "ASLPA1"))
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withNoParameters(Parameters.class)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testPackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("PlanDefinition", "ASLPA1"))
                .named(ProviderConstants.CR_OPERATION_PACKAGE)
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
        assertNotNull(result.getEntry());
        assertInstanceOf(PlanDefinition.class, result.getEntryFirstRep().getResource());
    }
}
