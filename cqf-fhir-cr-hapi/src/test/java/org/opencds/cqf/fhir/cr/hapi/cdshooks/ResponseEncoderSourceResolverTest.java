package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderSourceResolver;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

class ResponseEncoderSourceResolverTest {

    @Test
    void testResolveSource_WithValidDocumentation() {
        // Arrange
        IRequestActionAdapter mockAction = mock(IRequestActionAdapter.class);
        IBase mockDocument = mock(IBase.class);

        @SuppressWarnings("unchecked")
        ICompositeType mockDocumentation = mock(ICompositeType.class, withSettings()
            .extraInterfaces(IBaseHasExtensions.class));
        
        @SuppressWarnings("unchecked")
        List<ICompositeType> documentationList = List.of(mockDocumentation);

        String expectedLabel = "Test Label";
        String expectedUrl = "http://example/url";
        String expectedIconUrl = "http://example/icon";

        when(mockAction.hasDocumentation()).thenReturn(true);
        when(mockAction.getDocumentation()).thenReturn((List) documentationList);
        when(mockAction.resolvePathString(mockDocumentation, "display")).thenReturn(expectedLabel);
        when(mockAction.resolvePathString(mockDocumentation, "url")).thenReturn(expectedUrl);
        when(mockAction.resolvePath(mockDocumentation, "document")).thenReturn(mockDocument);
        when(mockAction.resolvePathString(mockDocument, "url")).thenReturn(expectedIconUrl);

        ResponseEncoderSourceResolver sourceResolver = new ResponseEncoderSourceResolver();

        // Act
        CdsServiceResponseCardSourceJson result = sourceResolver.resolveSource(mockAction);

        // Assert
        assertNotNull(result);
        assertEquals(expectedLabel, result.getLabel());
        assertEquals(expectedUrl, result.getUrl());
        assertEquals(expectedIconUrl, result.getIcon());
    }
}