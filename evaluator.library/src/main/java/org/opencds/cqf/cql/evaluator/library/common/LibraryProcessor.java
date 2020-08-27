package org.opencds.cqf.cql.evaluator.library.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
import org.opencds.cqf.cql.evaluator.builder.api.*;
import org.opencds.cqf.cql.evaluator.builder.api.model.DataProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.util.EndpointUtil;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.fhir.api.LibraryAdapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

public class LibraryProcessor implements org.opencds.cqf.cql.evaluator.library.api.LibraryProcessor {

    protected FhirContext fhirContext;
    protected LibraryLoaderFactory libraryLoaderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected TerminologyProviderFactory terminologyProviderFactory;
    protected DataProviderConfigurer dataProviderConfigurer;
    protected DataProviderExtender dataProviderExtender;

    @Inject
    public LibraryProcessor(FhirContext fhirContext, LibraryLoaderFactory libraryLoaderFactory,
            DataProviderFactory dataProviderFactory, TerminologyProviderFactory terminologyProviderFactory,
            DataProviderConfigurer dataProviderConfigurer, DataProviderExtender dataProviderExtender) {
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
    }

    protected LibraryLoader defaultLibraryLoader;

    public LibraryLoader getDefaultLibraryLoader() {
        return this.defaultLibraryLoader;
    }

    public void setDefaultLibraryLoader(LibraryLoader value) {
        this.defaultLibraryLoader = value;
    }

    protected DataProvider defaultDataProvider;

    public DataProvider getDefaultDataProvider() {
        return this.defaultDataProvider;
    }

    public void setDefaultDataProvider(DataProvider value) {
        this.defaultDataProvider = value;
    }

    protected TerminologyProvider defaultTerminologyProvider;

    public TerminologyProvider getDefaultTerminologyProvider() {
        return this.defaultTerminologyProvider;
    }

    public void setDefaultTerminologyProvider(TerminologyProvider value) {
        this.defaultTerminologyProvider = value;
    }

    protected Function<IIdType, IBaseResource> defaultLibraryLocator;

    public Function<IIdType, IBaseResource> getDefaultLibraryLocator() {
        return this.defaultLibraryLocator;
    }

    public void setDefaultLibraryLocator(Function<IIdType, IBaseResource> value) {
        this.defaultLibraryLocator = value;
    }

    @Override
    public IBaseParameters evaluate(IIdType id, String context, String patientId, String periodStart, String periodEnd,
            String productLine, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
            IBaseParameters parameters, IBaseBundle additionalData, Set<String> expressions) {

        LibraryLoader libraryLoader = this.libraryLoaderFactory.create(
                Collections.singletonList(new BundleLibrarySourceProvider(fhirContext, additionalData)),
                CqlTranslatorOptions.defaultOptions());

        TerminologyProvider terminologyProvider = this.terminologyProviderFactory
                .create(EndpointUtil.getEndpointInfo(fhirContext, terminologyEndpoint));

        Pair<String, DataProvider> dataProvider = this.dataProviderFactory
                .create(EndpointUtil.getEndpointInfo(fhirContext, dataEndpoint));

        // TODO: extend with Bundle

        this.dataProviderConfigurer.configure(dataProvider.getRight(),
                new DataProviderConfig().setTerminologyProvider(terminologyProvider));

        Map<String, DataProvider> dataProviders = new HashMap<>();

        dataProviders.put(dataProvider.getLeft(), dataProvider.getRight());

        Pair<String, Object> contextParameter = Pair.of(context, (Object) patientId);

        // TODO: Convert extra params..

        CqlEvaluator cqlEvaluator = new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider);

        LibraryEvaluator libraryEvaluator = new LibraryEvaluator(this.fhirContext, cqlEvaluator);

        List<? extends IBaseResource> libraries = BundleUtil.toListOfResourcesOfType(this.fhirContext, additionalData,
                this.fhirContext.getResourceDefinition("Library").getImplementingClass());

        VersionedIdentifier libraryIdentifier = null;
        for (IBaseResource resource : libraries) {
            LibraryAdapter libraryAdapter = org.opencds.cqf.cql.evaluator.fhir.common.AdapterFactory
                    .libraryAdapterFor(resource);

            if (libraryAdapter.getId(resource).equals(id)) {
                libraryIdentifier = new VersionedIdentifier().withId(libraryAdapter.getName(resource))
                        .withVersion(libraryAdapter.getVersion(resource));
                break;
            }
        }

        return libraryEvaluator.evaluate(libraryIdentifier, contextParameter, parameters, expressions);
    }

}