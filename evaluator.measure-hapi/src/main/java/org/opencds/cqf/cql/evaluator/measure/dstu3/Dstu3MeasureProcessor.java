package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Reference;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.terminology.PrivateCachingTerminologyProviderDecorator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureProcessor;
import org.opencds.cqf.cql.evaluator.measure.helper.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.EmbeddedFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;

import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;

// TODO: This class needs a bit of refactoring to match the patterns that
// have been defined in other parts of the cql-evaluator project. The main issue
// is the direct use of engine Context.
@Named
public class Dstu3MeasureProcessor implements MeasureProcessor<MeasureReport, Endpoint, Bundle> {

    private static Logger logger = LoggerFactory.getLogger(Dstu3MeasureProcessor.class);

    protected TerminologyProviderFactory terminologyProviderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected EndpointConverter endpointConverter;
    protected LibraryContentProviderFactory libraryContentProviderFactory;
    protected FhirDalFactory fhirDalFactory;

    private static Map<org.hl7.elm.r1.VersionedIdentifier, Model> globalModelCache = new HashMap<>();

    private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();
    private RetrieveProviderConfig retrieveProviderConfig = RetrieveProviderConfig.defaultConfig();

    @Inject
    public Dstu3MeasureProcessor(TerminologyProviderFactory terminologyProviderFactory,
            DataProviderFactory dataProviderFactory, LibraryContentProviderFactory libraryContentProviderFactory, FhirDalFactory fhirDalFactory,
            EndpointConverter endpointConverter) {
        this.terminologyProviderFactory = terminologyProviderFactory;
        this.dataProviderFactory = dataProviderFactory;
        this.libraryContentProviderFactory = libraryContentProviderFactory;
        this.endpointConverter = endpointConverter;
        this.fhirDalFactory = fhirDalFactory;

    }

    public MeasureReport evaluateMeasure(String url, String periodStart, String periodEnd, String reportType, String subject,
            String practitioner, String lastReceivedOn, Endpoint contentEndpoint, Endpoint terminologyEndpoint,
            Endpoint dataEndpoint, Bundle additionalData) {

        if (lastReceivedOn != null) {
            logger.warn("the Measure evaluate implementation does not yet support the lastReceivedOn parameter. Ignoring.");
        }
        
        FhirDal fhirDal = this.fhirDalFactory.create(this.endpointConverter.getEndpointInfo(contentEndpoint));

        Iterable<IBaseResource> measures = fhirDal.searchByUrl("Measure", url);
        Iterator<IBaseResource> measureIter = measures.iterator();
        if (!measureIter.hasNext()) {
            throw new IllegalArgumentException(String.format("Unable to locate Measure with url %s", url));
        }

        Measure measure = (Measure)measureIter.next();

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(String.format("Measure %s does not have a primary library specified", url)); 
        }

        Reference libraryReference = measure.getLibrary().get(0);

        org.hl7.fhir.dstu3.model.Library primaryLibrary = (org.hl7.fhir.dstu3.model.Library)fhirDal.read(new IdType(libraryReference.getId()));
        if (primaryLibrary == null) {
            throw new IllegalArgumentException(String.format("Unable to locate primary Library with id %s", libraryReference.getId()));
        }

        LibraryContentProvider libraryContentProvider = this.libraryContentProviderFactory.create(this.endpointConverter.getEndpointInfo(contentEndpoint));
        LibraryLoader libraryLoader = this.buildLibraryLoader(libraryContentProvider);

        Library library = libraryLoader.load(new VersionedIdentifier().withId(primaryLibrary.getName()).withVersion(primaryLibrary.getVersion()));

        TerminologyProvider terminologyProvider = this.buildTerminologyProvider(terminologyEndpoint);
        DataProvider dataProvider = this.buildDataProvider(dataEndpoint, additionalData, terminologyProvider);
        Interval measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
        Context context = this.buildMeasureContext(library, libraryLoader, terminologyProvider, dataProvider);

        Dstu3MeasureEvaluation measureEvaluation = new Dstu3MeasureEvaluation(context, measure);

        return measureEvaluation.evaluate(MeasureEvalType.fromCode(reportType), subject, measurementPeriod);
    }

    // TODO: This is duplicate logic from the evaluator builder
    private LibraryLoader buildLibraryLoader(LibraryContentProvider libraryContentProvider) {
        List<LibraryContentProvider> libraryContentProviders = new ArrayList<>();
        libraryContentProviders.add(libraryContentProvider);
        libraryContentProviders.add(new EmbeddedFhirLibraryContentProvider());

        TranslatorOptionAwareLibraryLoader libraryLoader = new TranslatingLibraryLoader(
                new CacheAwareModelManager(globalModelCache), libraryContentProviders, this.cqlTranslatorOptions);

        return libraryLoader;
    }

    private Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
        // resolve the measurement period
        return new Interval(DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodStart, true)), true,
                DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodEnd, false)), true);
    }

    // TODO: This is duplicate logic from the evaluator builder
    private DataProvider buildDataProvider(Endpoint dataEndpoint, Bundle additionalData, TerminologyProvider terminologyProvider) {
        if (dataEndpoint != null && additionalData != null) {
            throw new IllegalArgumentException(
                    "dataEndpoint and additionalData parameters are currently mutually exclusive. Use only one.");
        }

        if (dataEndpoint == null && additionalData == null) {
            throw new IllegalArgumentException(
                "Either dataEndpoint or additionalData must be specified");
        }
        Triple<String, ModelResolver, RetrieveProvider> dataProvider = null;
        if (dataEndpoint != null) {
            dataProvider = this.dataProviderFactory.create(this.endpointConverter.getEndpointInfo(dataEndpoint));
        } else {
            dataProvider = this.dataProviderFactory.create(additionalData);
        }

        RetrieveProviderConfigurer retrieveProviderConfigurer = new RetrieveProviderConfigurer(
            retrieveProviderConfig);

        retrieveProviderConfigurer.configure(dataProvider.getRight(), terminologyProvider);

        return new CompositeDataProvider(dataProvider.getMiddle(), dataProvider.getRight());
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
    private Context buildMeasureContext(Library primaryLibrary, LibraryLoader libraryLoader, TerminologyProvider terminologyProvider, DataProvider dataProvider) {
        Context context = new Context(primaryLibrary);
        context.registerLibraryLoader(libraryLoader);
        context.registerTerminologyProvider(terminologyProvider);
        context.registerDataProvider(Constants.FHIR_MODEL_URI, dataProvider);
        return context;
    }

}
