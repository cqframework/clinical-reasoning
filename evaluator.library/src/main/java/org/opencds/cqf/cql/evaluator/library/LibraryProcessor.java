package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

@SuppressWarnings({"unused", "squid:S107"})
@Named
public class LibraryProcessor {

  private static final Logger logger = LoggerFactory.getLogger(LibraryProcessor.class);

  protected FhirContext fhirContext;
  protected CqlFhirParametersConverter cqlFhirParametersConverter;
  protected LibrarySourceProviderFactory librarySourceProviderFactory;
  protected DataProviderFactory dataProviderFactory;
  protected TerminologyProviderFactory terminologyProviderFactory;
  protected EndpointConverter endpointConverter;
  protected CqlEvaluatorBuilder cqlEvaluatorBuilder;
  protected ModelResolverFactory fhirModelResolverFactory;
  protected Supplier<CqlEvaluatorBuilder> cqlEvaluatorBuilderSupplier;

  @Inject
  public LibraryProcessor(FhirContext fhirContext,
      CqlFhirParametersConverter cqlFhirParametersConverter,
      LibrarySourceProviderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
      TerminologyProviderFactory terminologyProviderFactory, EndpointConverter endpointConverter,
      ModelResolverFactory fhirModelResolverFactory,
      Supplier<CqlEvaluatorBuilder> cqlEvaluatorBuilderSupplier) {

    this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
    this.cqlFhirParametersConverter =
        requireNonNull(cqlFhirParametersConverter, "cqlFhirParametersConverter");
    this.librarySourceProviderFactory =
        requireNonNull(libraryLoaderFactory, "libraryLoaderFactory can not be null");
    this.dataProviderFactory =
        requireNonNull(dataProviderFactory, "dataProviderFactory can not be null");
    this.terminologyProviderFactory =
        requireNonNull(terminologyProviderFactory, "terminologyProviderFactory can not be null");

    this.endpointConverter = requireNonNull(endpointConverter, "endpointConverter can not be null");
    this.cqlEvaluatorBuilderSupplier =
        requireNonNull(cqlEvaluatorBuilderSupplier, "cqlEvaluatorBuilder can not be null");
    this.fhirModelResolverFactory =
        requireNonNull(fhirModelResolverFactory, "fhirModelResolverFactory can not be null");

    if (!this.fhirModelResolverFactory.getModelUri().equals(Constants.FHIR_MODEL_URI)) {
      throw new IllegalArgumentException(
          "fhirModelResolverFactory was not a FHIR modelResolverFactory");
    }
  }

  /**
   * The function evaluates a FHIR library by the Canonical Url and returns a Parameters resource
   * that contains the evaluation result
   *
   * @param url the url of the Library to evaluate
   * @param patientId the patient Id to use for evaluation, if applicable
   * @param parameters additional Parameters to set for the Library
   * @param libraryEndpoint the Endpoint to use for loading Library resources, if applicable
   * @param terminologyEndpoint the Endpoint to use for Terminology operations, if applicable
   * @param dataEndpoint the Endpoint to use for data, if applicable
   * @param additionalData additional data to use during evaluation
   * @param expressions names of expressions in the Library to evaluate. if omitted all expressions
   *        are evaluated.
   * @return IBaseParameters
   */
  public IBaseParameters evaluate(String url, String patientId, IBaseParameters parameters,
      IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
      IBaseBundle additionalData, Set<String> expressions) {

    return this.evaluate(this.getVersionedIdentifier(url), patientId, parameters, libraryEndpoint,
        terminologyEndpoint, dataEndpoint, additionalData, expressions);
  }

  /**
   * The function evaluates a FHIR library by Id and returns a Parameters resource that contains the
   * evaluation result
   *
   * @param id the Id of the Library to evaluate
   * @param patientId the patient Id to use for evaluation, if applicable
   * @param parameters additional Parameters to set for the Library
   * @param libraryEndpoint the Endpoint to use for loading Library resources, if applicable
   * @param terminologyEndpoint the Endpoint to use for Terminology operations, if applicable
   * @param dataEndpoint the Endpoint to use for data, if applicable
   * @param additionalData additional data to use during evaluation
   * @param expressions names of expressions in the Library to evaluate. if omitted all expressions
   *        are evaluated.
   * @return IBaseParameters
   */
  public IBaseParameters evaluate(IIdType id, String patientId, IBaseParameters parameters,
      IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint, IBaseResource dataEndpoint,
      IBaseBundle additionalData, Set<String> expressions) {

    return this.evaluate(this.getVersionedIdentifier(id, libraryEndpoint, additionalData),
        patientId, parameters, libraryEndpoint, terminologyEndpoint, dataEndpoint, additionalData,
        expressions);
  }

  /**
   * The function evaluates a CQL / FHIR library by VersionedIdentifier and returns a Parameters
   * resource that contains the evaluation result
   *
   * @param id the VersionedIdentifier of the Library to evaluate
   * @param patientId the patient Id to use for evaluation, if applicable
   * @param parameters additional Parameters to set for the Library
   * @param libraryEndpoint the Endpoint to use for loading Library resources, if applicable
   * @param terminologyEndpoint the Endpoint to use for Terminology operations, if applicable
   * @param dataEndpoint the Endpoint to use for data, if applicable
   * @param additionalData additional data to use during evaluation
   * @param expressions names of expressions in the Library to evaluate. if omitted all expressions
   *        are evaluated.
   * @return IBaseParameters
   */
  public IBaseParameters evaluate(VersionedIdentifier id, String patientId,
      IBaseParameters parameters, IBaseResource libraryEndpoint, IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint, IBaseBundle additionalData, Set<String> expressions) {

    this.cqlEvaluatorBuilder = this.cqlEvaluatorBuilderSupplier.get();

    this.addLibrarySourceProviders(libraryEndpoint, additionalData);
    this.addTerminologyProviders(terminologyEndpoint, additionalData);
    this.addDataProviders(dataEndpoint, additionalData);

    LibraryEvaluator libraryEvaluator =
        new LibraryEvaluator(this.cqlFhirParametersConverter, cqlEvaluatorBuilder.build());

    Pair<String, Object> contextParameter = null;
    if (patientId != null) {
      if (patientId.startsWith("Patient/")) {
        patientId = patientId.replace("Patient/", "");
      }
      contextParameter = Pair.of("Patient", patientId);
    }

    return libraryEvaluator.evaluate(id, contextParameter, parameters, expressions);
  }

  protected void addLibrarySourceProviders(IBaseResource libraryEndpoint,
      IBaseBundle additionalData) {
    if (libraryEndpoint != null) {
      LibrarySourceProvider librarySourceProvider = this.librarySourceProviderFactory
          .create(endpointConverter.getEndpointInfo(libraryEndpoint));
      this.cqlEvaluatorBuilder.withLibrarySourceProvider(librarySourceProvider);
    }

    if (additionalData != null) {
      this.cqlEvaluatorBuilder
          .withLibrarySourceProvider(this.librarySourceProviderFactory.create(additionalData));
    }
  }

  protected void addTerminologyProviders(IBaseResource terminologyEndpoint,
      IBaseBundle additionalData) {
    if (terminologyEndpoint != null) {
      this.cqlEvaluatorBuilder.withTerminologyProvider(this.terminologyProviderFactory
          .create(endpointConverter.getEndpointInfo(terminologyEndpoint)));
    }

    if (additionalData != null) {
      this.cqlEvaluatorBuilder
          .withTerminologyProvider(this.terminologyProviderFactory.create(additionalData));
    }
  }

  protected void addDataProviders(IBaseResource dataEndpoint, IBaseBundle additionalData) {
    if (dataEndpoint != null) {
      DataProviderComponents dataProvider =
          this.dataProviderFactory.create(endpointConverter.getEndpointInfo(dataEndpoint));
      this.cqlEvaluatorBuilder.withDataProviderComponents(dataProvider);
    }

    if (additionalData != null) {
      DataProviderComponents dataProvider = this.dataProviderFactory.create(additionalData);
      this.cqlEvaluatorBuilder.withDataProviderComponents(dataProvider);
    }

    if (additionalData == null && dataEndpoint == null) {
      // Set up a FHIR resolver in the event we don't have any data
      ModelResolver modelResolver = this.fhirModelResolverFactory
          .create(fhirContext.getVersion().getVersion().getFhirVersionString());
      this.cqlEvaluatorBuilder.withModelResolver(Constants.FHIR_MODEL_URI, modelResolver);
    }
  }

  protected VersionedIdentifier getVersionedIdentifier(IIdType id, IBaseResource libraryEndpoint,
      IBaseBundle additionalData) {
    throw new NotImplementedException();
  }

  protected VersionedIdentifier getVersionedIdentifier(String url) {
    if (!url.contains("/Library/")) {
      throw new IllegalArgumentException(
          "Invalid resource type for determining library version identifier: Library");
    }
    String[] urlSplit = url.split("/Library/");
    if (urlSplit.length != 2) {
      throw new IllegalArgumentException(
          "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
    }

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
