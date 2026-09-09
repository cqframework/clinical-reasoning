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

class NestedProfileAnswerConversionTest {
    private static final String PROFILE = "http://example.org/StructureDefinition/answers";
    private static final String EXTENSION = "http://example.org/answer-profile";
    private static final String CODING = """
        {"system":"http://example.org/choices","version":"2026","code":"no",
         "display":"Explicit negative","userSelected":true,
         "extension":[{"url":"http://example.org/annotation","valueString":"retained"}]}
        """;

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void extensionAnswersUseTheirOwnProfile(FhirVersionEnum version) {
        for (var target : new String[] {"Coding", "CodeableConcept"}) {
            for (var form : new String[] {"referenced", "inline", "snapshot", "both", "parent-both"}) {
                var context = FhirContext.forCached(version);
                var parser = context.newJsonParser();
                var repository = new InMemoryFhirRepository(context);
                repository.create(parser.parseResource("""
                    {"resourceType":"StructureDefinition","id":"answer","url":"%s",
                     "status":"active","kind":"complex-type","abstract":false,"type":"Extension",
                     "%s":{"element":[{"id":"Extension.value[x]","path":"Extension.value[x]",
                     "type":[{"code":"%s"}]}]}%s}
                    """.formatted(
                                EXTENSION,
                                form.equals("snapshot") || form.equals("both") ? "snapshot" : "differential",
                                target,
                                form.equals("both")
                                        ? ",\"differential\":{\"element\":[{\"id\":\"Extension.value[x]\",\"path\":\"Extension.value[x]\",\"min\":1}]}"
                                        : "")));
                var inline = form.equals("inline") ? """
                    ,{"id":"Observation.extension:answer.value[x]","path":"Observation.extension.value[x]",
                    "type":[{"code":"%s"}]}
                    """.formatted(target) : "";
                repository.create(parser.parseResource("""
                    {"resourceType":"StructureDefinition","id":"answers","url":"%s",
                     "status":"active","kind":"resource","abstract":false,"type":"Observation",
                     "%s":{"element":[
                     {"id":"Observation.value[x]","path":"Observation.value[x]","type":[{"code":"boolean"}]},
                     {"id":"Observation.extension:answer","path":"Observation.extension","sliceName":"answer",
                     "type":[{"code":"Extension","profile":%s}]}%s]}%s}
                    """.formatted(
                                PROFILE,
                                form.equals("parent-both") ? "snapshot" : "differential",
                                version == FhirVersionEnum.DSTU3 ? "\"" + EXTENSION + "\"" : "[\"" + EXTENSION + "\"]",
                                inline,
                                form.equals("parent-both")
                                        ? ",\"differential\":{\"element\":[{\"id\":\"Observation.extension:answer\",\"path\":\"Observation.extension\",\"sliceName\":\"answer\",\"type\":[{\"code\":\"Extension\"}]}]}"
                                        : "")));
                var observation = extract(version, repository, "Observation.extension:answer.value[x]");
                var values = context.newFhirPath().evaluate(observation, "extension.value", IBase.class);
                assertEquals(1, values.size(), target + ":" + form + ":" + parser.encodeResourceToString(observation));
                assertEquals(target, values.get(0).fhirType());
                var expected = parser.parseResource("""
                    {"resourceType":"Observation","extension":[{"url":"%s","value%s":%s}]}
                    """.formatted(
                                EXTENSION, target, target.equals("Coding") ? CODING : "{\"coding\":[" + CODING + "]}"));
                var carrier = context.getResourceDefinition("Observation").newInstance();
                IAdapterFactory.forFhirVersion(version)
                        .createResource(carrier)
                        .setValue("extension", context.newFhirPath().evaluate(observation, "extension", IBase.class));
                assertEquals(parser.encodeResourceToString(expected), parser.encodeResourceToString(carrier));
                assertTrue(context.newFhirPath()
                        .evaluate(observation, "value", IBase.class)
                        .isEmpty());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void itemDefinitionCanonicalDeterminesAnswerType(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        for (var entry : new String[][] {{"answers", PROFILE, "boolean"}, {"other", EXTENSION, "CodeableConcept"}}) {
            repository.create(context.newJsonParser().parseResource("""
                {"resourceType":"StructureDefinition","id":"%s","url":"%s","status":"active",
                 "kind":"resource","abstract":false,"type":"Observation",
                 "differential":{"element":[{"id":"Observation.value[x]","path":"Observation.value[x]",
                 "type":[{"code":"%s"}]}]}}
                """.formatted(entry[0], entry[1], entry[2])));
        }
        var observation = extractItems(
                version,
                repository,
                """
                [{"linkId":"answer","type":"%s","definition":"%s#Observation.value[x]"}]
                """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice", EXTENSION),
                """
                [{"linkId":"answer","answer":[{"valueCoding":%s}]}]
                """.formatted(CODING));
        var values = context.newFhirPath().evaluate(observation, "value", IBase.class);
        assertEquals(1, values.size());
        assertEquals("CodeableConcept", values.get(0).fhirType());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatingComponentsUseNestedType(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
             {"id":"Observation.value[x]","path":"Observation.value[x]","type":[{"code":"boolean"}]},
             {"id":"Observation.component.value[x]","path":"Observation.component.value[x]",
             "type":[{"code":"CodeableConcept"}]}]}}
            """.formatted(PROFILE)));
        var second = CODING.replace("\"no\"", "\"yes\"");
        var observation = extractItems(
                version,
                repository,
                """
                [{"linkId":"answer","type":"%s","repeats":true,"definition":"%s#Observation.component.value[x]"}]
                """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice", PROFILE),
                """
                [{"linkId":"answer","answer":[{"valueCoding":%s},{"valueCoding":%s}]}]
                """.formatted(CODING, second));
        var values = context.newFhirPath().evaluate(observation, "component.value", IBase.class);
        assertEquals(2, values.size(), context.newJsonParser().encodeResourceToString(observation));
        values.forEach(v -> assertEquals("CodeableConcept", v.fhirType()));
        var expected = context.newJsonParser().parseResource("""
            {"resourceType":"Observation","component":[{"valueCodeableConcept":{"coding":[%s]}},
             {"valueCodeableConcept":{"coding":[%s]}}]}
            """.formatted(CODING, second));
        var carrier = context.getResourceDefinition("Observation").newInstance();
        IAdapterFactory.forFhirVersion(version)
                .createResource(carrier)
                .setValue("component", context.newFhirPath().evaluate(observation, "component", IBase.class));
        assertEquals(
                context.newJsonParser().encodeResourceToString(expected),
                context.newJsonParser().encodeResourceToString(carrier));
        assertTrue(context.newFhirPath()
                .evaluate(observation, "value", IBase.class)
                .isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void snapshotSliceDefaultsDoNotLeakBetweenSimilarlyNamedSlices(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "snapshot":{"element":[
             {"id":"Observation.extension:answer","path":"Observation.extension","sliceName":"answer","type":[{"code":"Extension"}]},
             {"id":"Observation.extension:answer.url","path":"Observation.extension.url","fixedUri":"http://example.org/answer"},
             {"id":"Observation.extension:answer.value[x]","path":"Observation.extension.value[x]","type":[{"code":"Coding"}],"defaultValueCoding":{"code":"default"}},
             {"id":"Observation.extension:answerExtra","path":"Observation.extension","sliceName":"answerExtra","type":[{"code":"Extension"}]},
             {"id":"Observation.extension:answerExtra.url","path":"Observation.extension.url","fixedUri":"http://example.org/extra"},
             {"id":"Observation.extension:answerExtra.value[x]","path":"Observation.extension.value[x]","type":[{"code":"CodeableConcept"}]}]}}
            """.formatted(PROFILE)));
        var observation = extractItems(
                version,
                repository,
                """
                [{"linkId":"answer","type":"%s","definition":"%s#Observation.extension:answer.value[x]"},
                 {"linkId":"extra","type":"%s","definition":"%s#Observation.extension:answerExtra.value[x]"}]
                """.formatted(
                                version == FhirVersionEnum.R5 ? "coding" : "choice",
                                PROFILE,
                                version == FhirVersionEnum.R5 ? "coding" : "choice",
                                PROFILE),
                """
                [{"linkId":"answer","answer":[{"valueCoding":%s}]},
                 {"linkId":"extra","answer":[{"valueCoding":%s}]}]
                """.formatted(CODING, CODING));
        var coding = context.newFhirPath()
                .evaluate(observation, "extension.where(url = 'http://example.org/answer').value", IBase.class);
        var concept = context.newFhirPath()
                .evaluate(observation, "extension.where(url = 'http://example.org/extra').value", IBase.class);
        assertEquals(1, coding.size(), context.newJsonParser().encodeResourceToString(observation));
        assertEquals(1, concept.size());
        assertEquals("Coding", coding.get(0).fhirType());
        assertEquals("CodeableConcept", concept.get(0).fhirType());
        var code = context.newFhirPath().evaluate(coding.get(0), "code", IBase.class);
        assertEquals("no", ((org.hl7.fhir.instance.model.api.IPrimitiveType<?>) code.get(0)).getValueAsString());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void namedQuantitySliceAcceptsQuantityInsteadOfFirstSiblingType(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
             {"id":"Observation.value[x]","path":"Observation.value[x]",
              "slicing":{"discriminator":[{"type":"type","path":"$this"}],"rules":"closed"},
              "type":[{"code":"CodeableConcept"},{"code":"Quantity"}]},
             {"id":"Observation.value[x]:cc","path":"Observation.value[x]","sliceName":"cc","type":[{"code":"CodeableConcept"}]},
             {"id":"Observation.value[x]:quantity","path":"Observation.value[x]","sliceName":"quantity","type":[{"code":"Quantity"}]}]}}
            """.formatted(PROFILE)));
        var observation = extractItems(version, repository, """
                [{"linkId":"answer","type":"quantity","definition":"%s#Observation.value[x]:quantity"}]
                """.formatted(PROFILE), """
                [{"linkId":"answer","answer":[{"valueQuantity":{"value":5,"unit":"kg"}}]}]
                """);
        var values = context.newFhirPath().evaluate(observation, "value", IBase.class);
        assertEquals(1, values.size());
        assertEquals("Quantity", values.get(0).fhirType());
        var number = context.newFhirPath().evaluate(values.get(0), "value", IBase.class);
        assertEquals("5", ((org.hl7.fhir.instance.model.api.IPrimitiveType<?>) number.get(0)).getValueAsString());
    }

    private IBaseResource extract(FhirVersionEnum version, InMemoryFhirRepository repository, String path) {
        return extractItems(
                version,
                repository,
                """
                [{"linkId":"answer","type":"%s","definition":"%s#%s"}]
                """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice", PROFILE, path),
                """
                [{"linkId":"answer","answer":[{"valueCoding":%s}]}]
                """.formatted(CODING));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void inlineExtensionAliasesKeepTheirConstraintAndLocation(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        for (var parent : new String[] {"", "component."}) {
            for (var field : new String[] {"value[x]", "valueCodeableConcept"}) {
                var repository = new InMemoryFhirRepository(context);
                repository.create(context.newJsonParser()
                        .parseResource("""
                    {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
                     "kind":"resource","abstract":false,"type":"Observation",
                     "differential":{"element":[
                     {"id":"Observation.%sextension:answer","path":"Observation.%sextension","sliceName":"answer","type":[{"code":"Extension"}]},
                     {"id":"Observation.%sextension:answer.url","path":"Observation.%sextension.url","fixedUri":"http://example.org/answer"},
                     {"id":"Observation.%sextension:answer.value[x]","path":"Observation.%sextension.value[x]","type":[{"code":"CodeableConcept"}]}]}}
                    """.formatted(PROFILE, parent, parent, parent, parent, parent, parent)));
                var observation = extract(version, repository, "Observation." + parent + "extension:answer." + field);
                var extensions = context.newFhirPath().evaluate(observation, parent + "extension", IBase.class);
                assertEquals(1, extensions.size(), context.newJsonParser().encodeResourceToString(observation));
                assertEquals(
                        "http://example.org/answer",
                        ((org.hl7.fhir.instance.model.api.IBaseExtension<?, ?>) extensions.get(0)).getUrl());
                assertEquals(
                        "CodeableConcept",
                        ((org.hl7.fhir.instance.model.api.IBaseExtension<?, ?>) extensions.get(0))
                                .getValue()
                                .fhirType());
                if (!parent.isEmpty()) {
                    assertTrue(context.newFhirPath()
                            .evaluate(observation, "extension", IBase.class)
                            .isEmpty());
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatingNamedChoiceKeepsTheNamedType(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
             {"id":"Observation.component.value[x]","path":"Observation.component.value[x]","type":[{"code":"CodeableConcept"},{"code":"Quantity"}]},
             {"id":"Observation.component.value[x]:quantity","path":"Observation.component.value[x]","sliceName":"quantity","type":[{"code":"Quantity"}]}]}}
            """.formatted(PROFILE)));
        var items = """
            [{"linkId":"answer","type":"quantity","repeats":true,"definition":"%s#Observation.component.value[x]:quantity"}]
            """.formatted(PROFILE);
        var quantity = extractItems(version, repository, items, """
            [{"linkId":"answer","answer":[{"valueQuantity":{"value":5,"unit":"kg"}},{"valueQuantity":{"value":6,"unit":"kg"}}]}]
            """);
        var actual = context.newFhirPath().evaluate(quantity, "component.value", IBase.class);
        assertEquals(2, actual.size());
        actual.forEach(v -> assertEquals("Quantity", v.fhirType()));
        var coding = extractItems(version, repository, items, """
            [{"linkId":"answer","answer":[{"valueCoding":%s}]}]
            """.formatted(CODING));
        assertTrue(context.newFhirPath()
                .evaluate(coding, "component.value", IBase.class)
                .isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void terminalExtensionAndGroupedExtensionPreserveAnswer(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        for (var grouped : new boolean[] {false, true}) {
            for (var inline : new boolean[] {false, true}) {
                var repository = new InMemoryFhirRepository(context);
                var profileReference = inline
                        ? ""
                        : ",\"profile\":"
                                + (version == FhirVersionEnum.DSTU3
                                        ? "\"" + EXTENSION + "\""
                                        : "[\"" + EXTENSION + "\"]");
                var inlineElements = inline ? """
                ,{"id":"Observation.extension:answer.url","path":"Observation.extension.url","fixedUri":"%s"},
                {"id":"Observation.extension:answer.value[x]","path":"Observation.extension.value[x]","type":[{"code":"CodeableConcept"}]}
                """.formatted(EXTENSION) : "";
                repository.create(context.newJsonParser()
                        .parseResource("""
                {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
                 "kind":"resource","abstract":false,"type":"Observation",
                 "differential":{"element":[{"id":"Observation.extension:answer","path":"Observation.extension","sliceName":"answer",
                 "type":[{"code":"Extension"%s}]}%s]}}
                """.formatted(PROFILE, profileReference, inlineElements)));
                repository.create(context.newJsonParser().parseResource("""
                {"resourceType":"StructureDefinition","id":"answer","url":"%s","status":"active",
                 "kind":"complex-type","abstract":false,"type":"Extension",
                 "differential":{"element":[{"id":"Extension.value[x]","path":"Extension.value[x]","type":[{"code":"CodeableConcept"}]}]}}
                """.formatted(EXTENSION)));
                var observation = grouped
                        ? extractItems(
                                version,
                                repository,
                                """
                [{"linkId":"group","type":"group","definition":"%s#Observation.extension:answer","item":[
                 {"linkId":"answer","type":"%s","definition":"%s#Observation.extension:answer.value[x]"}]}]
                """.formatted(PROFILE, version == FhirVersionEnum.R5 ? "coding" : "choice", PROFILE),
                                """
                [{"linkId":"group","item":[{"linkId":"answer","answer":[{"valueCoding":%s}]}]}]
                """.formatted(CODING))
                        : extract(version, repository, "Observation.extension:answer");
                var extensions = context.newFhirPath().evaluate(observation, "extension", IBase.class);
                assertEquals(1, extensions.size(), context.newJsonParser().encodeResourceToString(observation));
                var actual = (org.hl7.fhir.instance.model.api.IBaseExtension<?, ?>) extensions.get(0);
                assertEquals(EXTENSION, actual.getUrl());
                assertNotNull(actual.getValue());
                assertEquals("CodeableConcept", actual.getValue().fhirType());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void missingBaseCanonicalUsesExtractionProfile(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
             {"id":"Observation.extension:answer","path":"Observation.extension","sliceName":"answer","type":[{"code":"Extension"}]},
             {"id":"Observation.extension:answer.url","path":"Observation.extension.url","fixedUri":"http://example.org/answer"},
             {"id":"Observation.extension:answer.value[x]","path":"Observation.extension.value[x]","type":[{"code":"CodeableConcept"}]}]}}
            """.formatted(PROFILE)));
        var observation = extractItems(
                version, repository, """
            [{"linkId":"answer","type":"%s","definition":"http://hl7.org/fhir/StructureDefinition/Observation#Observation.extension:answer.value[x]"}]
            """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice"), """
            [{"linkId":"answer","answer":[{"valueCoding":%s}]}]
            """.formatted(
                                CODING));
        var extensions = context.newFhirPath().evaluate(observation, "extension", IBase.class);
        assertEquals(1, extensions.size());
        var actual = (org.hl7.fhir.instance.model.api.IBaseExtension<?, ?>) extensions.get(0);
        assertEquals("http://example.org/answer", actual.getUrl());
        assertEquals("CodeableConcept", actual.getValue().fhirType());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void typedQuantityAliasDoesNotAllowCodeableConcept(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[{"id":"Observation.value[x]","path":"Observation.value[x]",
             "type":[{"code":"CodeableConcept"},{"code":"Quantity"}]}]}}
            """.formatted(PROFILE)));
        var items = """
            [{"linkId":"answer","type":"quantity","definition":"%s#Observation.valueQuantity"}]
            """.formatted(PROFILE);
        var valid = extractItems(version, repository, items, """
            [{"linkId":"answer","answer":[{"valueQuantity":{"value":5,"unit":"kg"}}]}]
            """);
        var quantity = context.newFhirPath().evaluate(valid, "value", IBase.class);
        assertEquals(1, quantity.size());
        assertEquals("Quantity", quantity.get(0).fhirType());
        var unit = context.newFhirPath().evaluate(quantity.get(0), "unit", IBase.class);
        assertEquals("kg", ((org.hl7.fhir.instance.model.api.IPrimitiveType<?>) unit.get(0)).getValueAsString());
        var invalid = extractItems(version, repository, items, """
            [{"linkId":"answer","answer":[{"valueCoding":%s}]}]
            """.formatted(CODING));
        assertTrue(context.newFhirPath().evaluate(invalid, "value", IBase.class).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void complexExtensionPathIsRejectedInsteadOfAttachedAtRoot(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        repository.create(context.newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"answers","url":"%s","status":"active",
             "kind":"resource","abstract":false,"type":"Observation",
             "differential":{"element":[
             {"id":"Observation.extension:outer","path":"Observation.extension","sliceName":"outer","type":[{"code":"Extension"}]},
             {"id":"Observation.extension:outer.extension:inner","path":"Observation.extension.extension","sliceName":"inner","type":[{"code":"Extension"}]},
             {"id":"Observation.extension:outer.extension:inner.value[x]","path":"Observation.extension.extension.value[x]","type":[{"code":"Coding"}]}]}}
            """.formatted(PROFILE)));
        for (var grouped : new boolean[] {false, true}) {
            var leaf = """
                {"linkId":"answer","type":"%s","definition":"%s#Observation.extension:outer.extension:inner.value[x]"}
                """.formatted(version == FhirVersionEnum.R5 ? "coding" : "choice", PROFILE);
            var items = grouped ? """
                [{"linkId":"group","type":"group","definition":"%s#Observation.extension:outer.extension:inner","item":[%s]}]
                """.formatted(PROFILE, leaf) : "[" + leaf + "]";
            var suppliedAnswer = """
                {"linkId":"answer","answer":[{"valueCoding":%s}]}
                """.formatted(CODING);
            var answers =
                    grouped ? "[{\"linkId\":\"group\",\"item\":[" + suppliedAnswer + "]}]" : "[" + suppliedAnswer + "]";
            var error = assertThrows(
                    ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException.class,
                    () -> extractItems(version, repository, items, answers));
            assertTrue(error.getMessage().contains("multiple nested slices"), error.getMessage());
            for (var emptyAnswer :
                    new String[] {"{\"linkId\":\"answer\"}", "{\"linkId\":\"answer\",\"answer\":[{}]}"}) {
                var empty =
                        grouped ? "[{\"linkId\":\"group\",\"item\":[" + emptyAnswer + "]}]" : "[" + emptyAnswer + "]";
                var observation = extractItems(version, repository, items, empty);
                assertTrue(context.newFhirPath()
                        .evaluate(observation, "extension", IBase.class)
                        .isEmpty());
            }
        }
    }

    private IBaseResource extractItems(
            FhirVersionEnum version, InMemoryFhirRepository repository, String items, String answers) {
        var context = FhirContext.forCached(version);
        var parser = context.newJsonParser();
        var questionnaire = parser.parseResource("""
            {"resourceType":"Questionnaire","id":"q","status":"active",
             "extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract",
             "valueUri":"%s"}],
             "item":%s}
            """.formatted(PROFILE, items));
        var response = parser.parseResource("""
            {"resourceType":"QuestionnaireResponse","id":"qr","status":"completed",
             "subject":{"reference":"Patient/patientId"},
             "item":%s}
            """.formatted(answers));
        var request = newExtractRequestForVersion(
                version, new LibraryEngine(repository, EvaluationSettings.getDefault()), response, questionnaire);
        var resources = context.newFhirPath()
                .evaluate(new ExtractProcessor().extract(request), "entry.resource", IBaseResource.class);
        assertEquals(1, resources.size());
        return resources.get(0);
    }
}
