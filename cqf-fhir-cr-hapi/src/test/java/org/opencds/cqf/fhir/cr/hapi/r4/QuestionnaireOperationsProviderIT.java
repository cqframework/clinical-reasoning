package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import java.util.Arrays;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnaireDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnairePackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnairePopulateProvider;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionnaireOperationsProviderIT extends BaseCrR4TestServer {
    @Autowired
    QuestionnairePopulateProvider questionnairePopulateProvider;

    @Autowired
    QuestionnairePackageProvider questionnairePackageProvider;

    @Autowired
    QuestionnaireDataRequirementsProvider questionnaireDataRequirementsProvider;

    @Test
    void testPopulate() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-PatientData.json");
        var requestDetails = setupRequestDetails();
        var fhirContextR4 = requestDetails.getFhirContext();
        var subject = "positive";
        var context = Arrays.asList(
                (ParametersParameterComponent) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "patient"),
                        newPart(fhirContextR4, "Reference", "content", "Patient/%s".formatted(subject))),
                (ParametersParameterComponent) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "ServiceRequest"),
                        newPart(fhirContextR4, "Reference", "content", "ServiceRequest/SleepStudy")),
                (ParametersParameterComponent) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "ServiceRequest"),
                        newPart(fhirContextR4, "Reference", "content", "ServiceRequest/SleepStudy2")),
                (ParametersParameterComponent) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "Coverage"),
                        newPart(fhirContextR4, "Reference", "content", "Coverage/Coverage-positive")));
        var result = questionnairePopulateProvider.populate(
                new IdType("Questionnaire", "ASLPA1"),
                null,
                null,
                null,
                null,
                null,
                context,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                requestDetails);

        assertNotNull(result);
        assertEquals("Patient/" + subject, result.getSubject().getReference());
        assertTrue(result.getItem().get(0).getItem().get(0).hasAnswer());
    }

    @Test
    void testQuestionnairePackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        var requestDetails = setupRequestDetails();
        var result = questionnairePackageProvider.packageQuestionnaire(
                "",
                "http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA1",
                null,
                null,
                null,
                new BooleanType("true"),
                requestDetails);

        assertNotNull(result);
        assertThat(result.getEntry()).hasSize(11);
        assertEquals(
                Enumerations.FHIRAllTypes.QUESTIONNAIRE.toCode(),
                result.getEntry().get(0).getResource().fhirType());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        var requestDetails = setupRequestDetails();
        var result = questionnaireDataRequirementsProvider.getDataRequirements(
                "Questionnaire/ASLPA1", null, null, null, requestDetails);
        assertInstanceOf(Library.class, result);
        assertEquals(
                "module-definition",
                ((Library) result).getType().getCodingFirstRep().getCode());
    }
}
