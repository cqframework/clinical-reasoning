package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;

class QuestionnaireOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testPopulate() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-PatientData.json");
        var requestDetails = setupRequestDetails();
        var fhirContextR4 = requestDetails.getFhirContext();
        var subject = "positive";
        var parameters = newParameters(
                getFhirContext(),
                newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "patient"),
                        newPart(fhirContextR4, "Reference", "content", "Patient/%s".formatted(subject))),
                newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "ServiceRequest"),
                        newPart(fhirContextR4, "Reference", "content", "ServiceRequest/SleepStudy")),
                newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "ServiceRequest"),
                        newPart(fhirContextR4, "Reference", "content", "ServiceRequest/SleepStudy2")),
                newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "Coverage"),
                        newPart(fhirContextR4, "Reference", "content", "Coverage/Coverage-positive")));
        var result = ourClient
                .operation()
                .onInstance(new IdType("Questionnaire", "ASLPA1"))
                .named(ProviderConstants.CR_OPERATION_POPULATE)
                .withParameters(parameters)
                .returnResourceType(QuestionnaireResponse.class)
                .execute();

        assertNotNull(result);
        assertEquals("Patient/" + subject, result.getSubject().getReference());
        assertTrue(result.getItem().get(0).getItem().get(0).hasAnswer());
    }

    @Test
    void testQuestionnairePackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("Questionnaire", "ASLPA1"))
                .named(ProviderConstants.CR_OPERATION_PACKAGE)
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();

        assertNotNull(result);
        assertThat(result.getEntry()).hasSize(11);
        assertEquals(
                Enumerations.FHIRAllTypes.QUESTIONNAIRE.toCode(),
                result.getEntry().get(0).getResource().fhirType());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-QuestionnairePackage.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("Questionnaire", "ASLPA1"))
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withNoParameters(Parameters.class)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }
}
