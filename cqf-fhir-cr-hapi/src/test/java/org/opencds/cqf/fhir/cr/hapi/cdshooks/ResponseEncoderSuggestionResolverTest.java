package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionJson;
import java.util.List;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
class ResponseEncoderSuggestionResolverTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

    private IAdapterFactory adapterFactory;
    private CdsResponseEncoderService fixture;
    private RequestGroupActionComponent requestGroupActionComponent;

    private final String expectedSuggestion = "Test Suggestion";
    private final String expectedId = "test-id";

    @BeforeEach
    void beforeEach() {
        IRepository repository = new InMemoryFhirRepository(fhirContext);
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        fixture = spy(new CdsResponseEncoderService(repository));

        requestGroupActionComponent = new RequestGroupActionComponent().setTitle(expectedSuggestion);
        requestGroupActionComponent.setId(expectedId);
    }

    @Test
    void testResolveSuggestion_noAction() {
        // given
        requestGroupActionComponent.setAction(List.of());
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        CdsServiceResponseSuggestionJson result = fixture.resolveSuggestion(actionAdapter);

        // then
        assertNotNull(result);
        assertEquals(expectedSuggestion, result.getLabel());
        assertEquals(expectedId, result.getUuid());
        assertNull(result.getActions());
    }

    @Test
    void testResolveSuggestion_withNestedActions() {
        // given
        CdsServiceResponseSuggestionActionJson nestedSuggestionAction = new CdsServiceResponseSuggestionActionJson();
        requestGroupActionComponent.addAction();
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        doReturn(nestedSuggestionAction).when(fixture).resolveSuggestionAction(any());
        CdsServiceResponseSuggestionJson result = fixture.resolveSuggestion(actionAdapter);

        // then
        assertNotNull(result);
        assertEquals(expectedSuggestion, result.getLabel());
        assertEquals(expectedId, result.getUuid());
        assertEquals(1, result.getActions().size());
        assertEquals(nestedSuggestionAction, result.getActions().get(0));
    }
}
