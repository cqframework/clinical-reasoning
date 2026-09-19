package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ProfileAnswerConversionTests {
    private static final String CODING = """
        {"system":"http://example.org/choices","version":"2026","code":"no",
         "display":"Explicit negative","userSelected":true,
         "extension":[{"url":"http://example.org/annotation","valueString":"retained"}]}
        """;

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void profileConstrainedCodingIsAssignedWithoutLosingMetadata(FhirVersionEnum version) {
        assign(version, "CodeableConcept", "Coding", CODING, true);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void matchingCodeableConceptIsPreserved(FhirVersionEnum version) {
        assign(version, "CodeableConcept", "CodeableConcept", "{\"coding\":[" + CODING + "]}", true);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void explicitBooleanFalseIsPreserved(FhirVersionEnum version) {
        assign(version, "boolean", "Boolean", "false", true);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void codingIntoRepeatedCodeableConceptPropertyIsPreserved(FhirVersionEnum version) {
        assign(version, "CodeableConcept", "Coding", CODING, true, "category");
    }

    // Characterizes an existing unsupported mismatch; this patch repairs Coding only.
    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void stringToCodeableConceptRemainsUnsupported(FhirVersionEnum version) {
        assign(version, "CodeableConcept", "String", "\"free text\"", false, "valueCodeableConcept");
    }

    private void assign(FhirVersionEnum version, String target, String source, String value, boolean supported) {
        assign(version, target, source, value, supported, "value[x]");
    }

    private void assign(
            FhirVersionEnum version, String target, String source, String value, boolean supported, String property) {
        var context = FhirContext.forCached(version);
        var parser = context.newJsonParser();
        var factory = IAdapterFactory.forFhirVersion(version);
        var qr = parser.parseResource("""
            {"resourceType":"QuestionnaireResponse","id":"supplied-qr","status":"completed",
             "subject":{"reference":"Patient/patientId"},
             "item":[{"linkId":"answer","answer":[{"value%s":%s}]}]}
            """.formatted(
                        source.equals("CodeableConcept") ? "Boolean" : source,
                        source.equals("CodeableConcept") ? "false" : value));
        var answer = factory.createQuestionnaireResponse(qr)
                .getItem()
                .get(0)
                .getAnswer()
                .get(0)
                .getValue();
        // CodeableConcept is a resource value, not a legal QR answer datatype.
        if (source.equals("CodeableConcept")) {
            var inputObservation = parser.parseResource("""
                {"resourceType":"Observation","valueCodeableConcept":%s}
                """.formatted(value));
            answer = context.newFhirPath()
                    .evaluate(inputObservation, "value", IBase.class)
                    .get(0);
        }
        assertNotNull(answer);
        var profile =
                factory.createStructureDefinition(parser.parseResource("""
            {"resourceType":"StructureDefinition","id":"answer-profile",
             "url":"http://example.org/StructureDefinition/answer-profile","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[{"id":"Observation.%s","path":"Observation.%s",
             "type":[{"code":"%s"}]}]}}
            """.formatted(property, property, target)));
        assertEquals(target, profile.getElementByPath(property).getTypeCode());
        var request = newExtractRequestForVersion(
                version,
                new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault()),
                qr,
                null);
        var observation =
                (IBaseResource) context.getResourceDefinition("Observation").newInstance();
        var path = context.getResourceDefinition("Observation").getChildByName(property);
        var processor = new ProcessDefinitionItem();
        processor.setAnswerValue(request, factory.createResource(observation), path, property, answer, profile);
        var valuePath = property.equals("value[x]") ? "value" : property;
        var values = context.newFhirPath().evaluate(observation, valuePath, IBase.class);
        if (!supported) {
            assertTrue(values.isEmpty(), "Characterized unsupported conversion must not look successful");
            return;
        }
        assertEquals(1, values.size());
        IBase actual = values.get(0);
        assertEquals(target, actual.fhirType());
        var expectedValue = source.equals("Coding") ? "{\"coding\":[" + CODING + "]}" : value;
        var field = property.equals("value[x]") ? "value" + (target.equals("boolean") ? "Boolean" : target) : property;
        if (property.equals("category")) {
            expectedValue = "[" + expectedValue + "]";
        }
        var expectedResource = parser.parseResource("""
                {"resourceType":"Observation","%s":%s}
                """.formatted(field, expectedValue));
        assertEquals(parser.encodeResourceToString(expectedResource), parser.encodeResourceToString(observation));
    }
}
