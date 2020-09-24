package org.opencds.cqf.cql.evaluator.library.common;

import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class LibraryProcessor implements org.opencds.cqf.cql.evaluator.library.LibraryProcessor {

    protected FhirContext fhirContext;
    protected LibraryLoaderFactory libraryLoaderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected TerminologyProviderFactory terminologyProviderFactory;
    protected AdapterFactory adapterFactory;
    protected EndpointConverter endpointConverter;
    protected CqlEvaluatorBuilder cqlEvaluatorBuilder;

    @Inject
    public LibraryProcessor(FhirContext fhirContext, AdapterFactory adapterFactory,
            LibraryLoaderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
            TerminologyProviderFactory terminologyProviderFactory, EndpointConverter endpointConverter,
            CqlEvaluatorBuilder cqlEvaluatorBuilder) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.libraryLoaderFactory = Objects.requireNonNull(libraryLoaderFactory,
                "libraryLoaderFactory can not be null");
        this.dataProviderFactory = Objects.requireNonNull(dataProviderFactory, "dataProviderFactory can not be null");
        this.terminologyProviderFactory = Objects.requireNonNull(terminologyProviderFactory,
                "terminologyProviderFactory can not be null");

        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory can not be null");

        this.endpointConverter = Objects.requireNonNull(endpointConverter, "endpointConverter can not be null");
        this.cqlEvaluatorBuilder = Objects.requireNonNull(cqlEvaluatorBuilder, "cqlEvaluatorBuilder can not be null");
    }

    @Override
    public IBaseParameters evaluate(String url, String context, String patientId, String periodStart, String periodEnd,
            String productLine, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
            IBaseResource dataEndpoint, IBaseParameters parameters, IBaseBundle additionalData,
            Set<String> expressions) {
        
        return this.evaluate(this.getVersionedIdentifer(url, libraryEndpoint, additionalData), context, patientId, periodStart,
        periodEnd, productLine, libraryEndpoint, terminologyEndpoint, dataEndpoint, parameters, additionalData,
        expressions);
    }

    @Override
    public IBaseParameters evaluate(IIdType id, String context, String patientId, String periodStart, String periodEnd,
            String productLine, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
            IBaseResource dataEndpoint, IBaseParameters parameters, IBaseBundle additionalData,
            Set<String> expressions) {

        return this.evaluate(this.getVersionedIdentifer(id, libraryEndpoint, additionalData), context, patientId, periodStart,
                periodEnd, productLine, libraryEndpoint, terminologyEndpoint, dataEndpoint, parameters, additionalData,
                expressions);
    }

    @Override
    public IBaseParameters evaluate(VersionedIdentifier id, String context, String patientId, String periodStart,
            String periodEnd, String productLine, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
            IBaseResource dataEndpoint, IBaseParameters parameters, IBaseBundle additionalData,
            Set<String> expressions) {

        IBaseParameters resolvedParameters = this.mergeParameters(parameters, periodStart, periodEnd, productLine);
        this.addLibraryLoaders(libraryEndpoint, additionalData);
        this.addTerminologyProviders(terminologyEndpoint, additionalData);
        this.addDataProviders(dataEndpoint, additionalData);

        LibraryEvaluator libraryEvaluator = new LibraryEvaluator(this.fhirContext, this.adapterFactory,
                cqlEvaluatorBuilder.build());

        Pair<String, Object> contextParameter = Pair.of(context, (Object) patientId);
        return libraryEvaluator.evaluate(id, contextParameter, resolvedParameters, expressions);
    }

    protected void addLibraryLoaders(IBaseResource libraryEndpoint, IBaseBundle additionalData) {
        CqlTranslatorOptions translatorOptions = CqlTranslatorOptions.defaultOptions();
        if (libraryEndpoint != null) {
            LibraryLoader libraryLoader = this.libraryLoaderFactory.create(endpointConverter.getEndpointInfo(libraryEndpoint), translatorOptions);
            this.cqlEvaluatorBuilder.withLibraryLoader(libraryLoader);
        }

        if (additionalData != null) {
            this.cqlEvaluatorBuilder.withLibraryLoader(this.libraryLoaderFactory.create(additionalData, translatorOptions));
        }
    }

    protected void addTerminologyProviders(IBaseResource terminologyEndpoint, IBaseBundle additionalData) {
        if (terminologyEndpoint != null) {
            this.cqlEvaluatorBuilder.withTerminologyProvider(this.terminologyProviderFactory.create(endpointConverter.getEndpointInfo(terminologyEndpoint)));
        }

        if (additionalData != null) {
            this.cqlEvaluatorBuilder.withTerminologyProvider(this.terminologyProviderFactory.create(additionalData));
        }
    }

    protected void addDataProviders(IBaseResource dataEndpoint, IBaseBundle additionalData) {
        if (dataEndpoint != null) {
            Triple<String, ModelResolver, RetrieveProvider> dataProvider = this.dataProviderFactory
            .create(endpointConverter.getEndpointInfo(dataEndpoint));
            this.cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider);
        }

        if (additionalData != null) {
            Triple<String, ModelResolver, RetrieveProvider> dataProvider  = this.dataProviderFactory.create(additionalData);
            this.cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider);
        }
    }

    protected IBaseParameters mergeParameters(IBaseParameters parameters, String periodStart, String periodEnd,
            String productLine) {
        // TODO: Convert other parameters
        return parameters;
    }

    protected VersionedIdentifier getVersionedIdentifer(IIdType id, IBaseResource libraryEndpoint, IBaseBundle additionalData) {
        throw new NotImplementedException();
    }

    protected VersionedIdentifier getVersionedIdentifer(String url, IBaseResource libraryEndpoint, IBaseBundle additionalData) {
        throw new NotImplementedException();
    }
}