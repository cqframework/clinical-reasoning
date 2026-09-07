package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.GeneratedIds;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class GeneratedResourceIdsTest {
    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void qualifiedVersionedQuestionnaireDoesNotQualifyGeneratedResponse(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var engine = new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault());
        var questionnaire = context.getResourceDefinition("Questionnaire")
                .newInstance()
                .setId("http://example.org/fhir/Questionnaire/q/_history/2");
        var oldComposite = context.getResourceDefinition("QuestionnaireResponse")
                .newInstance()
                .setId(questionnaire.getIdElement().getValue() + "-patientId")
                .getIdElement();
        assertEquals("q", oldComposite.getIdPart());
        assertEquals("Questionnaire", oldComposite.getResourceType());
        assertEquals("2-patientId", oldComposite.getVersionIdPart());
        var response = newPopulateRequestForVersion(version, engine, questionnaire)
                .getQuestionnaireResponseAdapter()
                .get();
        assertEquals("q-patientId", response.getIdElement().getValue());
        assertEquals("q-patientId", response.getIdElement().getIdPart());
        assertNull(response.getIdElement().getResourceType());
        assertNull(response.getIdElement().getVersionIdPart());
        assertEquals(
                "http://example.org/fhir/Questionnaire/q/_history/2",
                questionnaire.getIdElement().getValue());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void populatedResponseUsesLogicalQuestionnaireId(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var engine = new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault());
        for (var id : List.of("q", "Q".repeat(64))) {
            var questionnaire =
                    context.getResourceDefinition("Questionnaire").newInstance().setId("Questionnaire/" + id);
            var request = newPopulateRequestForVersion(version, engine, questionnaire);
            var actual = request.getQuestionnaireResponseAdapter()
                    .get()
                    .getIdElement()
                    .getIdPart();
            assertEquals(GeneratedIds.fromComposite(id + "-patientId"), actual);
            if (id.equals("q")) {
                assertEquals("q-patientId", actual);
            }
            assertEquals(id, questionnaire.getIdElement().getIdPart());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void definitionResourceAndContainedOutcomeUseFinalIds(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var factory = IAdapterFactory.forFhirVersion(version);
        var parser = context.newJsonParser();
        var id = "q".repeat(64);
        var qr = parser.parseResource("""
            {"resourceType":"QuestionnaireResponse","id":"%s","status":"completed",
             "subject":{"reference":"Patient/patientId"}}
            """.formatted(id));
        var engine = new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault());
        var request = newExtractRequestForVersion(version, engine, qr, null);
        var resource = context.getResourceDefinition("Observation").newInstance();
        new ProcessDefinitionItem()
                .processResource(request, resource, Optional.empty(), true, new ItemPair(null, null));
        assertEquals(
                GeneratedIds.fromComposite("extract-" + id),
                resource.getIdElement().getIdPart());
        assertEquals(id, qr.getIdElement().getIdPart());
        assertEquals("extract-" + id, request.getExtractId());
        var bundle = new ExtractProcessor().createBundle(request, List.of(resource));
        assertEquals(
                GeneratedIds.fromComposite("extract-" + id),
                bundle.getIdElement().getIdPart());
        request.logException("Intentional diagnostic for generated-ID reference regression");
        request.resolveOperationOutcome(qr);
        var outcome = request.getOperationOutcome();
        var outcomeId = outcome.getIdElement().getIdPart();
        assertEquals(GeneratedIds.fromComposite("extract-outcome-" + id), outcomeId);
        var serialized = parser.parseResource(parser.encodeResourceToString(qr));
        var references = context.newFhirPath().evaluate(serialized, "extension.value", IBaseReference.class);
        assertEquals(1, references.size());
        assertEquals("#" + outcomeId, references.get(0).getReferenceElement().getValue());
        var contained = context.newFhirPath().evaluate(serialized, "contained", IBaseResource.class);
        assertEquals(1, contained.size());
        assertEquals(outcomeId, contained.get(0).getIdElement().getIdPart());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"R4", "R5"})
    void observationIdsPreserveCallerIdentityAndProvenance(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var parser = context.newJsonParser();
        var id = "Caller.QR-" + "A".repeat(54);
        var link = "Link / æ¼¢å­— " + "z".repeat(90);
        var qr = parser.parseResource("""
            {"resourceType":"QuestionnaireResponse","id":"%s","questionnaire":"http://example.org/Q|V1",
             "status":"completed","subject":{"reference":"Patient/patientId"},"authored":"2026-01-04T00:00:00Z",
             "item":[{"linkId":"%s","answer":[{"valueBoolean":false}]}]}
            """.formatted(id, link));
        var engine = new LibraryEngine(new InMemoryFhirRepository(context), EvaluationSettings.getDefault());
        var request = newExtractRequestForVersion(version, engine, qr, null);
        var answer = request.getQuestionnaireResponseAdapter()
                .getItem()
                .get(0)
                .getAnswer()
                .get(0);
        var subject = context.newFhirPath()
                .evaluate(qr, "subject", IBaseReference.class)
                .get(0);
        IBaseCoding coding = version == FhirVersionEnum.R4
                ? new org.hl7.fhir.r4.model.Coding("http://example.org/code", "answer", null)
                : new org.hl7.fhir.r5.model.Coding("http://example.org/code", "answer", null);
        var codes = Map.of(link, List.of(coding));
        var before = parser.encodeResourceToString(qr);
        var observation = version == FhirVersionEnum.R4
                ? new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r4.ObservationResolver()
                        .resolve(request, answer, null, link, subject, codes, null)
                : new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r5.ObservationResolver()
                        .resolve(request, answer, null, link, subject, codes, null);
        assertEquals(before, parser.encodeResourceToString(qr));
        assertEquals(
                GeneratedIds.fromComposite("extract-" + id + "." + link),
                observation.getIdElement().getIdPart());
        var serialized = parser.parseResource(parser.encodeResourceToString(observation));
        var derived = context.newFhirPath().evaluate(serialized, "derivedFrom", IBaseReference.class);
        assertEquals(
                "QuestionnaireResponse/" + id,
                derived.get(0).getReferenceElement().getValue());
        assertEquals(
                "Patient/patientId",
                context.newFhirPath()
                        .evaluate(serialized, "subject", IBaseReference.class)
                        .get(0)
                        .getReferenceElement()
                        .getValue());
        assertTrue(parser.encodeResourceToString(observation).contains(link));
    }
}
