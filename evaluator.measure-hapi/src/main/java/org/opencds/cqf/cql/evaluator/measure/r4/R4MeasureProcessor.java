package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.terminology.PrivateCachingTerminologyProviderDecorator;
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.CompositeFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.ResourceValidator;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureProcessor;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;
import org.opencds.cqf.cql.evaluator.measure.helper.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

// TODO: This class needs a bit of refactoring to match the patterns that
// have been defined in other parts of the cql-evaluator project. The main issue
// is the direct use of engine Context.
@Named
public class R4MeasureProcessor implements MeasureProcessor<MeasureReport, Endpoint, Bundle> {

    private static Logger logger = LoggerFactory.getLogger(R4MeasureProcessor.class);

    protected TerminologyProviderFactory terminologyProviderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected EndpointConverter endpointConverter;
    protected LibrarySourceProviderFactory librarySourceProviderFactory;
    protected FhirDalFactory fhirDalFactory;

    private static Map<ModelIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

    private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache;

    private CqlOptions cqlOptions = CqlOptions.defaultOptions();

    private RetrieveProviderConfig retrieveProviderConfig = RetrieveProviderConfig.defaultConfig();
    private MeasureEvaluationOptions measureEvaluationOptions =
            MeasureEvaluationOptions.defaultOptions();

    private ResourceValidator validator;

    // TODO: This should all be collapsed down to FhirDal
    protected LibrarySourceProvider localLibrarySourceProvider;
    protected DataProvider localDataProvider;
    protected TerminologyProvider localTerminologyProvider;
    protected FhirDal localFhirDal;

    @Inject
    public R4MeasureProcessor(TerminologyProviderFactory terminologyProviderFactory,
            DataProviderFactory dataProviderFactory,
            LibrarySourceProviderFactory librarySourceProviderFactory,
            FhirDalFactory fhirDalFactory, EndpointConverter endpointConverter) {
        this(terminologyProviderFactory, dataProviderFactory, librarySourceProviderFactory,
                fhirDalFactory, endpointConverter, null, null, null, null, null, null, null);
    }

    public R4MeasureProcessor(TerminologyProviderFactory terminologyProviderFactory,
            DataProviderFactory dataProviderFactory,
            LibrarySourceProviderFactory librarySourceProviderFactory,
            FhirDalFactory fhirDalFactory, EndpointConverter endpointConverter,
            TerminologyProvider localTerminologyProvider,
            LibrarySourceProvider localLibrarySourceProvider, DataProvider localDataProvider,
            FhirDal localFhirDal, MeasureEvaluationOptions measureEvaluationOptions,
            CqlOptions cqlOptions,
            Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache) {
        this.terminologyProviderFactory = terminologyProviderFactory;
        this.dataProviderFactory = dataProviderFactory;
        this.librarySourceProviderFactory = librarySourceProviderFactory;
        this.endpointConverter = endpointConverter;
        this.fhirDalFactory = fhirDalFactory;

        this.localTerminologyProvider = localTerminologyProvider;
        this.localLibrarySourceProvider = localLibrarySourceProvider;
        this.localFhirDal = localFhirDal;
        this.localDataProvider = localDataProvider;

        this.libraryCache = libraryCache;

        if (measureEvaluationOptions != null) {
            this.measureEvaluationOptions = measureEvaluationOptions;
        }

        if (cqlOptions != null) {
            this.cqlOptions = cqlOptions;
        }

        if (this.measureEvaluationOptions.isValidationEnabled()) {
            createValidator();
        }
    }

    public R4MeasureProcessor(TerminologyProvider localTerminologyProvider,
            LibrarySourceProvider localLibrarySourceProvider, DataProvider localDataProvider,
            FhirDal localFhirDal) {
        this(null, null, null, null, null, localTerminologyProvider, localLibrarySourceProvider,
                localDataProvider, localFhirDal, null, null, null);
    }

    protected void createValidator() {
        this.validator = new ResourceValidator(FhirVersionEnum.R4,
                this.measureEvaluationOptions.getValidationProfiles(), this.localFhirDal);
    }

    public void setValidationEnabled(boolean value) {
        this.measureEvaluationOptions.setValidationEnabled(value);
        if (value) {
            createValidator();
        } else {
            this.validator = null;
        }
    }

    public MeasureReport evaluateMeasure(String url, String periodStart, String periodEnd,
            String reportType, String subject, String practitioner, String lastReceivedOn,
            Endpoint contentEndpoint, Endpoint terminologyEndpoint, Endpoint dataEndpoint,
            Bundle additionalData) {

        List<String> subjectIds = this.getSubjects(reportType,
                subject != null ? subject : practitioner, dataEndpoint, additionalData);

        return evaluateMeasure(url, periodStart, periodEnd, reportType, subjectIds, lastReceivedOn,
                contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);
    }

    public MeasureReport evaluateMeasure(String url, String periodStart, String periodEnd,
            String reportType, List<String> subjectIds, String lastReceivedOn,
            Endpoint contentEndpoint, Endpoint terminologyEndpoint, Endpoint dataEndpoint,
            Bundle additionalData) {

        // TODO: Need a federated FhirDal..
        FhirDal fhirDal =
                contentEndpoint != null
                        ? this.fhirDalFactory
                                .create(this.endpointConverter.getEndpointInfo(contentEndpoint))
                        : localFhirDal;

        measureEvalValidation(lastReceivedOn, fhirDal);

        Measure measure = getMeasure(fhirDal, url);

        MeasureReport measureReport = this.evaluateMeasure(measure, periodStart, periodEnd,
                reportType, subjectIds, fhirDal, contentEndpoint, terminologyEndpoint, dataEndpoint,
                additionalData);
        MeasureScoring measureScoring =
                MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoring, measureReport);

        return measureReport;
    }

    private void measureEvalValidation(String lastReceivedOn, FhirDal fhirDal) {
        if (lastReceivedOn != null) {
            logger.warn(
                    "the Measure evaluate implementation does not yet support the lastReceivedOn parameter. Ignoring.");
        }

        if (fhirDal == null) {
            throw new IllegalStateException(
                    "a fhirDal was not provided and one could not be constructed");
        }
    }

    private Measure getMeasure(FhirDal fhirDal, String url) {
        Iterable<IBaseResource> measures = fhirDal.searchByUrl("Measure", url);
        Iterator<IBaseResource> measureIter = measures.iterator();
        if (!measureIter.hasNext()) {
            throw new IllegalArgumentException(
                    String.format("Unable to locate Measure with url %s", url));
        }

        return (Measure) measureIter.next();
    }

    public static <T> List<List<T>> getBatches(List<T> collection, int batchSize) {
        int i = 0;
        List<List<T>> batches = new ArrayList<>();
        while (i < collection.size()) {
            int nextInc = Math.min(collection.size() - i, batchSize);
            List<T> batch = collection.subList(i, i + nextInc);
            batches.add(batch);
            i = i + nextInc;
        }

        return batches;
    }

    public List<String> getSubjects(String reportType, String subjectId, Endpoint dataEndpoint) {
        MeasureEvalType measureEvalType = MeasureEvalType.fromCode(reportType);
        return getSubjects(measureEvalType, subjectId, dataEndpoint, null);
    }

    public List<String> getSubjects(String reportType, String subjectId, Bundle additionalData) {
        MeasureEvalType measureEvalType = MeasureEvalType.fromCode(reportType);
        return getSubjects(measureEvalType, subjectId, null, additionalData);
    }

    public List<String> getSubjects(String reportType, String subjectId, Endpoint dataEndpoint,
            Bundle additionalData) {
        MeasureEvalType measureEvalType = MeasureEvalType.fromCode(reportType);
        return getSubjects(measureEvalType, subjectId, dataEndpoint, additionalData);
    }

    public List<String> getSubjects(MeasureEvalType measureEvalType, String subjectId,
            Endpoint dataEndpoint, Bundle additionalData) {
        CompositeFhirDal compositeFhirDal;
        BundleFhirDal bundleDal = null;
        FhirDal endpointDal = null;

        if (this.fhirDalFactory != null && dataEndpoint != null) {
            endpointDal = this.fhirDalFactory
                    .create(this.endpointConverter.getEndpointInfo(dataEndpoint));
        }
        if (additionalData != null) {
            bundleDal =
                    new BundleFhirDal(FhirContext.forCached(FhirVersionEnum.R4), additionalData);
        }

        compositeFhirDal = new CompositeFhirDal(bundleDal, endpointDal, localFhirDal);
        SubjectProvider subjectProvider = new R4FhirDalSubjectProvider(compositeFhirDal);
        return subjectProvider.getSubjects(measureEvalType, subjectId);

    }

    public MeasureReport evaluateMeasure(Measure measure, String periodStart, String periodEnd,
            String reportType, List<String> subjectIds, FhirDal fhirDal, Endpoint contentEndpoint,
            Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData) {
        if (Boolean.TRUE.equals(this.measureEvaluationOptions.isValidationEnabled())) {
            if (this.validator == null) {
                // Throw or log?
                logger.error(
                        "Validation is enabled and no validator has been found. Check measure validation configuration.");
            } else {
                this.validator.validate(measure, true);
            }
        }

        if (this.measureEvaluationOptions.isThreadedEnabled()
                && subjectIds.size() > this.measureEvaluationOptions.getThreadedBatchSize()) {
            return threadedMeasureEvaluate(measure, periodStart, periodEnd, reportType, subjectIds,
                    fhirDal, contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);
        } else {
            return innerEvaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds,
                    fhirDal, contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

        }

    }

    protected MeasureReport threadedMeasureEvaluate(Measure measure, String periodStart,
            String periodEnd, String reportType, List<String> subjectIds, FhirDal fhirDal,
            Endpoint contentEndpoint, Endpoint terminologyEndpoint, Endpoint dataEndpoint,
            Bundle additionalData) {
        List<List<String>> batches =
                getBatches(subjectIds, this.measureEvaluationOptions.getThreadedBatchSize());
        ExecutorService executor =
                Executors.newFixedThreadPool(this.measureEvaluationOptions.getNumThreads());
        List<CompletableFuture<MeasureReport>> futures = new ArrayList<>();
        for (List<String> idBatch : batches) {
            futures.add(
                    CompletableFuture
                            .supplyAsync(
                                    () -> this.innerEvaluateMeasure(measure, periodStart, periodEnd,
                                            reportType, idBatch, fhirDal, contentEndpoint,
                                            terminologyEndpoint, dataEndpoint, additionalData),
                                    executor));
        }

        List<MeasureReport> reports = new ArrayList<>();
        futures.forEach(x -> reports.add(x.join()));
        R4MeasureReportAggregator reportAggregator = new R4MeasureReportAggregator();
        return reportAggregator.aggregate(reports);
    }

    protected MeasureReport innerEvaluateMeasure(Measure measure, String periodStart,
            String periodEnd, String reportType, List<String> subjectIds, FhirDal fhirDal,
            Endpoint contentEndpoint, Endpoint terminologyEndpoint, Endpoint dataEndpoint,
            Bundle additionalData) {

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(String.format(
                    "Measure %s does not have a primary library specified", measure.getUrl()));
        }

        CanonicalType libraryUrl = measure.getLibrary().get(0);

        Iterable<IBaseResource> libraries = fhirDal.searchByUrl("Library", libraryUrl.getValue());
        Iterator<IBaseResource> libraryIter = libraries.iterator();
        if (!libraryIter.hasNext()) {
            throw new IllegalArgumentException(String
                    .format("Unable to locate primary Library with url %s", libraryUrl.getValue()));
        }

        org.hl7.fhir.r4.model.Library primaryLibrary =
                (org.hl7.fhir.r4.model.Library) libraryIter.next();

        LibrarySourceProvider librarySourceProvider = contentEndpoint != null
                ? this.librarySourceProviderFactory
                        .create(this.endpointConverter.getEndpointInfo(contentEndpoint))
                : localLibrarySourceProvider;

        if (librarySourceProvider == null) {
            throw new IllegalStateException(
                    "a librarySourceProvider was not provided and one could not be constructed");
        }

        LibraryLoader libraryLoader = this.buildLibraryLoader(librarySourceProvider);

        Library library = libraryLoader.load(new VersionedIdentifier()
                .withId(primaryLibrary.getName()).withVersion(primaryLibrary.getVersion()));

        TerminologyProvider terminologyProvider =
                terminologyEndpoint != null ? this.buildTerminologyProvider(terminologyEndpoint)
                        : this.localTerminologyProvider;

        if (terminologyProvider == null) {
            throw new IllegalStateException(
                    "a terminologyProvider was not provided and one could not be constructed");
        }

        DataProvider dataProvider = (dataEndpoint != null || additionalData != null)
                ? this.buildDataProvider(dataEndpoint, additionalData, terminologyProvider)
                : this.localDataProvider;

        if (dataProvider == null) {
            throw new IllegalStateException(
                    "a dataProvider was not provided and one could not be constructed");
        }


        Interval measurementPeriod = null;
        if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
            measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
        }

        Context context =
                this.buildMeasureContext(library, libraryLoader, terminologyProvider, dataProvider);
        R4MeasureEvaluation measureEvaluator = new R4MeasureEvaluation(context, measure);
        return measureEvaluator.evaluate(MeasureEvalType.fromCode(reportType), subjectIds,
                measurementPeriod);
    }

    protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
        switch (measureEvalType) {
            case PATIENT:
            case SUBJECT:
                return MeasureReportType.INDIVIDUAL;
            case PATIENTLIST:
            case SUBJECTLIST:
                return MeasureReportType.PATIENTLIST;
            case POPULATION:
                return MeasureReportType.SUMMARY;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported MeasureEvalType: %s", measureEvalType.toCode()));
        }
    }

    // TODO: This is duplicate logic from the evaluator builder
    // TODO: Add NPM library source loader support
    private LibraryLoader buildLibraryLoader(LibrarySourceProvider librarySourceProvider) {
        List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
        librarySourceProviders.add(librarySourceProvider);
        if (this.cqlOptions.useEmbeddedLibraries()) {
            librarySourceProviders.add(new FhirLibrarySourceProvider());
        }

        TranslatorOptionAwareLibraryLoader libraryLoader =
                new TranslatingLibraryLoader(new CacheAwareModelManager(globalModelCache),
                        librarySourceProviders, this.cqlOptions.getCqlTranslatorOptions(), null);

        if (this.libraryCache != null) {
            libraryLoader = new CacheAwareLibraryLoaderDecorator(libraryLoader, this.libraryCache);
        }

        return libraryLoader;
    }

    private Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
        // resolve the measurement period
        return new Interval(DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodStart, true)),
                true, DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodEnd, false)), true);
    }

    // TODO: This is duplicate logic from the evaluator builder
    private DataProvider buildDataProvider(Endpoint dataEndpoint, Bundle additionalData,
            TerminologyProvider terminologyProvider) {
        if (dataEndpoint != null && additionalData != null) {
            throw new IllegalArgumentException(
                    "dataEndpoint and additionalData parameters are currently mutually exclusive. Use only one.");
        }

        if (dataEndpoint == null && additionalData == null) {
            throw new IllegalArgumentException(
                    "Either dataEndpoint or additionalData must be specified");
        }

        DataProviderComponents dataProvider;
        if (dataEndpoint != null) {
            dataProvider = this.dataProviderFactory
                    .create(this.endpointConverter.getEndpointInfo(dataEndpoint));
        } else {
            dataProvider = this.dataProviderFactory.create(additionalData);
        }

        RetrieveProviderConfigurer retrieveProviderConfigurer =
                new RetrieveProviderConfigurer(retrieveProviderConfig);

        retrieveProviderConfigurer.configure(dataProvider.getRetrieveProvider(),
                terminologyProvider);

        return new CompositeDataProvider(dataProvider.getModelResolver(),
                dataProvider.getRetrieveProvider());
    }

    // TODO: This is duplicate logic from the evaluator builder
    private TerminologyProvider buildTerminologyProvider(Endpoint terminologyEndpoint) {
        if (terminologyEndpoint != null) {
            return new PrivateCachingTerminologyProviderDecorator(this.terminologyProviderFactory
                    .create(this.endpointConverter.getEndpointInfo(terminologyEndpoint)));
        }

        return null;
    }

    // TODO: This is duplicate logic from the evaluator builder
    private Context buildMeasureContext(Library primaryLibrary, LibraryLoader libraryLoader,
            TerminologyProvider terminologyProvider, DataProvider dataProvider) {
        Context context = new Context(primaryLibrary);
        context.registerLibraryLoader(libraryLoader);
        context.registerTerminologyProvider(terminologyProvider);
        context.registerDataProvider(Constants.FHIR_MODEL_URI, dataProvider);
        context.setDebugMap(new DebugMap());

        if (this.cqlOptions.getCqlEngineOptions().getOptions()
                .contains(CqlEngine.Options.EnableExpressionCaching)) {
            context.setExpressionCaching(true);
        }

        if (this.cqlOptions.getCqlEngineOptions().isDebugLoggingEnabled()) {
            context.getDebugMap().setIsLoggingEnabled(true);
        }

        return context;
    }
}
