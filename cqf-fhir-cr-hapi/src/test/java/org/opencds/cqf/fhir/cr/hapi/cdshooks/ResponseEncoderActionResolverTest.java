package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseLinkJson;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderActionResolver;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderIndicatorResolver;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSourceResolver;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSuggestionResolver;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

class ResponseEncoderActionResolverTest {

    @Test
    void testResolveAction_shouldSetSummaryAndDetailsCorrectly() {

        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        String expectedTitle = "Sample Title";
        String expectedDescription = "Sample Description";

        when(mockAction.getTitle()).thenReturn(expectedTitle);
        when(mockAction.getDescription()).thenReturn(expectedDescription);
        when(mockAction.hasPriority()).thenReturn(false);
        when(mockAction.hasDocumentation()).thenReturn(false);
        when(mockAction.hasSelectionBehavior()).thenReturn(false);

        ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
        ResponseEncoderSourceResolver sourceResolver = Mockito.mock(ResponseEncoderSourceResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = Mockito.mock(ResponseEncoderSuggestionResolver.class);

        ResponseEncoderActionResolver actionResolver = new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);
        CdsServiceResponseCardJson result = actionResolver.resolveAction(mockAction, List.of());

        assertEquals(expectedTitle, result.getSummary());
        assertEquals(expectedDescription, result.getDetail());
    }

    @Test
    void testResolveAction_shouldSetLinksCorrectly() {
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        when(mockAction.getTitle()).thenReturn("Test Title");
        when(mockAction.getDescription()).thenReturn("Test Description");
        when(mockAction.hasPriority()).thenReturn(false);
        when(mockAction.hasDocumentation()).thenReturn(false);

        String expectedLinkLabel = "Link Label";
        String expectedUrl = "http://example.com";
        CdsServiceResponseLinkJson link = new CdsServiceResponseLinkJson().setLabel(expectedLinkLabel).setUrl(expectedUrl);
        List<CdsServiceResponseLinkJson> links = List.of(link);

        ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
        ResponseEncoderSourceResolver sourceResolver = Mockito.mock(ResponseEncoderSourceResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = Mockito.mock(ResponseEncoderSuggestionResolver.class);

        ResponseEncoderActionResolver actionResolver = new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);
        CdsServiceResponseCardJson result = actionResolver.resolveAction(mockAction, links);

        assertNotNull(result.getLinks());
        assertEquals(1, result.getLinks().size());
        assertEquals(expectedLinkLabel, result.getLinks().get(0).getLabel());
        assertEquals(expectedUrl, result.getLinks().get(0).getUrl());
    }

    @Test
    void testResolveAction_shouldSetIndicatorCorrectly() {
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        when(mockAction.getTitle()).thenReturn("Mock Title");
        when(mockAction.getPriority()).thenReturn("routine");
        when(mockAction.hasPriority()).thenReturn(true);

        ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
        ResponseEncoderSourceResolver sourceResolver = Mockito.mock(ResponseEncoderSourceResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = Mockito.mock(ResponseEncoderSuggestionResolver.class);

        ResponseEncoderActionResolver actionResolver = new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);
        CdsServiceResponseCardJson result = actionResolver.resolveAction(mockAction, List.of());

        assertNotNull(result.getIndicator());
        assertEquals(CdsServiceIndicatorEnum.INFO, result.getIndicator());
    }

    @Test
    void testResolveAction_shouldSetSourceCorrectlyWhenDocumentationExists() {
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        CdsServiceResponseCardSourceJson mockSource = new CdsServiceResponseCardSourceJson().setLabel("Mock Label").setUrl("http://mocksource.com");

        when(mockAction.hasDocumentation()).thenReturn(true);

        ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
        ResponseEncoderSourceResolver sourceResolver = Mockito.mock(ResponseEncoderSourceResolver.class);
        when(sourceResolver.resolveSource(mockAction)).thenReturn(mockSource);

        ResponseEncoderSuggestionResolver suggestionResolver = Mockito.mock(ResponseEncoderSuggestionResolver.class);
        ResponseEncoderActionResolver actionResolver = new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);

        CdsServiceResponseCardJson result = actionResolver.resolveAction(mockAction, List.of());

        assertNotNull(result.getSource());
        assertEquals(mockSource, result.getSource());
    }

    @Test
    void testResolveAction_shouldSetSelectionBehaviorCorrectly() {
        String expectedBehavior = "any";
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        when(mockAction.getSelectionBehavior()).thenReturn(expectedBehavior);
        when(mockAction.hasSelectionBehavior()).thenReturn(true);
        when(mockAction.getAction()).thenReturn(List.of());

        ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
        ResponseEncoderSourceResolver sourceResolver = Mockito.mock(ResponseEncoderSourceResolver.class);
        ResponseEncoderSuggestionResolver suggestionResolver = Mockito.mock(ResponseEncoderSuggestionResolver.class);

        ResponseEncoderActionResolver actionResolver = new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);
        CdsServiceResponseCardJson result = actionResolver.resolveAction(mockAction, List.of());

        assertEquals(expectedBehavior, result.getSelectionBehaviour());
    }
}