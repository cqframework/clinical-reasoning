package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ResponseEncoderMethodResolveResourceTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

    private IAdapterFactory adapterFactory;
    private CdsResponseEncoderService fixture;

    @BeforeEach
    void beforeEach() {
        IRepository repository = new InMemoryFhirRepository(fhirContext);
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        fixture = spy(new CdsResponseEncoderService(repository));
    }

    @Test
    void testResolveResource_fromBundle() {
        // given
        var idType = new IdType("Patient", "123");
        var patient = new Patient().setId(idType);
        var reference = new Reference(idType);

        var bundle = new Bundle();
        bundle.addEntry().setResource((Resource) patient);
        var iResourceAdapter = adapterFactory.createResource(bundle);

        // when
        doReturn(iResourceAdapter).when(fixture).getResponseAdapter();
        var resolvedResource = fixture.resolveResource(reference);

        // then
        assertNotNull(resolvedResource);
        assertEquals(patient, resolvedResource);
    }

    @Test
    void testResolveResource_fromContainedResources() {
        // given
        var questionnaireResponse =
                (QuestionnaireResponse) new QuestionnaireResponse().setId(new IdType("QuestionnaireResponse", "1"));
        var contained1 = (Questionnaire) new Questionnaire().setId(new IdType("Questionnaire", "1"));
        var contained2 = (Questionnaire) new Questionnaire().setId(new IdType("Questionnaire", "2"));
        questionnaireResponse.setContained(List.of(contained1, contained2));

        var reference = new Reference(contained2.getIdElement());

        var questionnaireResponseAdapter = adapterFactory.createResource(questionnaireResponse);

        doReturn(questionnaireResponseAdapter).when(fixture).getResponseAdapter();

        // when
        var resolvedResource = fixture.resolveResource(reference);

        // then
        assertNotNull(resolvedResource);
        assertEquals(contained2, resolvedResource);
    }
}
