package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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

import ca.uhn.fhir.context.FhirContext;

@Named
public class LibraryProcessor {

    // private static Logger logger =
    // LoggerFactory.getLogger(LibraryProcessor.class);

    protected FhirContext fhirContext;
    protected CqlFhirParametersConverter cqlFhirParametersConverter;
    protected LibraryLoaderFactory libraryLoaderFactory;
    protected DataProviderFactory dataProviderFactory;
    protected TerminologyProviderFactory terminologyProviderFactory;
    protected EndpointConverter endpointConverter;
    protected CqlEvaluatorBuilder cqlEvaluatorBuilder;

    @Inject
    public LibraryProcessor(FhirContext fhirContext, CqlFhirParametersConverter cqlFhirParametersConverter,
            LibraryLoaderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
            TerminologyProviderFactory terminologyProviderFactory, EndpointConverter endpointConverter,
            CqlEvaluatorBuilder cqlEvaluatorBuilder) {

        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.cqlFhirParametersConverter = requireNonNull(cqlFhirParametersConverter, "cqlFhirParametersConverter");
        this.libraryLoaderFactory = requireNonNull(libraryLoaderFactory, "libraryLoaderFactory can not be null");
        this.dataProviderFactory = requireNonNull(dataProviderFactory, "dataProviderFactory can not be null");
        this.terminologyProviderFactory = requireNonNull(terminologyProviderFactory,
                "terminologyProviderFactory can not be null");

        this.endpointConverter = requireNonNull(endpointConverter, "endpointConverter can not be null");
        this.cqlEvaluatorBuilder = requireNonNull(cqlEvaluatorBuilder, "cqlEvaluatorBuilder can not be null");
    }

    /**
     * The function evaluates a FHIR library by the Canonical Url and returns a
     * Parameters resource that contains the evaluation result
     * 
     * @param url                 the url of the Library to evaluate
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param parameters          additional Parameters to set for the Library
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of expressions in the Library to evaluate.
     *                            if omitted all expressions are evaluated.
     * @return IBaseParameters
     */
    public IBaseParameters evaluate(String url, String patientId, IBaseParameters parameters,
            IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
            IBaseBundle additionalData, Set<String> expressions) {

        return this.evaluate(this.getVersionedIdentifer(url, libraryEndpoint, additionalData), patientId, parameters,
                libraryEndpoint, terminologyEndpoint, dataEndpoint, additionalData, expressions);
    }

    /**
     * The function evaluates a FHIR library by Id and returns a Parameters resource
     * that contains the evaluation result
     * 
     * @param id                  the Id of the Library to evaluate
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param parameters          additional Parameters to set for the Library
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of expressions in the Library to evaluate.
     *                            if omitted all expressions are evaluated.
     * @return IBaseParameters
     */
    public IBaseParameters evaluate(IIdType id, String patientId, IBaseParameters parameters,
            IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
            IBaseBundle additionalData, Set<String> expressions) {

        return this.evaluate(this.getVersionedIdentifer(id, libraryEndpoint, additionalData), patientId, parameters,
                libraryEndpoint, terminologyEndpoint, dataEndpoint, additionalData, expressions);
    }

    /**
     * The function evaluates a CQL / FHIR library by VersionedIdentifier and
     * returns a Parameters resource that contains the evaluation result
     * 
     * @param id                  the VersionedIdentifier of the Library to evaluate
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param parameters          additional Parameters to set for the Library
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of expressions in the Library to evaluate.
     *                            if omitted all expressions are evaluated.
     * @return IBaseParameters
     */
    public IBaseParameters evaluate(VersionedIdentifier id, String patientId, IBaseParameters parameters,
            IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
            IBaseBundle additionalData, Set<String> expressions) {

        this.addLibraryLoaders(libraryEndpoint, additionalData);
        this.addTerminologyProviders(terminologyEndpoint, additionalData);
        this.addDataProviders(dataEndpoint, additionalData);

        LibraryEvaluator libraryEvaluator = new LibraryEvaluator(this.cqlFhirParametersConverter,
                cqlEvaluatorBuilder.build());

        Pair<String, Object> contextParameter = null;
        if (patientId != null) {
            contextParameter = Pair.of("Patient", (Object) patientId);
        }

        return libraryEvaluator.evaluate(id, contextParameter, parameters, expressions);
    }

    protected void addLibraryLoaders(IBaseResource libraryEndpoint, IBaseBundle additionalData) {
        CqlTranslatorOptions translatorOptions = CqlTranslatorOptions.defaultOptions();
        if (libraryEndpoint != null) {
            LibraryLoader libraryLoader = this.libraryLoaderFactory
                    .create(endpointConverter.getEndpointInfo(libraryEndpoint), translatorOptions);
            this.cqlEvaluatorBuilder.withLibraryLoader(libraryLoader);
        }

        if (additionalData != null) {
            this.cqlEvaluatorBuilder
                    .withLibraryLoader(this.libraryLoaderFactory.create(additionalData, translatorOptions));
        }
    }

    protected void addTerminologyProviders(IBaseResource terminologyEndpoint, IBaseBundle additionalData) {
        if (terminologyEndpoint != null) {
            this.cqlEvaluatorBuilder.withTerminologyProvider(
                    this.terminologyProviderFactory.create(endpointConverter.getEndpointInfo(terminologyEndpoint)));
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
            Triple<String, ModelResolver, RetrieveProvider> dataProvider = this.dataProviderFactory
                    .create(additionalData);
            this.cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider);
        }
    }

    protected VersionedIdentifier getVersionedIdentifer(IIdType id, IBaseResource libraryEndpoint,
            IBaseBundle additionalData) {
        throw new NotImplementedException();
    }

    protected VersionedIdentifier getVersionedIdentifer(String url, IBaseResource libraryEndpoint,
            IBaseBundle additionalData) {
        if (!url.contains("/Library/")) {
            throw new IllegalArgumentException("Invalid resource type for determining library version identifier: Library");
        }
        String [] urlSplit = url.split("/Library/");
        if (urlSplit.length != 2) {
            throw new IllegalArgumentException("Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
        }

        @SuppressWarnings("unused") 
        String cqlNamespaceUrl = urlSplit[0];

        String cqlName = urlSplit[1];
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        if (cqlName.contains("|")) {
            String[] nameVersion = cqlName.split("\\|");
            String name = nameVersion[0];
            String version = nameVersion[1];
            versionedIdentifier.setId(name);
            versionedIdentifier.setVersion(version);
        } else {
            versionedIdentifier.setId(cqlName);
        }
        return versionedIdentifier;
    }
}