package org.opencds.cqf.fhir.cr.plandefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionPropagationContext;
import org.opencds.cqf.fhir.cr.common.ExtensionPropagationPolicy;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
class ExtensionPropagationTests {
    private static final String OWNED_BY = "urn:example:owned-by";
    private static final String PLAN_DEFAULT = "urn:example:plan-definition/plan-default";
    private static final String ALLOWED = "urn:test:propagated";

    static Stream<Arguments> policies() {
        return Stream.of(FhirVersionEnum.DSTU3, FhirVersionEnum.R4, FhirVersionEnum.R5)
                .flatMap(version -> Stream.of(
                        Arguments.of(version, CrSettings.getDefault(), List.of(OWNED_BY, ALLOWED)),
                        Arguments.of(version, settings(ExtensionPropagationPolicy.none()), List.of()),
                        Arguments.of(
                                version,
                                settings(ExtensionPropagationPolicy.excludeUrls(Set.of(OWNED_BY, PLAN_DEFAULT))),
                                List.of(ALLOWED)),
                        Arguments.of(
                                version,
                                settings(ExtensionPropagationPolicy.allowOnlyUrls(
                                        Set.of(ALLOWED, Constants.CPG_KNOWLEDGE_CAPABILITY))),
                                List.of(ALLOWED))));
    }

    @ParameterizedTest
    @MethodSource("policies")
    void appliesPolicyToPlanActionsGoalTargetsAndActivities(
            FhirVersionEnum version, CrSettings settings, List<String> expected) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        var plan = plan(context);
        var factory = IAdapterFactory.forFhirContext(context);
        var activity = (IBaseResource)
                factory.createResource(plan).resolvePathList("contained").get(0);
        var original = context.newJsonParser().encodeResourceToString(plan);

        var resources = applyPlan(repository, settings, plan);
        var orchestration = resources.stream()
                .filter(r -> r.fhirType().equals(orchestrationType(version)))
                .findFirst()
                .orElseThrow();
        var adapter = factory.createResource(orchestration);
        var expectedRoot = new ArrayList<>(expected);
        if (expected.contains(OWNED_BY)) {
            expectedRoot.add(PLAN_DEFAULT);
        }
        expectedRoot.add(Constants.PERTAINS_TO_GOAL);
        assertEquals(expectedRoot, urls(context, orchestration));
        var action = adapter.resolvePathList("action").get(0);
        assertEquals(expected, urls(context, action));
        assertEquals(
                expected,
                urls(
                        context,
                        factory.createBase(action).resolvePathList("action").get(0)));

        var goal = resources.stream()
                .filter(r -> r.fhirType().equals("Goal"))
                .findFirst()
                .orElseThrow();
        var goalAdapter = factory.createResource(goal);
        // Goal.target is singular in DSTU3 and repeating in R4/R5.
        var goalTarget = version == FhirVersionEnum.DSTU3
                ? (IBase) goalAdapter.resolvePath("target")
                : goalAdapter.resolvePathList("target").get(0);
        assertEquals(expected, urls(context, goalTarget));

        var generated = resources.stream()
                .filter(r -> r.fhirType().equals(activityType(version)))
                .findFirst()
                .orElseThrow();
        assertEquals(expected, urls(context, generated));

        var direct = new ActivityDefinitionProcessor(repository, settings)
                .apply(Eithers.forRight3(activity), "patient", null, null, null, null, null, null, null, null);
        assertEquals(expected, urls(context, direct));
        assertEquals(original, context.newJsonParser().encodeResourceToString(plan));
        assertFalse(resources.stream()
                .anyMatch(r -> !factory.createResource(r).getContained().isEmpty()));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void suppliesVersionAndElementPathsToCustomPolicy(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var seen = new ArrayList<ExtensionPropagationContext>();
        var settings = settings(copy -> {
            seen.add(copy);
            return copy.targetPath().equals(orchestrationType(version) + ".action");
        });
        var resources = applyPlan(new InMemoryFhirRepository(context), settings, plan(context));
        var orchestration = resources.stream()
                .filter(r -> r.fhirType().equals(orchestrationType(version)))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(Constants.PERTAINS_TO_GOAL), urls(context, orchestration));
        var action = IAdapterFactory.forFhirContext(context)
                .createResource(orchestration)
                .resolvePathList("action")
                .get(0);
        assertEquals(List.of(OWNED_BY, ALLOWED), urls(context, action));
        assertTrue(seen.contains(
                new ExtensionPropagationContext(OWNED_BY, version, "PlanDefinition", orchestrationType(version))));
        assertTrue(seen.contains(new ExtensionPropagationContext(
                OWNED_BY, version, "PlanDefinition.action", orchestrationType(version) + ".action")));
        assertTrue(seen.contains(
                new ExtensionPropagationContext(OWNED_BY, version, "PlanDefinition.goal.target", "Goal.target")));
        assertTrue(seen.contains(
                new ExtensionPropagationContext(OWNED_BY, version, "ActivityDefinition", activityType(version))));
        assertFalse(seen.stream().anyMatch(copy -> copy.extensionUrl().equals(Constants.CPG_KNOWLEDGE_CAPABILITY)));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void nestedPlansInheritPolicy(FhirVersionEnum version) {
        var context = FhirContext.forCached(version);
        var repository = new InMemoryFhirRepository(context);
        var nested = plan(context);
        repository.update(nested);
        var outer = context.newJsonParser()
                .parseResource("""
                {"resourceType":"PlanDefinition", "id":"outer", "status":"active",
                 "action":[{%s}]}
                """.formatted(definition(version, "http://example.org/PlanDefinition/plan")));
        var resources = applyPlan(repository, settings(ExtensionPropagationPolicy.none()), outer);
        assertEquals(
                2,
                resources.stream()
                        .filter(r -> r.fhirType().equals(orchestrationType(version)))
                        .count());
        assertTrue(resources.stream().anyMatch(r -> r.fhirType().equals(activityType(version))));
        for (var resource : resources) {
            assertFalse(urls(context, resource).contains(OWNED_BY));
            assertFalse(urls(context, resource).contains(PLAN_DEFAULT));
        }
    }

    private static List<IBaseResource> applyPlan(
            InMemoryFhirRepository repository, CrSettings settings, IBaseResource plan) {
        var processor = new PlanDefinitionProcessor(repository, settings);
        var request = processor.buildApplyRequest(
                Eithers.forRight3(plan),
                "patient",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new LibraryEngine(repository, settings.getEvaluationSettings()));
        return BundleHelper.getEntryResources(processor.applyR5(request));
    }

    private static CrSettings settings(ExtensionPropagationPolicy policy) {
        return CrSettings.getDefault().withExtensionPropagationPolicy(policy);
    }

    private static String orchestrationType(FhirVersionEnum version) {
        return version == FhirVersionEnum.R5 ? "RequestOrchestration" : "RequestGroup";
    }

    private static String activityType(FhirVersionEnum version) {
        return version == FhirVersionEnum.DSTU3 ? "ProcedureRequest" : "Appointment";
    }

    private static String definition(FhirVersionEnum version, String reference) {
        return version == FhirVersionEnum.DSTU3
                ? "\"definition\":{\"reference\":\"" + reference + "\"}"
                : "\"definitionCanonical\":\"" + reference + "\"";
    }

    private static List<String> urls(FhirContext context, IBase element) {
        return IAdapterFactory.forFhirContext(context).createBase(element).getExtension().stream()
                .map(e -> e.getUrl())
                .toList();
    }

    private static IBaseResource plan(FhirContext context) {
        var version = context.getVersion().getVersion();
        var extensions = """
                {"url":"%s", "valueString":"mdm"}, {"url":"%s", "valueString":"keep"}
                """.formatted(OWNED_BY, ALLOWED);
        var machinery = """
                {"url":"%s", "valueCode":"shareable"}
                """.formatted(Constants.CPG_KNOWLEDGE_CAPABILITY);
        return context.newJsonParser().parseResource("""
                {"resourceType":"PlanDefinition", "id":"plan",
                 "url":"http://example.org/PlanDefinition/plan", "status":"active",
                 "extension":[%s, {"url":"%s", "valueBoolean":true}, %s],
                 "contained":[{"resourceType":"ActivityDefinition", "id":"activity",
                   "status":"active", "kind":"%s", "code":{"text":"Test activity"},
                   "extension":[%s, %s]}],
                 "goal":[{"description":{"text":"Test goal"}, "target":[{"detailQuantity":{"value":1}, "extension":[%s]}]}],
                 "action":[{"id":"parent", "extension":[%s],
                   "action":[{"id":"child", "extension":[%s], %s}]}]}
                """.formatted(
                        extensions,
                        PLAN_DEFAULT,
                        machinery,
                        activityType(version),
                        extensions,
                        machinery,
                        extensions,
                        extensions,
                        extensions,
                        definition(version, "#activity")));
    }
}
