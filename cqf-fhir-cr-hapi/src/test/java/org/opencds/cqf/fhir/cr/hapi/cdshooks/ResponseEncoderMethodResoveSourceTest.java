package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import java.util.List;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class ResponseEncoderMethodResoveSourceTest {

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
    void testResolveSource_withValidDocumentation() {
        // given
        String expectedLabel = "Test Label";
        String expectedUrl = "http://example/url";
        String expectedIconUrl = "http://example/icon";

        RelatedArtifact relatedArtifact = new RelatedArtifact().setDisplay(expectedLabel).setUrl(expectedUrl);
        relatedArtifact.setDocument(new Attachment().setUrl(expectedIconUrl));
        requestGroupActionComponent.setDocumentation(List.of(relatedArtifact));

        IRequestActionAdapter requestAction = adapterFactory.createRequestAction(
            requestGroupActionComponent);

        // when
        CdsServiceResponseCardSourceJson result = fixture.resolveSource(requestAction);

        // then
        assertNotNull(result);
        assertEquals(expectedLabel, result.getLabel());
        assertEquals(expectedUrl, result.getUrl());
        assertEquals(expectedIconUrl, result.getIcon());
    }
}