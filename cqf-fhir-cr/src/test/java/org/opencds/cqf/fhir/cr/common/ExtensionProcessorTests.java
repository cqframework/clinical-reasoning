package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ExtensionProcessorTests {
    private static final String URL = "urn:test:expression";

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void resolvesCopiesWithoutMutatingSourceAcrossRepeatedApplications(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var factory = IAdapterFactory.forFhirContext(context);
        var expressionValue = version == FhirVersionEnum.DSTU3
                ? "\"valueString\":\"'resolved'\""
                : "\"valueExpression\":{\"language\":\"text/cql-expression\",\"expression\":\"'resolved'\"}";
        var source = context.newJsonParser().parseResource("""
                {"resourceType":"PlanDefinition", "extension":[{
                  "url":"urn:test:expression", "extension":[{
                    "url":"value", "valueString":"placeholder", "_valueString":{"extension":[{
                      "url":"http://hl7.org/fhir/StructureDefinition/cqf-expression", %s
                    }]}
                  }]
                }]}
                """.formatted(expressionValue));
        var original = context.newJsonParser().encodeResourceToString(source);
        var request = mock(ICqlOperationRequest.class);
        when(request.getSubjectId()).thenReturn(Ids.newId(version, "Patient/patient"));
        var libraryEngine = new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault());
        if (version == FhirVersionEnum.R5) {
            // The default engine does not bundle FHIR 5.0.0 model information or FHIRHelpers.
            libraryEngine = mock(LibraryEngine.class);
            when(libraryEngine.resolveExpression(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(new org.hl7.fhir.r5.model.StringType("resolved")));
        }
        when(request.getLibraryEngine()).thenReturn(libraryEngine);
        var processor = new ExtensionProcessor(ExtensionPropagationPolicy.allowOnlyUrls(Set.of(URL)));
        for (int i = 0; i < 2; i++) {
            var target = factory.createResource(context.newJsonParser().parseResource("{\"resourceType\":\"Task\"}"));
            processor.processExtensions(request, target, source, List.of());
            var copied = target.getExtension().get(0);
            var nested = (IBaseExtension<?, ?>) copied.getExtension().get(0);
            assertEquals("resolved", ((IPrimitiveType<?>) nested.getValue()).getValueAsString());
            assertNotSame(factory.createResource(source).getExtension().get(0), copied);
            nested.setUrl("changed-in-output");
            assertEquals(original, context.newJsonParser().encodeResourceToString(source));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void excludesExtensionsBeforeRequestingExpressionEvaluation(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var source = context.newJsonParser().parseResource("""
                {"resourceType":"PlanDefinition", "extension":[{"url":"urn:test:expression", "valueString":"skip"}]}
                """);
        var target = IAdapterFactory.forFhirContext(context)
                .createResource(context.newJsonParser().parseResource("{\"resourceType\":\"Task\"}"));
        var request = mock(ICqlOperationRequest.class);
        new ExtensionProcessor(ExtensionPropagationPolicy.none()).processExtensions(request, target, source, List.of());
        assertTrue(target.getExtension().isEmpty());
        verifyNoInteractions(request);
    }
}
