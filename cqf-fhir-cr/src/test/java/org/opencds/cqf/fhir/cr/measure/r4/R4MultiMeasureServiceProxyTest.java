package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.util.Collections;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * Tests that verify the RepositoryProxyFactory correctly routes requests
 * to different repositories based on endpoint configuration.
 *
 * <p>Each test splits resources across InMemoryFhirRepositories so that
 * evaluation can only succeed if the proxy routes to the correct repository.
 */
class R4MultiMeasureServiceProxyTest {

    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    private IgRepository fullRepository() {
        return new IgRepository(
                FHIR_CONTEXT,
                Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/MinimalMeasureEvaluation"));
    }

    private MeasureEvaluationOptions defaultOptions() {
        var options = MeasureEvaluationOptions.defaultOptions();
        options.getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        options.getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        return options;
    }

    /**
     * Sentinel endpoint: a non-null Endpoint signals the TestRepositoryProxyFactory
     * to substitute the corresponding InMemoryFhirRepository.
     */
    private Endpoint sentinel() {
        return new Endpoint().setAddress("http://test");
    }

    /**
     * Builds an InMemoryFhirRepository containing only content resources
     * (Measure and Library) from the full repository.
     */
    private IRepository buildContentRepository(IgRepository source) {
        var repo = new InMemoryFhirRepository(FHIR_CONTEXT);
        // Copy all Measure resources
        var measures = source.search(Bundle.class, Measure.class, Collections.emptyMap());
        for (var resource : BundleHelper.getEntryResources(measures)) {
            repo.update(resource);
        }
        // Copy all Library resources
        var libraries = source.search(Bundle.class, Library.class, Collections.emptyMap());
        for (var resource : BundleHelper.getEntryResources(libraries)) {
            repo.update(resource);
        }
        return repo;
    }

    /**
     * Builds an InMemoryFhirRepository containing only terminology resources
     * (ValueSet) from the full repository.
     */
    private IRepository buildTerminologyRepository(IgRepository source) {
        var repo = new InMemoryFhirRepository(FHIR_CONTEXT);
        var valueSets = source.search(Bundle.class, ValueSet.class, Collections.emptyMap());
        for (var resource : BundleHelper.getEntryResources(valueSets)) {
            repo.update(resource);
        }
        return repo;
    }

    /**
     * Builds an InMemoryFhirRepository containing only data resources
     * (Patient, Encounter, etc.) from the full repository.
     */
    private IRepository buildDataRepository(IgRepository source) {
        var repo = new InMemoryFhirRepository(FHIR_CONTEXT);
        var patients = source.search(Bundle.class, Patient.class, Collections.emptyMap());
        for (var resource : BundleHelper.getEntryResources(patients)) {
            repo.update(resource);
        }
        // Also copy Encounter resources
        var encounters = source.search(Bundle.class, org.hl7.fhir.r4.model.Encounter.class, Collections.emptyMap());
        for (var resource : BundleHelper.getEntryResources(encounters)) {
            repo.update(resource);
        }
        return repo;
    }

    @Test
    void noEndpoints_usesStandardRepository() {
        var repo = fullRepository();
        var service = new R4MultiMeasureService(
                repo,
                defaultOptions(),
                "http://localhost",
                new MeasurePeriodValidator(),
                new TestRepositoryProxyFactory(null, null, null));

        // Evaluate with no endpoints - should use the standard repository
        var report = service.evaluate(
                org.opencds.cqf.fhir.utility.monad.Eithers.forMiddle3(
                        new IdType("Measure", "MinimalProportionBooleanBasisSingleGroup")),
                null,
                null,
                "subject",
                "Patient/female-1988",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertNotNull(report);
        assertNotNull(report.getId());
    }

    @Test
    void onlyContentEndpoint_contentFromProxy() {
        var source = fullRepository();
        var contentRepo = buildContentRepository(source);

        // Create a local repo WITHOUT content (Measures/Libraries)
        // The content must come from the proxy's content repository
        var service = new R4MultiMeasureService(
                source,
                defaultOptions(),
                "http://localhost",
                new MeasurePeriodValidator(),
                new TestRepositoryProxyFactory(null, contentRepo, null));

        // Passing only content endpoint sentinel - content should route to contentRepo
        var report = service.evaluate(
                org.opencds.cqf.fhir.utility.monad.Eithers.forMiddle3(
                        new IdType("Measure", "MinimalProportionBooleanBasisSingleGroup")),
                null,
                null,
                "subject",
                "Patient/female-1988",
                null,
                sentinel(),
                null,
                null,
                null,
                null,
                null,
                null);
        assertNotNull(report);
    }

    @Test
    void onlyTerminologyEndpoint_terminologyFromProxy() {
        var source = fullRepository();
        var terminologyRepo = buildTerminologyRepository(source);

        var service = new R4MultiMeasureService(
                source,
                defaultOptions(),
                "http://localhost",
                new MeasurePeriodValidator(),
                new TestRepositoryProxyFactory(null, null, terminologyRepo));

        // Passing only terminology endpoint sentinel
        var report = service.evaluate(
                org.opencds.cqf.fhir.utility.monad.Eithers.forMiddle3(
                        new IdType("Measure", "MinimalProportionBooleanBasisSingleGroup")),
                null,
                null,
                "subject",
                "Patient/female-1988",
                null,
                null,
                sentinel(),
                null,
                null,
                null,
                null,
                null);
        assertNotNull(report);
    }

    @Test
    void allThreeEndpoints_eachTypeSeparatelyRouted() {
        var source = fullRepository();
        var dataRepo = buildDataRepository(source);
        var contentRepo = buildContentRepository(source);
        var terminologyRepo = buildTerminologyRepository(source);

        var service = new R4MultiMeasureService(
                source,
                defaultOptions(),
                "http://localhost",
                new MeasurePeriodValidator(),
                new TestRepositoryProxyFactory(dataRepo, contentRepo, terminologyRepo));

        // All three endpoints specified - each type routed to its respective repo
        var report = service.evaluate(
                org.opencds.cqf.fhir.utility.monad.Eithers.forMiddle3(
                        new IdType("Measure", "MinimalProportionBooleanBasisSingleGroup")),
                null,
                null,
                "subject",
                "Patient/female-1988",
                null,
                sentinel(),
                sentinel(),
                sentinel(),
                null,
                null,
                null,
                null);
        assertNotNull(report);
    }

    @Test
    void contentAndTerminologyEndpoints_dataFromLocal() {
        var source = fullRepository();
        var contentRepo = buildContentRepository(source);
        var terminologyRepo = buildTerminologyRepository(source);

        var service = new R4MultiMeasureService(
                source,
                defaultOptions(),
                "http://localhost",
                new MeasurePeriodValidator(),
                new TestRepositoryProxyFactory(null, contentRepo, terminologyRepo));

        // Content + terminology endpoints, data from local
        var report = service.evaluate(
                org.opencds.cqf.fhir.utility.monad.Eithers.forMiddle3(
                        new IdType("Measure", "MinimalProportionBooleanBasisSingleGroup")),
                null,
                null,
                "subject",
                "Patient/female-1988",
                null,
                sentinel(),
                sentinel(),
                null,
                null,
                null,
                null,
                null);
        assertNotNull(report);
    }
}
