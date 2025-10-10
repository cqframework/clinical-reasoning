package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderResourceResolver;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSuggestionActionResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

class ResponseEncoderSuggestionActionResolverTest {

    @Test
    void testResolveSuggestionAction_WithDescriptionAndType() {
        // Arrange
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        ResponseEncoderResourceResolver mockResolver = mock(ResponseEncoderResourceResolver.class);
        ResponseEncoderSuggestionActionResolver resolver = new ResponseEncoderSuggestionActionResolver(mockResolver);

        String expectedDescription = "Test description";
        String expectedActionType = "actionType";
        when(mockAction.getDescription()).thenReturn(expectedDescription);
        when(mockAction.hasType()).thenReturn(true);
        ICodeableConceptAdapter mockType = mock(ICodeableConceptAdapter.class);
        when(mockAction.getType()).thenReturn(mockType);
        when(mockType.hasCoding()).thenReturn(true);
        var coding = mock(ICodingAdapter.class);
        when(mockType.getCoding()).thenReturn(List.of(coding));
        when(coding.hasCode()).thenReturn(true);
        when(coding.getCode()).thenReturn(expectedActionType);

        // Act
        CdsServiceResponseSuggestionActionJson result = resolver.resolveSuggestionAction(mockAction);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDescription, result.getDescription());
        assertEquals(expectedActionType, result.getType());
    }

    @Test
    void testResolveSuggestionAction_WithResource() {
        // Arrange
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        ResponseEncoderResourceResolver mockResolver = mock(ResponseEncoderResourceResolver.class);
        ResponseEncoderSuggestionActionResolver resolver = new ResponseEncoderSuggestionActionResolver(mockResolver);

        IBaseReference mockReference = mock(IBaseReference.class);
        when(mockAction.hasResource()).thenReturn(true);
        when(mockAction.getResource()).thenReturn(mockReference);
        IBaseResource mockResource = mock(IBaseResource.class);
        when(mockResolver.resolveResource(mockReference)).thenReturn(mockResource);

        // Act
        CdsServiceResponseSuggestionActionJson result = resolver.resolveSuggestionAction(mockAction);

        // Assert
        assertNotNull(result);
        assertEquals(mockResource, result.getResource());
    }

    @Test
    void testResolveSuggestionAction_NoTypeCodeOrResource() {
        // Arrange
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        ResponseEncoderResourceResolver mockResolver = mock(ResponseEncoderResourceResolver.class);
        ResponseEncoderSuggestionActionResolver resolver = new ResponseEncoderSuggestionActionResolver(mockResolver);

        when(mockAction.getDescription()).thenReturn("No Type or Resource");
        when(mockAction.hasType()).thenReturn(false);
        when(mockAction.hasResource()).thenReturn(false);

        // Act
        CdsServiceResponseSuggestionActionJson result = resolver.resolveSuggestionAction(mockAction);

        // Assert
        assertNotNull(result);
        assertEquals("No Type or Resource", result.getDescription());
        assertNull(result.getType());
        assertNull(result.getResource());
    }

    @Test
    void testResolveSuggestionAction_WithFireEventTypeCode() {
        // Arrange
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        ResponseEncoderResourceResolver mockResolver = mock(ResponseEncoderResourceResolver.class);
        ResponseEncoderSuggestionActionResolver resolver = new ResponseEncoderSuggestionActionResolver(mockResolver);

        when(mockAction.hasType()).thenReturn(true);
        ICodeableConceptAdapter mockType = mock(ICodeableConceptAdapter.class);
        when(mockAction.getType()).thenReturn(mockType);
        when(mockType.hasCoding()).thenReturn(true);
        var coding = mock(ICodingAdapter.class);
        when(mockType.getCoding()).thenReturn(List.of(coding));
        when(coding.getCode()).thenReturn("fire-event");

        // Act
        CdsServiceResponseSuggestionActionJson result = resolver.resolveSuggestionAction(mockAction);

        // Assert
        assertNotNull(result);
        assertNull(result.getType()); // "fire-event" should not be set as type
    }

}