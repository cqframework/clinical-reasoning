package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.TestOperationProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionApplyProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.utility.repository.FhirResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

class PlanDefinitionOperationsProviderIT extends BaseCrR4TestServer {
    @Autowired
    PlanDefinitionApplyProvider planDefinitionApplyProvider;

    @Autowired
    PlanDefinitionDataRequirementsProvider planDefinitionDataRequirementsProvider;

    @Test
    void testGenerateQuestionnaire() {
        var resourceLoader = new FhirResourceLoader(
                getFhirContext(), TestOperationProvider.class, List.of("shared/r4/pa-aslp"), true);
        resourceLoader.getResources().forEach(this::loadResource);

        var requestDetails = setupRequestDetails();
        var url = "http://example.org/sdh/dtr/aslp/PlanDefinition/ASLPA1";
        var version = "1.0.0";
        var patientID = "positive";
        var parameters = new Parameters()
                .addParameter("Service Request Id", "SleepStudy")
                .addParameter("Service Request Id", "SleepStudy2");
        var result = (CarePlan) planDefinitionApplyProvider.apply(
                null,
                null,
                null,
                url,
                version,
                patientID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                parameters,
                new BooleanType(true),
                null,
                null,
                null,
                null,
                null,
                requestDetails);

        assertNotNull(result);
        assertEquals(1, result.getContained().size());

        var resultR5 = (Bundle) planDefinitionApplyProvider.applyR5(
                null,
                null,
                null,
                url,
                version,
                patientID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                parameters,
                new BooleanType(true),
                null,
                null,
                null,
                null,
                null,
                requestDetails);

        assertNotNull(resultR5);
        var questionnaireResponses = resultR5.getEntry().stream()
                .filter(e -> e.hasResource() && e.getResource().fhirType().equals("QuestionnaireResponse"))
                .toList();
        assertEquals(1, questionnaireResponses.size());
        var questionnaireResponse =
                (QuestionnaireResponse) questionnaireResponses.get(0).getResource();
        assertNotNull(questionnaireResponse);
        var questionnaires = resultR5.getEntry().stream()
                .filter(e -> e.hasResource() && e.getResource().fhirType().equals("Questionnaire"))
                .toList();
        assertEquals(1, questionnaires.size());
        var questionnaire = (Questionnaire) questionnaires.get(0).getResource();
        assertNotNull(questionnaire);
        assertThat(questionnaire.getItem().get(0).getItem().get(0).getText()).isEqualTo("Sleep Study");
        assertTrue(questionnaireResponse.getItem().get(0).getItem().get(0).hasAnswer());
        assertTrue(questionnaireResponse.getItem().get(0).getItem().get(1).hasAnswer());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var requestDetails = setupRequestDetails();
        var result = planDefinitionDataRequirementsProvider.getDataRequirements(
                "PlanDefinition/ASLPA1", null, null, null, requestDetails);
        assertInstanceOf(Library.class, result);
        assertEquals(
                "module-definition",
                ((Library) result).getType().getCodingFirstRep().getCode());
    }
}
