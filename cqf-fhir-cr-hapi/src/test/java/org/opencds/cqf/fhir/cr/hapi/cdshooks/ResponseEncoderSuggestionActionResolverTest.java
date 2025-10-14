package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class ResponseEncoderSuggestionActionResolverTest {

    private IRepository repository;
    private IAdapterFactory adapterFactory;
    private CdsResponseEncoderService fixture;
    private RequestGroupActionComponent requestGroupActionComponent;

    @BeforeEach
    void beforeEach() {
        adapterFactory = new AdapterFactory();
        repository = mock(IRepository.class);
        fixture = spy(new CdsResponseEncoderService(repository, adapterFactory));

        requestGroupActionComponent = new RequestGroupActionComponent()
            .setTitle("Test Title")
            .setDescription("Test Description");
    }

    @Test
    void testResolveSuggestionAction_withDescriptionAndType() {
        // given
        String expectedDescription = "Test description";
        String expectedActionType = "actionType";

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setCode(expectedActionType);

        requestGroupActionComponent.setType(codeableConcept);
        requestGroupActionComponent.setDescription(expectedDescription);
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        CdsServiceResponseSuggestionActionJson result = fixture.resolveSuggestionAction(actionAdapter);

        // then
        assertNotNull(result);
        assertEquals(expectedDescription, result.getDescription());
        assertEquals(expectedActionType, result.getType());
    }

    @Test
    void testResolveSuggestionAction_withResource() {
        // given
        Reference reference = new Reference("Action/123");
        IBaseResource expectedResource = mock(IBaseResource.class);
        requestGroupActionComponent.setResource(reference);
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);
        doReturn(expectedResource).when(fixture).resolveResource(reference);

        // when
        CdsServiceResponseSuggestionActionJson result = fixture.resolveSuggestionAction(actionAdapter);

        // then
        assertNotNull(result);
        assertEquals(expectedResource, result.getResource());
    }

    @Test
    void testResolveSuggestionAction_noTypeCodeOrResource() {
        // given
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        CdsServiceResponseSuggestionActionJson result = fixture.resolveSuggestionAction(actionAdapter);

        // then
        assertNotNull(result);
        assertNull(result.getType());
        assertNull(result.getResource());
    }

    @Test
    void testResolveSuggestionAction_withFireEventTypeCode() {
        // given
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setCode("fire-event");

        requestGroupActionComponent.setType(codeableConcept);
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        CdsServiceResponseSuggestionActionJson result = fixture.resolveSuggestionAction(actionAdapter);

        // then
        assertNotNull(result);
        assertNull(result.getType()); // "fire-event" should not be set as type
    }

}