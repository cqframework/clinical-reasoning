package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionJson;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSuggestionActionResolver;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSuggestionResolver;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

class ResponseEncoderSuggestionResolverTest {

    @Test
    void testResolveSuggestion_WithValidAction() {
        IRequestActionAdapter action = mock(IRequestActionAdapter.class);
        ResponseEncoderSuggestionActionResolver suggestionActionResolver = mock(ResponseEncoderSuggestionActionResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = new ResponseEncoderSuggestionResolver(suggestionActionResolver);

        // Mock the methods for the action
        String expectedSuggestion = "Test Suggestion";
        String expectedId = "test-id";
        when(action.getTitle()).thenReturn(expectedSuggestion);
        when(action.getId()).thenReturn(expectedId);
        when(action.getAction()).thenReturn(List.of());

        CdsServiceResponseSuggestionJson result = suggestionResolver.resolveSuggestion(action);

        // Assertions
        assertNotNull(result);
        assertEquals(expectedSuggestion, result.getLabel());
        assertEquals(expectedId, result.getUuid());
        assertTrue(result.getActions().isEmpty());
    }

    @Test
    void testResolveSuggestion_WithNestedActions() {
        IRequestActionAdapter nestedAction = mock(IRequestActionAdapter.class);
        IRequestActionAdapter mainAction = mock(IRequestActionAdapter.class);
        ResponseEncoderSuggestionActionResolver suggestionActionResolver = mock(ResponseEncoderSuggestionActionResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = new ResponseEncoderSuggestionResolver(suggestionActionResolver);

        // Mock the methods for the nested action
        String nestedActionDescription = "Nested Action Description";
        when(nestedAction.getDescription()).thenReturn(nestedActionDescription);

        // Mock suggestion action resolver
        CdsServiceResponseSuggestionActionJson nestedSuggestionAction = new CdsServiceResponseSuggestionActionJson()
                .setDescription(nestedActionDescription);
        when(suggestionActionResolver.resolveSuggestionAction(nestedAction)).thenReturn(nestedSuggestionAction);

        // Mock the methods for the main action
        String expectedSuggestion = "Main Suggestion";
        String expectedId = "main-id";
        when(mainAction.getTitle()).thenReturn(expectedSuggestion);
        when(mainAction.getId()).thenReturn(expectedId);
        when(mainAction.getAction()).thenReturn(List.of(nestedAction));

        CdsServiceResponseSuggestionJson result = suggestionResolver.resolveSuggestion(mainAction);

        // Assertions
        assertNotNull(result);
        assertEquals(expectedSuggestion, result.getLabel());
        assertEquals(expectedId, result.getUuid());
        assertEquals(1, result.getActions().size());
        assertEquals(nestedSuggestionAction, result.getActions().get(0));
    }

    @Test
    void testResolveSuggestion_WithEmptyValues() {
        IRequestActionAdapter action = mock(IRequestActionAdapter.class);
        ResponseEncoderSuggestionActionResolver suggestionActionResolver = mock(ResponseEncoderSuggestionActionResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = new ResponseEncoderSuggestionResolver(suggestionActionResolver);

        // Mock the methods for the action with null or empty values
        when(action.getTitle()).thenReturn(null);
        when(action.getId()).thenReturn(null);
        when(action.getAction()).thenReturn(List.of());

        CdsServiceResponseSuggestionJson result = suggestionResolver.resolveSuggestion(action);

        // Assertions
        assertNotNull(result);
        assertNull(result.getLabel());
        assertNull(result.getUuid());
        assertTrue(result.getActions().isEmpty());
    }
}