package org.opencds.cqf.cql.evaluator.measure.dstu3;

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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureProcessor;
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
public class Dstu3MeasureProcessor implements MeasureProcessor<MeasureReport, Endpoint, Bundle> {

    private static Logger logger = LoggerFactory.getLogger(Dstu3MeasureProcessor.class);

    protected TerminologyProviderFactory terminologyProviderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected EndpointConverter endpointConverter;
    protected LibrarySourceProviderFactory librarySourceProviderFactory;
    protected FhirDalFactory fhirDalFactory;

    private static Map<ModelIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

    private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache;

    private CqlOptions cqlOptions = CqlOptions.defaultOptions();
    private RetrieveProviderConfig retrieveProviderConfig = RetrieveProviderConfig.defaultConfig();
    private MeasureEvaluationOptions measureEvaluationOptions = MeasureEvaluationOptions.defaultOptions();

    // TODO: This should all be collapsed down to FhirDal
    protected LibrarySourceProvider localLibrarySourceProvider;
    protected DataProvider localDataProvider;
    protected TerminologyProvider localTerminologyProvider;
    protected FhirDal localFhirDal;

    @Inject
    public Dstu3MeasureProcessor(TerminologyProviderFactory terminologyProviderFactory,
            DataProviderFactory dataProviderFactory, LibrarySourceProviderFactory librarySourceProviderFactory,
            FhirDalFactory fhirDalFactory, EndpointConverter endpointConverter) {
        this(terminologyProviderFactory, dataProviderFactory, librarySourceProviderFactory, fhirDalFactory,
                endpointConverter, null, null, null, null, null, null, null);
    }

    public Dstu3MeasureProcessor(TerminologyProviderFactory terminologyProviderFactory,
            DataProviderFactory dataProviderFactory, LibrarySourceProviderFactory librarySourceProviderFactory,
            FhirDalFactory fhirDalFactory, EndpointConverter endpointConverter,
            TerminologyProvider localTerminologyProvider, LibrarySourceProvider localLibrarySourceProvider,
            DataProvider localDataProvider, FhirDal localFhirDal, MeasureEvaluationOptions measureEvaluationOptions,
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
    }

    public Dstu3MeasureProcessor(TerminologyProvider localTerminologyProvider,
            LibrarySourceProvider localLibrarySourceProvider, DataProvider localDataProvider, FhirDal localFhirDal) {
        this(null, null, null, null, null, localTerminologyProvider, localLibrarySourceProvider, localDataProvider,
                localFhirDal, null, null, null);
    }

    public MeasureReport evaluateMeasure(String url, String periodStart, String periodEnd, String reportType,
            String subject, String practitioner, String lastReceivedOn, Endpoint contentEndpoint,
            Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData) {

        if (lastReceivedOn != null) {
            logger.warn(
                    "the Measure evaluate implementation does not yet support the lastReceivedOn parameter. Ignoring.");
        }

        FhirDal fhirDal = contentEndpoint != null
                ? this.fhirDalFactory.create(this.endpointConverter.getEndpointInfo(contentEndpoint))
                : localFhirDal;

        if (fhirDal == null) {
            throw new IllegalStateException("a fhirDal was not provided and one could not be constructed");
        }

        MeasureEvalType measureEvalType = MeasureEvalType.fromCode(reportType);
        Iterable<String> subjectIds = this.getSubjects(measureEvalType,
                subject != null ? subject : practitioner, dataEndpoint, additionalData);

        Iterable<IBaseResource> measures = fhirDal.searchByUrl("Measure", url);
        Iterator<IBaseResource> measureIter = measures.iterator();
        if (!measureIter.hasNext()) {
            throw new IllegalArgumentException(String.format("Unable to locate Measure with url %s", url));
        }

        Measure measure = (Measure) measureIter.next();

        MeasureReport measureReport = evaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds, fhirDal,
                contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

        MeasureScoring measureScoring = MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        Dstu3MeasureReportScorer scorer = new Dstu3MeasureReportScorer();
        scorer.score(measureScoring, measureReport);
        return measureReport;
    }

    public Iterable<String> getSubjects(String reportType, String subjectId) {
        return this.getSubjects(reportType, subjectId, null, null);
    }

    public Iterable<String> getSubjects(String reportType, String subjectId, Endpoint dataEndpoint, Bundle additionalData) {
        MeasureEvalType measureEvalType = MeasureEvalType.fromCode(reportType);
        return getSubjects(measureEvalType, subjectId, dataEndpoint, additionalData);
    }

    public Iterable<String> getSubjects(MeasureEvalType measureEvalType, String subjectId, Endpoint dataEndpoint,
            Bundle additionalData) {
        CompositeFhirDal compositeFhirDal;
        BundleFhirDal bundleDal = null;
        FhirDal endpointDal = null;

        if (this.fhirDalFactory != null && dataEndpoint != null) {
            endpointDal = this.fhirDalFactory.create(this.endpointConverter.getEndpointInfo(dataEndpoint));
        }
        if (additionalData != null) {
            bundleDal = new BundleFhirDal(FhirContext.forCached(FhirVersionEnum.DSTU3), additionalData);
        }

        compositeFhirDal = new CompositeFhirDal(bundleDal, endpointDal, localFhirDal);
        SubjectProvider subjectProvider = new Dstu3FhirDalSubjectProvider(compositeFhirDal);
        return subjectProvider.getSubjects(measureEvalType, subjectId);
    }

    public MeasureReport evaluateMeasure(Measure measure, String periodStart, String periodEnd, String reportType,
                                                               Iterable<String> subjectIds, FhirDal fhirDal, Endpoint contentEndpoint, Endpoint terminologyEndpoint,
                                                               Endpoint dataEndpoint, Bundle additionalData) {

        var subjectIterator = subjectIds.iterator();
        var ids = new ArrayList<String>();
        int threadBatchSize = this.measureEvaluationOptions.getThreadedBatchSize();
        List<CompletableFuture<MeasureReport>> futures = new ArrayList<>();
        while (subjectIterator.hasNext()){
            ids.add(subjectIterator.next());
            if (ids.size() % threadBatchSize == 0) {
                var idsTr = new ArrayList<String>();
                idsTr.addAll(ids);
                futures.add(runEvaluate(measure, periodStart, periodEnd, reportType, idsTr, fhirDal,
                        contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData));
                ids.clear();
            }
        }
        futures.add(runEvaluate(measure, periodStart, periodEnd, reportType, ids, fhirDal,
                contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData) );
        List<MeasureReport> reports = new ArrayList<>();
        futures.forEach(x -> reports.add(x.join()));
        Dstu3MeasureReportAggregator reportAggregator = new Dstu3MeasureReportAggregator();
        return reportAggregator.aggregate(reports);
    }

    protected CompletableFuture<MeasureReport> runEvaluate(Measure measure, String periodStart, String periodEnd,
                                                                                 String reportType,
                                                                                 List<String> subjectIds, FhirDal fhirDal, Endpoint contentEndpoint, Endpoint terminologyEndpoint,
                                                                                 Endpoint dataEndpoint, Bundle additionalData) {

        ExecutorService executor = Executors.newFixedThreadPool(this.measureEvaluationOptions.getNumThreads());
        if (measureEvaluationOptions.isThreadedEnabled()) {
            return CompletableFuture.supplyAsync(
                    () -> this.innerEvaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds,
                            fhirDal, contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData),
                    executor);
        }
        else {
            return CompletableFuture.completedFuture(this.innerEvaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds,
                    fhirDal, contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData));
        }

    }

    protected MeasureReport innerEvaluateMeasure(Measure measure, String periodStart, String periodEnd,
            String reportType, Iterable<String> subjectIds, FhirDal fhirDal, Endpoint contentEndpoint,
            Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData) {

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(
                    String.format("Measure %s does not have a primary library specified", measure.getUrl()));
        }

        Reference libraryUrl = measure.getLibrary().get(0);

        var primaryLibrary = (org.hl7.fhir.dstu3.model.Library) fhirDal.read(libraryUrl.getReferenceElement());
        LibrarySourceProvider librarySourceProvider = contentEndpoint != null
                ? this.librarySourceProviderFactory.create(this.endpointConverter.getEndpointInfo(contentEndpoint))
                : localLibrarySourceProvider;

        if (librarySourceProvider == null) {
            throw new IllegalStateException(
                    "a librarySourceProvider was not provided and one could not be constructed");
        }

        LibraryLoader libraryLoader = this.buildLibraryLoader(librarySourceProvider);

        Library library = libraryLoader.load(
                new VersionedIdentifier().withId(primaryLibrary.getName()).withVersion(primaryLibrary.getVersion()));

        TerminologyProvider terminologyProvider = terminologyEndpoint != null
                ? this.buildTerminologyProvider(terminologyEndpoint)
                : this.localTerminologyProvider;

        if (terminologyProvider == null) {
            throw new IllegalStateException("a terminologyProvider was not provided and one could not be constructed");
        }

        DataProvider dataProvider = (dataEndpoint != null || additionalData != null)
                ? this.buildDataProvider(dataEndpoint, additionalData, terminologyProvider)
                : this.localDataProvider;

        if (dataProvider == null) {
            throw new IllegalStateException("a dataProvider was not provided and one could not be constructed");
        }

        Interval measurementPeriod = null;
        if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
            measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
        }

        Context context = this.buildMeasureContext(library, libraryLoader, terminologyProvider, dataProvider);

        Dstu3MeasureEvaluation measureEvaluator = new Dstu3MeasureEvaluation(context, measure);
        return measureEvaluator.evaluate(MeasureEvalType.fromCode(reportType), subjectIds, measurementPeriod);
    }

    // TODO: This is duplicate logic from the evaluator builder
    private LibraryLoader buildLibraryLoader(LibrarySourceProvider librarySourceProvider) {
        List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
        librarySourceProviders.add(librarySourceProvider);
        if (this.cqlOptions.useEmbeddedLibraries()) {
            librarySourceProviders.add(new FhirLibrarySourceProvider());
        }

        /* NOTE: Npm package support not implemented for Dstu3 measure processing */
        TranslatorOptionAwareLibraryLoader libraryLoader = new TranslatingLibraryLoader(
                new CacheAwareModelManager(globalModelCache), librarySourceProviders,
                this.cqlOptions.getCqlTranslatorOptions(), null);

        if (this.libraryCache != null) {
            libraryLoader = new CacheAwareLibraryLoaderDecorator(libraryLoader, this.libraryCache);
        }

        return libraryLoader;
    }

    private Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
        // resolve the measurement period
        return new Interval(DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodStart, true)), true,
                DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodEnd, false)), true);
    }

    // TODO: This is duplicate logic from the evaluator builder
    private DataProvider buildDataProvider(Endpoint dataEndpoint, Bundle additionalData,
            TerminologyProvider terminologyProvider) {
        if (dataEndpoint != null && additionalData != null) {
            throw new IllegalArgumentException(
                    "dataEndpoint and additionalData parameters are currently mutually exclusive. Use only one.");
        }

        if (dataEndpoint == null && additionalData == null) {
            throw new IllegalArgumentException("Either dataEndpoint or additionalData must be specified");
        }

        DataProviderComponents dataProvider = null;
        if (dataEndpoint != null) {
            dataProvider = this.dataProviderFactory.create(this.endpointConverter.getEndpointInfo(dataEndpoint));
        } else {
            dataProvider = this.dataProviderFactory.create(additionalData);
        }

        RetrieveProviderConfigurer retrieveProviderConfigurer = new RetrieveProviderConfigurer(retrieveProviderConfig);

        retrieveProviderConfigurer.configure(dataProvider.getRetrieveProvider(), terminologyProvider);

        return new CompositeDataProvider(dataProvider.getModelResolver(), dataProvider.getRetrieveProvider());
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

        if (this.cqlOptions.getCqlEngineOptions().getOptions().contains(CqlEngine.Options.EnableExpressionCaching)) {
            context.setExpressionCaching(true);
        }

        if (this.cqlOptions.getCqlEngineOptions().isDebugLoggingEnabled()) {
            context.getDebugMap().setIsLoggingEnabled(true);
        }

        return context;
    }
}