package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ComplexNamedSliceExtractionTest {
    private static final String PROFILE = "http://example.org/StructureDefinition/complex-answers";
    private static final String OUTER = "http://example.org/StructureDefinition/outer";
    private static final String INNER = "http://example.org/StructureDefinition/inner";
    private static final String PATH = "Observation.extension:outer.extension:inner";
    private static final String CODING = """
        {"system":"http://example.org/codes","version":"v1","code":"yes","display":"Yes","userSelected":true,
         "extension":[{"url":"http://example.org/note","valueString":"retained"}]}
        """;

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void flatAndGroupedPathsBuildTheSameTree(FhirVersionEnum version) {
        for (var referenced : new boolean[] {false, true}) {
            for (var snapshot : new boolean[] {false, true}) {
                var repo = repository(version, referenced, snapshot);
                var leaf = question(version, "answer", PATH + ".value[x]");
                var response = answer("answer", CODING);
                for (int depth = 0; depth < 3; depth++) {
                    var q = leaf;
                    var qr = response;
                    if (depth > 0) {
                        q = group("inner", PATH, q, false);
                        qr = responseGroup("inner", qr);
                    }
                    if (depth > 1) {
                        q = group("outer", "Observation.extension:outer", q, false);
                        qr = responseGroup("outer", qr);
                    }
                    var result = extract(version, repo, q, qr);
                    assertTree(version, result, 1, 1);
                    assertCoding(version, result, "extension.extension.value.coding", CODING);
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedGroupsAndAnswersStaySeparate(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        for (var defined : new boolean[] {false, true}) {
            var q = group(
                    "outer",
                    defined ? "Observation.extension:outer" : null,
                    question(version, "answer", PATH + ".value[x]"),
                    true);
            var qr = responseGroup("outer", answer("answer", CODING)) + ","
                    + responseGroup("outer", answer("answer", CODING.replace("\"yes\"", "\"no\"")));
            var result = extract(version, repo, q, qr);
            assertTree(version, result, 2, 2);
            assertEquals(
                    List.of("yes", "no"),
                    values(version, result, "extension.extension.value.coding.code").stream()
                            .map(value ->
                                    ((org.hl7.fhir.instance.model.api.IPrimitiveType<?>) value).getValueAsString())
                            .toList());
        }
        var q = question(version, "answer", PATH + ".value[x]").replace("\"linkId\"", "\"repeats\":true,\"linkId\"");
        var qr = "{\"linkId\":\"answer\",\"answer\":[{\"valueCoding\":" + CODING + "},{\"valueCoding\":"
                + CODING.replace("\"yes\"", "\"no\"") + "}]}";
        assertTree(version, extract(version, repo, q, qr), 1, 2);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void siblingSlicesDoNotMergeAndDefaultsDoNotOverwriteAnswers(FhirVersionEnum version) {
        var repo = repository(version, false, true);
        var paths = new String[] {
            PATH + ".value[x]",
            "Observation.extension:outer.extension:other.value[x]",
            "Observation.extension:outerExtra.extension:inner.value[x]"
        };
        for (var reverse : new boolean[] {false, true}) {
            var questions = new java.util.ArrayList<String>();
            var answers = new java.util.ArrayList<String>();
            for (int n = 0; n < paths.length; n++) {
                int i = reverse ? paths.length - 1 - n : n;
                questions.add(question(version, "a" + i, paths[i]));
                answers.add(answer("a" + i, CODING.replace("\"yes\"", "\"answer" + i + "\"")));
            }
            var result = extract(version, repo, String.join(",", questions), String.join(",", answers));
            assertEquals(2, values(version, result, "extension").size());
            assertEquals(3, values(version, result, "extension.extension").size());
            assertCoding(
                    version,
                    result,
                    "extension.where(url='outer').extension.where(url='inner').value.coding",
                    CODING.replace("\"yes\"", "\"answer0\""));
            assertCoding(
                    version,
                    result,
                    "extension.where(url='outer').extension.where(url='other').value.coding",
                    CODING.replace("\"yes\"", "\"answer1\""));
            assertCoding(
                    version,
                    result,
                    "extension.where(url='outerExtra').extension.where(url='inner').value.coding",
                    CODING.replace("\"yes\"", "\"answer2\""));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void absentAnswersDoNotConstructContainersAndContextDoesNotLeak(FhirVersionEnum version) {
        var repo = repository(version, true, false);
        var q = group("outer", "Observation.extension:outer", question(version, "answer", PATH + ".value[x]"), false);
        assertTree(version, extract(version, repo, q, responseGroup("outer", answer("answer", CODING))), 1, 1);
        for (var empty : new String[] {"{\"linkId\":\"answer\"}", "{\"linkId\":\"answer\",\"answer\":[{}]}"}) {
            assertTrue(values(version, extract(version, repo, q, responseGroup("outer", empty)), "extension")
                    .isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void missingIntermediateProfileReportsAnsweredPathOnly(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        createProfile(
                version,
                repo,
                PROFILE,
                "Observation",
                "differential",
                slice(version, "Observation.extension:outer", OUTER),
                "");
        var q = question(version, "answer", PATH + ".value[x]");
        var error = assertThrows(
                UnprocessableEntityException.class, () -> extract(version, repo, q, answer("answer", CODING)));
        assertTrue(error.getMessage().contains("extension:outer.extension:inner"), error.getMessage());
        assertTrue(
                error.getMessage().startsWith("Unable to resolve Extension URL for extraction path "),
                error.getMessage());
        assertTrue(values(version, extract(version, repo, q, "{\"linkId\":\"answer\"}"), "extension")
                .isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void namedComponentAncestorsWorkFlatGroupedAndRepeated(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        var component = "Observation.component:panel";
        var outer = component + ".extension:outer";
        var inner = outer + ".extension:inner";
        createProfile(
                version,
                repo,
                PROFILE,
                "Observation",
                "differential",
                """
            {"id":"Observation.component:panel","path":"Observation.component","sliceName":"panel","type":[{"code":"BackboneElement"}]},
            {"id":"Observation.component:panel.code","path":"Observation.component.code","fixedCodeableConcept":{"coding":[{"code":"panel"}]}},
            """ + slice(version, outer, null) + "," + fixed(outer + ".url", "outer") + ","
                        + slice(version, inner, null) + "," + fixed(inner + ".url", "inner") + ","
                        + value(inner + ".value[x]"),
                "");
        for (int mode = 0; mode < 3; mode++) {
            var q = question(version, "answer", inner + ".value[x]");
            var qr = answer("answer", CODING);
            if (mode > 0) {
                q = group("panel", component, q, mode == 2);
                qr = responseGroup("panel", qr);
                if (mode == 2) {
                    qr += "," + qr;
                }
            }
            var result = extract(version, repo, q, qr);
            assertEquals(mode == 2 ? 2 : 1, values(version, result, "component").size());
            assertEquals(
                    mode == 2 ? 2 : 1,
                    values(version, result, "component.extension.extension.value.coding")
                            .size());
            assertEquals(
                    mode == 2 ? 2 : 1,
                    values(version, result, "component.code.coding.where(code='panel')")
                            .size());
            assertTrue(values(version, result, "extension").isEmpty());
            assertTrue(values(version, result, "component.extension.value").isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void revisitingAContainerPreservesAnswersAndDefaultDatatypes(FhirVersionEnum version) {
        var repo = repository(version, false, true);
        var qValue = question(version, "answer", PATH + ".value[x]");
        var qId = question(version, "id", PATH + ".id")
                .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"string\"");
        var qrId = "{\"linkId\":\"id\",\"answer\":[{\"valueString\":\"inner-id\"}]}";
        for (var reverse : new boolean[] {false, true}) {
            var q = reverse ? qId + "," + qValue : qValue + "," + qId;
            var qr = reverse ? qrId + "," + answer("answer", CODING) : answer("answer", CODING) + "," + qrId;
            var result = extract(version, repo, q, qr);
            assertTree(version, result, 1, 1);
            assertCoding(version, result, "extension.extension.value.coding", CODING);
            assertEquals(
                    1,
                    values(version, result, "extension.extension.where(id='inner-id')")
                            .size());
        }
        var qText = question(version, "text", PATH + ".valueCodeableConcept.text")
                .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"string\"");
        var withText =
                extract(version, repo, qText, "{\"linkId\":\"text\",\"answer\":[{\"valueString\":\"Authored text\"}]}");
        assertEquals(
                1,
                values(version, withText, "extension.extension.value.coding.where(code='default')")
                        .size());
        assertEquals(
                1,
                values(version, withText, "extension.extension.value.where(text='Authored text')")
                        .size());
        var later = extract(version, repo, qId, qrId);
        assertTrue(
                values(version, later, "extension.extension.value.text").isEmpty(),
                "Profile default must remain unmodified across extraction calls");
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedCodingAnswersAppendWithinOneCodeableConcept(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        var outer = "Observation.extension:outer";
        var valueWithoutDefault = "{\"id\":\"" + PATH
                + ".value[x]\",\"path\":\"Observation.extension.extension.value[x]\",\"type\":[{\"code\":\"CodeableConcept\"}]}";
        createProfile(
                version,
                repo,
                PROFILE,
                "Observation",
                "differential",
                slice(version, outer, null) + "," + fixed(outer + ".url", "outer") + "," + slice(version, PATH, null)
                        + "," + fixed(PATH + ".url", "inner") + "," + valueWithoutDefault,
                "");
        var q = question(version, "answer", PATH + ".valueCodeableConcept.coding");
        var qr = "{\"linkId\":\"answer\",\"answer\":[{\"valueCoding\":" + CODING + "},{\"valueCoding\":"
                + CODING.replace("\"yes\"", "\"no\"") + "}]}";
        var result = extract(version, repo, q, qr);
        assertTree(version, result, 1, 1);
        assertEquals(
                2, values(version, result, "extension.extension.value.coding").size());
        assertCoding(version, result, "extension.extension.value.coding.where(code='yes')", CODING);
        assertCoding(
                version,
                result,
                "extension.extension.value.coding.where(code='no')",
                CODING.replace("\"yes\"", "\"no\""));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedResponseOccurrencesDoNotRequireQuestionnaireRepeatFlags(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        for (var defined : new boolean[] {false, true}) {
            var q = group(
                    "outer",
                    defined ? "Observation.extension:outer" : null,
                    question(version, "answer", PATH + ".value[x]"),
                    false);
            for (var includeQuestionnaire : new boolean[] {false, true}) {
                var answer1 = answer("answer", CODING)
                        .replace(
                                "\"linkId\":\"answer\"",
                                "\"linkId\":\"answer\",\"definition\":\"" + PROFILE + "#" + PATH + ".value[x]\"");
                var qr = responseGroup("outer", answer1) + ","
                        + responseGroup("outer", answer1.replace("\"yes\"", "\"no\""));
                if (defined) {
                    qr = qr.replace(
                            "\"linkId\":\"outer\"",
                            "\"linkId\":\"outer\",\"definition\":\"" + PROFILE + "#Observation.extension:outer\"");
                }
                assertTree(version, extract(version, repo, q, qr, includeQuestionnaire), 2, 2);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void urlAnswersAreAppliedBeforeUrlValidation(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        createProfile(
                version,
                repo,
                PROFILE,
                "Observation",
                "differential",
                slice(version, "Observation.extension:outer", null)
                        + "," + fixed("Observation.extension:outer.url", "outer") + "," + slice(version, PATH, null)
                        + "," + value(PATH + ".value[x]"),
                "");
        var urlQuestion = question(version, "url", PATH + ".url")
                .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"url\"");
        var q = group("inner", PATH, urlQuestion + "," + question(version, "answer", PATH + ".value[x]"), false);
        var qr = responseGroup(
                "inner", "{\"linkId\":\"url\",\"answer\":[{\"valueUri\":\"inner\"}]}," + answer("answer", CODING));
        for (var repeat : new boolean[] {false, true}) {
            var response = repeat ? qr + "," + qr.replace("\"yes\"", "\"no\"") : qr;
            var result = extract(version, repo, q, response);
            assertTree(version, result, 1, repeat ? 2 : 1);
            assertCoding(version, result, "extension.extension.value.coding.where(code='yes')", CODING);
            if (repeat) {
                assertCoding(
                        version,
                        result,
                        "extension.extension.value.coding.where(code='no')",
                        CODING.replace("\"yes\"", "\"no\""));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void absoluteChildPathsReturnToResourceRoot(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        var elsewhere = "Observation.extension:outerExtra.extension:inner.value[x]";
        for (var ownAnswer : new boolean[] {false, true}) {
            var qChildren = question(version, "other", elsewhere)
                    + ","
                    + question(version, "status", "Observation.status")
                            .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"string\"");
            var qrChildren =
                    answer("other", CODING) + ",{\"linkId\":\"status\",\"answer\":[{\"valueString\":\"final\"}]}";
            if (ownAnswer) {
                qChildren += "," + question(version, "local", PATH + ".value[x]");
                qrChildren += "," + answer("local", CODING);
            }
            var result = extract(
                    version,
                    repo,
                    group("outer", "Observation.extension:outer", qChildren, false),
                    responseGroup("outer", qrChildren));
            assertEquals(ownAnswer ? 2 : 1, values(version, result, "extension").size());
            assertCoding(version, result, "extension.where(url='outerExtra').extension.value.coding", CODING);
            assertEquals(
                    1, values(version, result, "status.where($this = 'final')").size());
            assertTrue(values(version, result, "extension.extension.extension").isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void nestedDefinitionlessRepeatsRetainTheirExistingParent(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        var qId = question(version, "id", "Observation.extension:outer.id")
                .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"string\"");
        var q = group(
                "outer",
                "Observation.extension:outer",
                qId + "," + group("repeated", null, question(version, "answer", PATH + ".value[x]"), false),
                false);
        var qr = responseGroup(
                "outer",
                "{\"linkId\":\"id\",\"answer\":[{\"valueString\":\"outer-id\"}]},"
                        + responseGroup("repeated", answer("answer", CODING)) + ","
                        + responseGroup("repeated", answer("answer", CODING)));
        var result = extract(version, repo, q, qr);
        assertTree(version, result, 1, 2);
        assertEquals(
                1, values(version, result, "extension.where(id='outer-id')").size());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void inlineUrlOverridesReferencedProfileDefault(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        createProfile(
                version,
                repo,
                INNER,
                "Extension",
                "snapshot",
                fixed("Extension.url", "inner") + "," + value("Extension.value[x]"),
                "");
        createProfile(
                version,
                repo,
                OUTER,
                "Extension",
                "snapshot",
                fixed("Extension.url", "outer") + "," + slice(version, "Extension.extension:inner", INNER),
                "");
        createProfile(
                version,
                repo,
                PROFILE,
                "Observation",
                "differential",
                slice(version, "Observation.extension:outer", OUTER) + "," + fixed(PATH + ".url", "inline-inner"),
                "");
        var result = extract(version, repo, question(version, "answer", PATH + ".value[x]"), answer("answer", CODING));
        assertEquals(
                1,
                values(version, result, "extension.where(url='outer').extension.where(url='inline-inner')")
                        .size());
        assertCoding(version, result, "extension.extension.value.coding", CODING);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedLayoutGroupsKeepSeparateAnswers(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        var target = "Observation.extension:outerExtra.extension:inner.value[x]";
        for (var local : new boolean[] {false, true}) {
            var qChildren = question(version, "answer", target)
                    + (local ? "," + stringQuestion(version, "id", "Observation.extension:outer.id") : "");
            var qrChildren = answer("answer", CODING) + (local ? "," + stringAnswer("id", "local") : "");
            var q = group("layout", "Observation.extension:outer", qChildren, false);
            var qr = responseGroup("layout", qrChildren) + ","
                    + responseGroup("layout", qrChildren.replace("\"yes\"", "\"no\""));
            var result = extract(version, repo, q, qr);
            assertEquals(local ? 4 : 2, values(version, result, "extension").size());
            assertEquals(2, values(version, result, "extension.extension").size());
            assertEquals(
                    2,
                    values(version, result, "extension.where(url='outerExtra')").size());
            assertCoding(version, result, "extension.extension.value.coding.where(code='yes')", CODING);
            assertCoding(
                    version,
                    result,
                    "extension.extension.value.coding.where(code='no')",
                    CODING.replace("\"yes\"", "\"no\""));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedSingleValuedGroupsMergeTheirFields(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        var q = group(
                "code",
                "Observation.code",
                stringQuestion(version, "text", "Observation.code.text") + ","
                        + question(version, "coding", "Observation.code.coding"),
                false);
        var qr = responseGroup("code", stringAnswer("text", "Authored text")) + ","
                + responseGroup("code", answer("coding", CODING));
        var result = extract(version, repo, q, qr);
        assertEquals(
                1,
                values(version, result, "code.where(text='Authored text')").size(),
                () -> FhirContext.forCached(version).newJsonParser().encodeResourceToString(result));
        assertCoding(version, result, "code.coding", CODING);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void repeatedAbsolutePathsRetainTheirExistingAncestor(FhirVersionEnum version) {
        var repo = repository(version, false, false);
        var target = "Observation.extension:outer.extension:other.value[x]";
        var q = group(
                "inner",
                PATH,
                stringQuestion(version, "id", PATH + ".id") + ","
                        + group("layout", null, question(version, "answer", target), false),
                false);
        var qr = responseGroup(
                "inner",
                stringAnswer("id", "existing-inner") + ","
                        + responseGroup("layout", answer("answer", CODING)) + ","
                        + responseGroup("layout", answer("answer", CODING.replace("\"yes\"", "\"no\""))));
        var result = extract(version, repo, q, qr);
        assertEquals(1, values(version, result, "extension").size());
        assertEquals(3, values(version, result, "extension.extension").size());
        assertEquals(
                1,
                values(version, result, "extension.extension.where(id='existing-inner')")
                        .size());
        assertEquals(
                2,
                values(version, result, "extension.extension.where(url='other')")
                        .size());
        assertCoding(version, result, "extension.extension.where(url='other').value.coding.where(code='yes')", CODING);
        assertCoding(
                version,
                result,
                "extension.extension.where(url='other').value.coding.where(code='no')",
                CODING.replace("\"yes\"", "\"no\""));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void componentLeafDefaultsAreNotMutated(FhirVersionEnum version) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        createProfile(version, repo, PROFILE, "Observation", "snapshot", """
                {"id":"Observation.component:panel","path":"Observation.component","type":[{"code":"BackboneElement"}]},
                {"id":"Observation.component:panel.code","path":"Observation.component.code","fixedCodeableConcept":{"coding":[{"code":"panel"}]}}
                """, "");
        var q = stringQuestion(version, "text", "Observation.component:panel.code.text");
        var result = extract(version, repo, q, stringAnswer("text", "Authored text"));
        assertEquals(
                1,
                values(version, result, "component.code.where(text='Authored text').coding.where(code='panel')")
                        .size());
        var later = extract(
                version,
                repo,
                stringQuestion(version, "id", "Observation.component:panel.id"),
                stringAnswer("id", "next"));
        assertTrue(values(version, later, "component.code.text").isEmpty());
        assertEquals(
                1,
                values(version, later, "component.code.coding.where(code='panel')")
                        .size());
    }

    private String stringQuestion(FhirVersionEnum version, String linkId, String path) {
        return question(version, linkId, path)
                .replace(version == FhirVersionEnum.R5 ? "\"coding\"" : "\"choice\"", "\"string\"");
    }

    private String stringAnswer(String linkId, String value) {
        return "{\"linkId\":\"" + linkId + "\",\"answer\":[{\"valueString\":\"" + value + "\"}]}";
    }

    private InMemoryFhirRepository repository(FhirVersionEnum version, boolean referenced, boolean snapshot) {
        var repo = new InMemoryFhirRepository(FhirContext.forCached(version));
        var section = snapshot ? "snapshot" : "differential";
        if (referenced) {
            createProfile(
                    version,
                    repo,
                    INNER,
                    "Extension",
                    section,
                    fixed("Extension.url", "inner") + "," + value("Extension.value[x]"),
                    "");
            createProfile(
                    version,
                    repo,
                    OUTER,
                    "Extension",
                    section,
                    fixed("Extension.url", "outer") + "," + slice(version, "Extension.extension:inner", INNER),
                    "");
            createProfile(
                    version,
                    repo,
                    PROFILE,
                    "Observation",
                    section,
                    slice(version, "Observation.extension:outer", OUTER),
                    "");
        } else {
            var elements = new java.util.ArrayList<String>();
            for (var outer : new String[] {"outer", "outerExtra"}) {
                var prefix = "Observation.extension:" + outer;
                elements.add(slice(version, prefix, null));
                elements.add(fixed(prefix + ".url", outer));
                for (var inner : new String[] {"inner", "other"}) {
                    var child = prefix + ".extension:" + inner;
                    elements.add(slice(version, child, null));
                    elements.add(fixed(child + ".url", inner));
                    elements.add(value(child + ".value[x]"));
                }
            }
            createProfile(version, repo, PROFILE, "Observation", section, String.join(",", elements), "");
        }
        return repo;
    }

    private String slice(FhirVersionEnum version, String id, String canonical) {
        return "{\"id\":\"" + id + "\",\"path\":\"" + id.replaceAll(":[^.]+", "")
                + "\",\"sliceName\":\"" + id.substring(id.lastIndexOf(':') + 1) + "\",\"type\":[{\"code\":\"Extension\""
                + (canonical == null
                        ? ""
                        : ",\"profile\":"
                                + (version == FhirVersionEnum.DSTU3
                                        ? "\"" + canonical + "\""
                                        : "[\"" + canonical + "\"]"))
                + "}]}";
    }

    private String fixed(String id, String url) {
        return "{\"id\":\"" + id + "\",\"path\":\"" + id.replaceAll(":[^.]+", "") + "\",\"fixedUri\":\"" + url + "\"}";
    }

    private String value(String id) {
        return "{\"id\":\"" + id + "\",\"path\":\"" + id.replaceAll(":[^.]+", "")
                + "\",\"type\":[{\"code\":\"CodeableConcept\"}],\"defaultValueCodeableConcept\":{\"coding\":[{\"code\":\"default\"}]}}";
    }

    private void createProfile(
            FhirVersionEnum version,
            InMemoryFhirRepository repo,
            String url,
            String type,
            String section,
            String elements,
            String extra) {
        repo.create(FhirContext.forCached(version).newJsonParser().parseResource("""
            {"resourceType":"StructureDefinition","id":"%s","url":"%s","status":"active","kind":"%s","abstract":false,
            "type":"%s","%s":{"element":[%s]}%s}
            """.formatted(
                        url.substring(url.lastIndexOf('/') + 1),
                        url,
                        type.equals("Observation") ? "resource" : "complex-type",
                        type,
                        section,
                        elements,
                        extra)));
    }

    private String question(FhirVersionEnum version, String id, String path) {
        return """
            {"linkId":"%s","type":"%s","definition":"%s#%s"}
            """.formatted(id, version == FhirVersionEnum.R5 ? "coding" : "choice", PROFILE, path);
    }

    private String group(String id, String path, String children, boolean repeats) {
        return "{\"linkId\":\"" + id + "\",\"type\":\"group\",\"repeats\":" + repeats
                + (path == null ? "" : ",\"definition\":\"" + PROFILE + "#" + path + "\"") + ",\"item\":[" + children
                + "]}";
    }

    private String answer(String id, String coding) {
        return "{\"linkId\":\"" + id + "\",\"answer\":[{\"valueCoding\":" + coding + "}]}";
    }

    private String responseGroup(String id, String children) {
        return "{\"linkId\":\"" + id + "\",\"item\":[" + children + "]}";
    }

    private List<IBase> values(FhirVersionEnum version, IBaseResource result, String path) {
        return FhirContext.forCached(version).newFhirPath().evaluate(result, path, IBase.class);
    }

    private void assertTree(FhirVersionEnum version, IBaseResource result, int outers, int inners) {
        assertEquals(outers, values(version, result, "extension").size());
        assertEquals(
                outers, values(version, result, "extension.where(url='outer')").size());
        assertEquals(
                inners,
                values(version, result, "extension.extension.where(url='inner')")
                        .size());
        assertTrue(values(version, result, "extension.value").isEmpty());
        assertTrue(values(version, result, "value").isEmpty());
    }

    private void assertCoding(FhirVersionEnum version, IBaseResource result, String path, String coding) {
        var context = FhirContext.forCached(version);
        var actual = context.getResourceDefinition("Observation").newInstance();
        var actualValues = values(version, result, path);
        assertEquals(1, actualValues.size());
        var cc = org.opencds.cqf.fhir.utility.Resources.newBaseForVersion("CodeableConcept", version);
        IAdapterFactory.forFhirVersion(version).createBase(cc).setValue("coding", actualValues);
        IAdapterFactory.forFhirVersion(version).createResource(actual).setValue("code", cc);
        var expected = context.newJsonParser()
                .parseResource("{\"resourceType\":\"Observation\",\"code\":{\"coding\":[" + coding + "]}}");
        assertEquals(
                context.newJsonParser().encodeResourceToString(expected),
                context.newJsonParser().encodeResourceToString(actual));
    }

    private IBaseResource extract(FhirVersionEnum version, InMemoryFhirRepository repo, String qItems, String qrItems) {
        return extract(version, repo, qItems, qrItems, true);
    }

    private IBaseResource extract(
            FhirVersionEnum version,
            InMemoryFhirRepository repo,
            String qItems,
            String qrItems,
            boolean includeQuestionnaire) {
        var context = FhirContext.forCached(version);
        var q = context.newJsonParser().parseResource("""
            {"resourceType":"Questionnaire","id":"q","status":"active",
             "extension":[{"url":"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract","valueUri":"%s"}],
             "item":[%s]}
            """.formatted(PROFILE, qItems));
        var qr = context.newJsonParser()
                .parseResource(
                        "{\"resourceType\":\"QuestionnaireResponse\",\"id\":\"qr\",\"status\":\"completed\",\"extension\":[{\"url\":\"http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract\",\"valueUri\":\""
                                + PROFILE + "\"}],\"item\":[" + qrItems + "]}");
        var request = newExtractRequestForVersion(
                version, new LibraryEngine(repo, EvaluationSettings.getDefault()), qr, includeQuestionnaire ? q : null);
        var resources = context.newFhirPath()
                .evaluate(new ExtractProcessor().extract(request), "entry.resource", IBaseResource.class);
        assertEquals(1, resources.size());
        return resources.get(0);
    }
}
