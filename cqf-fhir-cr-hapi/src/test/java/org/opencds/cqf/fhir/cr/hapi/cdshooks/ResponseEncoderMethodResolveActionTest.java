package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseLinkJson;
import java.util.List;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.ActionSelectionBehavior;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ResponseEncoderMethodResolveActionTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

    private IAdapterFactory adapterFactory;
    private CdsResponseEncoderService fixture;
    private RequestGroupActionComponent requestGroupActionComponent;

    @BeforeEach
    void beforeEach() {
        IRepository repository = new InMemoryFhirRepository(fhirContext);
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        fixture = spy(new CdsResponseEncoderService(repository));

        requestGroupActionComponent =
                new RequestGroupActionComponent().setTitle("Test Title").setDescription("Test Description");
    }

    @Test
    void testResolveAction_shouldSetSummaryAndDetailsCorrectly() {
        // given
        var expectedTitle = "Sample Title";
        var expectedDescription = "Sample Description";
        requestGroupActionComponent =
                new RequestGroupActionComponent().setTitle(expectedTitle).setDescription(expectedDescription);

        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        var result = fixture.resolveAction(actionAdapter, List.of());

        // then
        assertEquals(expectedTitle, result.getSummary());
        assertEquals(expectedDescription, result.getDetail());
    }

    @Test
    void testResolveAction_shouldSetLinksCorrectly() {
        // given
        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);
        var expectedLinkLabel = "Link Label";
        var expectedUrl = "http://example.com";
        var link = new CdsServiceResponseLinkJson().setLabel(expectedLinkLabel).setUrl(expectedUrl);

        // when
        var result = fixture.resolveAction(actionAdapter, List.of(link));

        // then
        assertNotNull(result.getLinks());
        assertEquals(1, result.getLinks().size());
        assertEquals(expectedLinkLabel, result.getLinks().get(0).getLabel());
        assertEquals(expectedUrl, result.getLinks().get(0).getUrl());
    }

    @Test
    void testResolveAction_shouldSetIndicatorCorrectly() {
        // given
        requestGroupActionComponent.setPriority(RequestPriority.ROUTINE);

        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        var result = fixture.resolveAction(actionAdapter, List.of());

        // then
        assertNotNull(result.getIndicator());
        assertEquals(CdsServiceIndicatorEnum.INFO, result.getIndicator());
    }

    @Test
    void testResolveAction_shouldSetSourceCorrectlyWhenDocumentationExists() {
        // given
        requestGroupActionComponent.setDocumentation(List.of(mock(RelatedArtifact.class)));

        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        var responseCardSourceJson =
                new CdsServiceResponseCardSourceJson().setLabel("Mock Label").setUrl("http://mocksource.com");
        doReturn(responseCardSourceJson).when(fixture).resolveSource(actionAdapter);

        // when
        var result = fixture.resolveAction(actionAdapter, List.of());

        // then
        assertNotNull(result.getSource());
        assertEquals(responseCardSourceJson, result.getSource());
    }

    @Test
    void testResolveAction_shouldSetSelectionBehaviorCorrectly() {
        // given
        var expectedBehavior = ActionSelectionBehavior.ALL;

        requestGroupActionComponent.setSelectionBehavior(expectedBehavior).setAction(List.of());

        var actionAdapter = adapterFactory.createRequestAction(requestGroupActionComponent);

        // when
        var result = fixture.resolveAction(actionAdapter, List.of());

        // then
        assertEquals(expectedBehavior.toCode(), result.getSelectionBehaviour());
    }
}
