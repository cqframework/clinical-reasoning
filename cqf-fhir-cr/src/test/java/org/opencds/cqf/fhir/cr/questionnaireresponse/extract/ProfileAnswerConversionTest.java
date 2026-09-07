package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ProfileAnswerConversionTest {
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
        assign(version, "CodeableConcept", "String", "\"free text\"", false);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void extensionSliceCannotBorrowRootValueConstraint(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var parser = context.newJsonParser();
        var factory = IAdapterFactory.forFhirVersion(version);
        var qr = parser.parseResource("""
            {"resourceType":"QuestionnaireResponse","id":"supplied-qr","status":"completed",
             "subject":{"reference":"Patient/patientId"},
             "item":[{"linkId":"answer","answer":[{"valueCoding":%s}]}]}
            """.formatted(CODING));
        var profile = factory.createStructureDefinition(parser.parseResource("""
            {"resourceType":"StructureDefinition","id":"slice-profile",
             "url":"http://example.org/StructureDefinition/slice-profile","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
              {"id":"Observation.value[x]","path":"Observation.value[x]","type":[{"code":"CodeableConcept"}]},
              {"id":"Observation.extension:answer","path":"Observation.extension","sliceName":"answer",
               "type":[{"code":"Extension","profile":["http://example.org/coding-answer"]}]}]}}
            """));
        var request = newExtractRequestForVersion(
                version,
                new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault()),
                qr,
                null);
        var observation =
                (IBaseResource) context.getResourceDefinition("Observation").newInstance();
        var processor = new ProcessDefinitionItem();
        var identifiers = new String[] {"extension:answer", "value[x]"};
        var properties = processor.getPropertyDefinitions(
                request, context.getResourceDefinition("Observation"), profile, identifiers);
        processor.processSliceItem(
                request,
                profile,
                factory.createResource(observation),
                factory.createQuestionnaireResponse(qr).getItem().get(0).getAnswer(),
                identifiers,
                properties);
        // Nested profile resolution is not repaired by this root-conversion patch.
        // It must retain the baseline's unsupported value, never manufacture the root's datatype.
        assertEquals(
                1,
                context.newFhirPath()
                        .evaluate(observation, "extension", IBase.class)
                        .size());
        assertTrue(
                context.newFhirPath()
                        .evaluate(observation, "extension.value", IBase.class)
                        .isEmpty(),
                parser.encodeResourceToString(observation));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void extractEntryPointPreservesProfileConstrainedCoding(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var parser = context.newJsonParser();
        for (var profileForm :
                new String[] {"differential", "snapshot", "both", "sliced", "multi-cc-first", "multi-quantity-first"}) {
            for (var answerPath : profileForm.equals("sliced")
                    ? new String[] {"value[x]:quantity"}
                    : profileForm.startsWith("multi-")
                            ? new String[] {"value[x]"}
                            : new String[] {"value[x]", "valueCodeableConcept"}) {
                var constrained = """
                    {"element":[{"id":"Observation.value[x]","path":"Observation.value[x]",
                     "min":0,"max":"1","type":[{"code":"CodeableConcept"}]}]}
                    """;
                var broadSnapshot = """
                    {"element":[{"id":"Observation.value[x]","path":"Observation.value[x]",
                     "min":0,"max":"1","type":[{"code":"Quantity"},{"code":"CodeableConcept"}]}]}
                    """;
                var elements = profileForm.equals("both")
                        ? "\"snapshot\":" + broadSnapshot + ",\"differential\":" + constrained
                        : "\"" + profileForm + "\":" + constrained;
                if (profileForm.equals("sliced")) {
                    elements = """
                        "differential":{"element":[
                         {"id":"Observation.value[x]","path":"Observation.value[x]",
                          "slicing":{"discriminator":[{"type":"type","path":"$this"}],"rules":"closed"},
                          "type":[{"code":"CodeableConcept"},{"code":"Quantity"}]},
                         {"id":"Observation.value[x]:cc","path":"Observation.value[x]","sliceName":"cc",
                          "type":[{"code":"CodeableConcept"}]},
                         {"id":"Observation.value[x]:quantity","path":"Observation.value[x]","sliceName":"quantity",
                          "type":[{"code":"Quantity"}]}]}
                        """;
                }
                if (profileForm.startsWith("multi-")) {
                    elements = "\"differential\":"
                            + (profileForm.equals("multi-cc-first")
                                    ? broadSnapshot.replace(
                                            "{\"code\":\"Quantity\"},{\"code\":\"CodeableConcept\"}",
                                            "{\"code\":\"CodeableConcept\"},{\"code\":\"Quantity\"}")
                                    : broadSnapshot);
                }
                var profile = parser.parseResource("""
                    {"resourceType":"StructureDefinition","id":"entry-profile",
                     "url":"http://example.org/StructureDefinition/entry-profile","status":"active",
                     "kind":"resource","abstract":false,"type":"Observation",%s}
                    """.formatted(elements));
                if (profileForm.startsWith("multi-")) {
                    var element = IAdapterFactory.forFhirVersion(version)
                            .createStructureDefinition(profile)
                            .getElementByPath("value[x]");
                    assertEquals(2, element.getType().size());
                    assertEquals(
                            profileForm.equals("multi-cc-first") ? "CodeableConcept" : "Quantity",
                            element.getTypeCode());
                }
                if (profileForm.equals("sliced")) {
                    var element = IAdapterFactory.forFhirVersion(version)
                            .createStructureDefinition(profile)
                            .getElementByPath("value[x]");
                    assertEquals("cc", element.getSliceName());
                    assertEquals(1, element.getType().size());
                    assertEquals("CodeableConcept", element.getTypeCode());
                }
                var repository = new InMemoryFhirRepository(context);
                repository.create(profile);
                var questionnaire = parser.parseResource(
                        """
                    {"resourceType":"Questionnaire","id":"q","status":"active",
                     "extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract",
                        "valueUri":"http://example.org/StructureDefinition/entry-profile"}],
                     "item":[{"linkId":"answer","type":"%s",
                       "definition":"http://example.org/StructureDefinition/entry-profile#Observation.%s"}]}
                    """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice", answerPath));
                var qr = parser.parseResource("""
                    {"resourceType":"QuestionnaireResponse","id":"supplied-qr","status":"completed",
                     "subject":{"reference":"Patient/patientId"},
                     "item":[{"linkId":"answer","answer":[{"valueCoding":%s}]}]}
                    """.formatted(CODING));
                var request = newExtractRequestForVersion(
                        version, new LibraryEngine(repository, EvaluationSettings.getDefault()), qr, questionnaire);
                var bundle = new ExtractProcessor().extract(request);
                var observations = context.newFhirPath().evaluate(bundle, "entry.resource", IBaseResource.class);
                assertEquals(1, observations.size(), profileForm + ":" + answerPath);
                assertEquals("Observation", observations.get(0).fhirType());
                var advertisedProfile =
                        context.newFhirPath().evaluate(observations.get(0), "meta.profile", IBase.class);
                assertEquals(1, advertisedProfile.size(), "Extraction must actually resolve the supplied profile");
                assertEquals(
                        "http://example.org/StructureDefinition/entry-profile",
                        ((org.hl7.fhir.instance.model.api.IPrimitiveType<?>) advertisedProfile.get(0))
                                .getValueAsString());
                var actualValue = context.newFhirPath().evaluate(observations.get(0), "value", IBase.class);
                if (profileForm.equals("sliced") || profileForm.startsWith("multi-")) {
                    assertTrue(
                            actualValue.isEmpty(),
                            "Unsupported sliced or multi-type profile must not choose the first CodeableConcept constraint");
                    continue;
                }
                assertEquals(1, actualValue.size(), profileForm + ":" + answerPath);
                var expected = parser.parseResource("""
                    {"resourceType":"Observation","valueCodeableConcept":{"coding":[%s]}}
                    """.formatted(CODING));
                var carrier = context.getResourceDefinition("Observation").newInstance();
                IAdapterFactory.forFhirVersion(version).createResource(carrier).setValue("value", actualValue.get(0));
                assertEquals(
                        parser.encodeResourceToString(expected),
                        parser.encodeResourceToString(carrier),
                        profileForm + ":" + answerPath);
            }
        }
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
