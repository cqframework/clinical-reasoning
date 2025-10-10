package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderResourceResolver;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class ResponseEncoderResourceResolverTest {

    @Test
    void testResolveResourceFromBundle() {
        // Arrange
        var adapterFactory = new AdapterFactory();
        var patient = new Patient();
        var bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        var iResourceAdapter = adapterFactory.createResource(bundle);
        var idType = new IdType("Patient", "123");

        patient.setId(idType);
        Reference reference = new Reference(idType);

        var resolver = new ResponseEncoderResourceResolver(iResourceAdapter);

        // Act
        var resolvedResource = resolver.resolveResource(reference);

        // Assert
        assertNotNull(resolvedResource);
        assertEquals(patient, resolvedResource);

    }

    @Test
    void testResolveResourceFromContainedResources() {
        // Arrange
        var adapterFactory = new AdapterFactory();
        var questionnaireResponse = (QuestionnaireResponse) new QuestionnaireResponse().setId(new IdType("QuestionnaireResponse", "1"));
        var contained1 = (Questionnaire) new Questionnaire().setId(new IdType("Questionnaire", "1"));
        var contained2 = (Questionnaire) new Questionnaire().setId(new IdType("Questionnaire", "2"));
        questionnaireResponse.setContained(List.of(contained1, contained2));

        var reference = new Reference(contained2.getIdElement());

        var questionnaireResponseAdapter = adapterFactory.createResource(questionnaireResponse);

        var resolver = new ResponseEncoderResourceResolver(questionnaireResponseAdapter);

        // Act
        var resolvedResource = resolver.resolveResource(reference);

        // Assert
        assertNotNull(resolvedResource);
        assertEquals(contained2, resolvedResource);

    }

    @Test
    void testAdapterContainedResources() {
        // Arrange
        var adapterFactory = new AdapterFactory();
        var questionnaireResponse = (QuestionnaireResponse) new QuestionnaireResponse().setId(new IdType("QuestionnaireResponse", "1"));
        var contained1 = (Questionnaire) new Questionnaire().setId(new IdType("Questionnaire", "1"));

        questionnaireResponse.setContained(List.of(contained1));

        var questionnaireResponseAdapter = adapterFactory.createResource(questionnaireResponse);

        IBaseResource resource = questionnaireResponseAdapter.getContained().get(0);
        assertEquals("Questionnaire", resource.getIdElement().getResourceType());
        assertEquals("1", resource.getIdElement().getIdPart());
        assertEquals("Questionnaire/1", resource.getIdElement().getValueAsString());

    }


}