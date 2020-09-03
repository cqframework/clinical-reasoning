package org.opencds.cqf.cql.evaluator.library;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.ParameterParser;
import org.opencds.cqf.cql.evaluator.builder.*;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class InjectableLibraryEvaluator implements org.opencds.cqf.cql.evaluator.library.LibraryEvaluator {

    protected FhirContext fhirContext;
    protected LibraryLoaderFactory libraryLoaderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected TerminologyProviderFactory terminologyProviderFactory;
    protected DataProviderConfigurer dataProviderConfigurer;
    protected DataProviderExtender dataProviderExtender;
    protected ParameterParser parameterParser;
    protected AdapterFactory adapterFactory;
    protected EndpointConverter endpointConverter;

    @Inject
    public InjectableLibraryEvaluator(FhirContext fhirContext, AdapterFactory adapterFactory, LibraryLoaderFactory libraryLoaderFactory,
            DataProviderFactory dataProviderFactory, TerminologyProviderFactory terminologyProviderFactory,
            DataProviderConfigurer dataProviderConfigurer, DataProviderExtender dataProviderExtender, ParameterParser parameterParser, EndpointConverter endpointConverter) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.libraryLoaderFactory = Objects.requireNonNull(libraryLoaderFactory,
                "libraryLoaderFactory can not be null");
        this.dataProviderFactory = Objects.requireNonNull(dataProviderFactory, "dataProviderFactory can not be null");
        this.terminologyProviderFactory = Objects.requireNonNull(terminologyProviderFactory,
                "terminologyProviderFactory can not be null");
        this.dataProviderConfigurer = Objects.requireNonNull(dataProviderConfigurer,
                "dataProviderConfigurer can not be null");
        this.dataProviderExtender = Objects.requireNonNull(dataProviderExtender,
                "dataProviderExtender can not be null");

        this.parameterParser = Objects.requireNonNull(parameterParser, "parameterParser can not be null");

        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory can not be null");

        this.endpointConverter = Objects.requireNonNull(endpointConverter, "endpointConverter can not be null");
    }

    /**
     * The function evaluates a FHIR library by Id and returns a Parameters resource
     * that contains the evaluation result
     * 
     * @param id                  the Id of the Library to evaluate
     * @param context             the context of the evaluation (e.g. "Patient",
     *                            "Unspecified")
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param periodStart         the "Measurement Period" start date, if applicable
     * @param periodEnd           the "Measurement Period" end date, if applicable
     * @param productLine         the "Product Line", if applicable
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param parameters          additional Parameters to set for the Library
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of Expressions in the Library to evaluate
     * @return IBaseParameters
     */
    @Override
    public IBaseParameters evaluate(IIdType id, String context, String patientId, String periodStart, String periodEnd,
            String productLine, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
            IBaseResource dataEndpoint, IBaseParameters parameters, IBaseBundle additionalData,
            Set<String> expressions) {

        return this.evaluate(new VersionedIdentifier().withId(id.getValue()), context, patientId, periodStart,
                periodEnd, productLine, libraryEndpoint, terminologyEndpoint, dataEndpoint, parameters, additionalData,
                expressions);

    }

    /**
     * The function evaluates a CQL / FHIR library by VersionedIdentifier and
     * returns a Parameters resource that contains the evaluation result
     * 
     * @param id                  the VersionedIdentifier of the Library to evaluate
     * @param context             the context of the evaluation (e.g. "Patient",
     *                            "Unspecified")
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param periodStart         the "Measurement Period" start date, if applicable
     * @param periodEnd           the "Measurement Period" end date, if applicable
     * @param productLine         the "Product Line", if applicable
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param parameters          additional Parameters to set for the Library
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of Expressions in the Library to evaluate
     * @return IBaseParameters
     */
    @Override
    public IBaseParameters evaluate(VersionedIdentifier id, String context, String patientId, String periodStart,
            String periodEnd, String productLine, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
            IBaseResource dataEndpoint, IBaseParameters parameters, IBaseBundle additionalData,
            Set<String> expressions) {

        LibraryLoader libraryLoader = this.resolveLibraryLoader(libraryEndpoint);
        TerminologyProvider terminologyProvider = this.resolveTerminologyProvider(terminologyEndpoint);
        Map<String, DataProvider> dataProviders = this.resolveDataProviders(dataEndpoint, terminologyProvider, additionalData);


        CqlEvaluator cqlEvaluator = new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider);
        LibraryProcessor libraryProcessor = new LibraryProcessor(this.fhirContext, this.adapterFactory, cqlEvaluator, libraryLoader, this.parameterParser);

        IBaseParameters resolvedParameters = this.mergeParameters(parameters, periodStart, periodEnd, productLine);
        Pair<String, Object> contextParameter = Pair.of(context, (Object)patientId);

        return libraryProcessor.evaluate(id, contextParameter, resolvedParameters, expressions);
    }


    protected LibraryLoader resolveLibraryLoader(IBaseResource libraryEndpoint) {
        return this.libraryLoaderFactory.create(
            endpointConverter.getEndpointInfo(libraryEndpoint), CqlTranslatorOptions.defaultOptions());
    }

    protected TerminologyProvider resolveTerminologyProvider(IBaseResource terminologyEndpoint) {
        return this.terminologyProviderFactory.create(endpointConverter.getEndpointInfo(terminologyEndpoint));
    }

    protected Map<String, DataProvider> resolveDataProviders(IBaseResource dataEndpoint, TerminologyProvider terminologyProvider, IBaseBundle additionalData) {
        Pair<String, DataProvider> dataProvider = this.dataProviderFactory
        .create(endpointConverter.getEndpointInfo(dataEndpoint));

        // TODO: extend with Bundle

        this.dataProviderConfigurer.configure(dataProvider.getRight(),
                new DataProviderConfig().setTerminologyProvider(terminologyProvider));

        Map<String, DataProvider> dataProviders = new HashMap<>();

        dataProviders.put(dataProvider.getLeft(), dataProvider.getRight());

        return dataProviders;
    }

    protected IBaseParameters mergeParameters(IBaseParameters parameters, String periodStart, String periodEnd, String productLine) {
        // TODO: Convert other parameters
        return parameters;
    }


    protected VersionedIdentifier getVersionedIdentifer(IIdType id, IBaseResource libraryEndpoint) {
        // TODO: read from disk or server...
        return null;
    }

}