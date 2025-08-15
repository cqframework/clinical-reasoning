package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmResourceInfoForCql;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4RepositorySubjectProvider subjectProvider;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();
    private final NpmPackageLoader npmPackageLoader;

    public R4MeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator,
            NpmPackageLoader npmPackageLoader) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.npmPackageLoader = npmPackageLoader;
    }

    @Override
    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String practitioner) {

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        var proxyRepoForMeasureProcessor =
                Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
        var processor = new R4MeasureProcessor(
                proxyRepoForMeasureProcessor,
                this.measureEvaluationOptions,
                this.measureProcessorUtils,
                this.npmPackageLoader);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository, npmPackageLoader);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        MeasureReport measureReport;

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        var evalType = r4MeasureServiceUtils.getMeasureEvalType(
                reportType, Optional.ofNullable(subjectId).map(List::of).orElse(List.of()));

        var subjects = getSubjects(subjectId, proxyRepoForMeasureProcessor, additionalData);

        var measurePlusNpmResourceHolder =
                R4MeasureServiceUtils.foldMeasure(measure, proxyRepoForMeasureProcessor, npmPackageLoader);

        // Replicate the old logic of using the repository used to initialize the measure processor
        // as the repository for the CQL engine context.
        // LUKETODO:  find and pass the the NPM resource load and loaded NPM resources here?
        var context = Engines.forRepository(
                proxyRepoForMeasureProcessor,
                this.measureEvaluationOptions.getEvaluationSettings(),
                additionalData,
                NpmPackageLoaderWithCache.of(measurePlusNpmResourceHolder.npmResourceHolder(), npmPackageLoader));

        var evaluationResults = processor.evaluateMeasureWithCqlEngine(
                subjects, measurePlusNpmResourceHolder, periodStart, periodEnd, parameters, context);

        measureReport = processor.evaluateMeasure(
                measurePlusNpmResourceHolder.getMeasure(),
                periodStart,
                periodEnd,
                reportType,
                subjects,
                evalType,
                context,
                evaluationResults);

        // add ProductLine after report is generated
        measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

        // add subject reference for non-individual reportTypes
        return r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);
    }

    // LUKETODO:  javadoc
    // LUKETODO:  top level
    static class NpmPackageLoaderWithCache implements NpmPackageLoader {
        private final List<NpmResourceInfoForCql> npmResourceHolders;
        private final NpmPackageLoader npmPackageLoader;

        public static NpmPackageLoaderWithCache of(NpmResourceInfoForCql npmResourceHolder, NpmPackageLoader npmPackageLoader) {
            return new NpmPackageLoaderWithCache(List.of(npmResourceHolder), npmPackageLoader);
        }

        public static NpmPackageLoaderWithCache of(List<NpmResourceInfoForCql> npmResourceHolders, NpmPackageLoader npmPackageLoader) {
            return new NpmPackageLoaderWithCache(npmResourceHolders, npmPackageLoader);
        }

        private NpmPackageLoaderWithCache(List<NpmResourceInfoForCql> npmResourceHolders, NpmPackageLoader npmPackageLoader) {
            this.npmResourceHolders = npmResourceHolders;
            this.npmPackageLoader = npmPackageLoader;
        }

        @Override
        public NpmResourceInfoForCql loadNpmResources(IPrimitiveType<String> measureUrl) {
            return npmResourceHolders.stream()
                .filter(npmResourceHolder -> isMeasureUrlMatch(npmResourceHolder, measureUrl))
                .findFirst()
                .orElseGet( () -> npmPackageLoader.loadNpmResources(measureUrl));
        }

        @Override
        public Optional<ILibraryAdapter> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {
            var optLibrary = npmResourceHolders.stream()
                .map(
                    npmResourceHolder -> npmResourceHolder.findMatchingLibrary(versionedIdentifier))
                .flatMap(Optional::stream)
                .findFirst();

            if (optLibrary.isPresent()) {
                return optLibrary;
            }

           return findLibraryFromUnrelatedNpmPackage(versionedIdentifier);
        }

        @Override
        public Optional<ILibraryAdapter> findMatchingLibrary(ModelIdentifier modelIdentifier) {
            var optLibrary = npmResourceHolders.stream()
                .map(
                    npmResourceHolder -> npmResourceHolder.findMatchingLibrary(modelIdentifier))
                .flatMap(Optional::stream)
                .findFirst();

            if (optLibrary.isPresent()) {
                return optLibrary;
            }

            return findLibraryFromUnrelatedNpmPackage(modelIdentifier);
        }

        @Override
        public List<NamespaceInfo> getAllNamespaceInfos() {
            return npmPackageLoader.getAllNamespaceInfos();
        }

        @Override
        public Optional<ILibraryAdapter> loadLibraryByUrl(String libraryUrl) {

            var optLibrary = npmResourceHolders.stream()
                .filter(npmResourceHolder -> isLibraryUrlMatch(npmResourceHolder, libraryUrl))
                .map(NpmResourceInfoForCql::getOptMainLibrary)
                .flatMap(Optional::stream)
                .findFirst();

            if (optLibrary.isPresent()) {
                return optLibrary;
            }

            return npmPackageLoader.loadLibraryByUrl(libraryUrl);
        }

        private static boolean isMeasureUrlMatch(NpmResourceInfoForCql npmResourceHolder, IPrimitiveType<String> measureUrl) {
            return npmResourceHolder.getMeasure()
                .map(measure -> measure.getUrl().equals(measureUrl.getValue()))
                .orElse(false);
        }

        private static boolean isLibraryUrlMatch(NpmResourceInfoForCql npmResourceHolder, String libraryUrl) {
            return npmResourceHolder.getMeasure()
                .map(library -> library.getUrl().equals(libraryUrl))
                .orElse(false);
        }
    }

    @Nonnull
    private List<String> getSubjects(
            String subjectId, IRepository proxyRepoForMeasureProcessor, Bundle additionalData) {
        final IRepository repoToUseForSubjectProvider;
        if (additionalData != null) {
            repoToUseForSubjectProvider = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        } else {
            repoToUseForSubjectProvider = proxyRepoForMeasureProcessor;
        }

        return subjectProvider
                .getSubjects(
                        repoToUseForSubjectProvider,
                        Optional.ofNullable(subjectId).map(List::of).orElse(List.of()))
                .toList();
    }
}
