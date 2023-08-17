package org.opencds.cqf.cql.evaluator.plandefinition;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.Contexts;
import org.opencds.cqf.cql.evaluator.library.CqfExpression;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IElement;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BasePlanDefinitionProcessor<T> {
  private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
  protected static final String subjectType = "Patient";

  protected final OperationParametersParser operationParametersParser;
  protected final ModelResolver modelResolver;
  protected final IFhirPath fhirPath;
  protected Repository repository;
  protected LibraryEngine libraryEngine;

  protected String patientId;
  protected String encounterId;
  protected String practitionerId;
  protected String organizationId;
  protected String userType;
  protected String userLanguage;
  protected String userTaskContext;
  protected String setting;
  protected String settingContext;
  protected IBaseParameters parameters;
  protected Boolean useServerData;
  protected IBaseBundle bundle;
  protected IBaseParameters prefetchData;
  protected Boolean containResources;
  protected IBaseResource questionnaire;
  protected Collection<IBaseResource> requestResources;
  protected Collection<IBaseResource> extractedResources;
  protected EvaluationSettings evaluationSettings;

  protected BasePlanDefinitionProcessor(Repository repository,
      EvaluationSettings evaluationSettings) {
    this.repository = requireNonNull(repository, "repository can not be null");
    this.evaluationSettings =
        requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    this.fhirPath = FhirPathCache.cachedForContext(repository.fhirContext());
    this.operationParametersParser = new OperationParametersParser(
        Contexts.getAdapterFactory(repository.fhirContext()),
        new FhirTypeConverterFactory().create(repository.fhirContext().getVersion().getVersion()));
    this.modelResolver = new FhirModelResolverFactory()
        .create(repository.fhirContext().getVersion().getVersion().getFhirVersionString());
  }

  public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type,
      String errorMessage) {
    if (obj == null)
      return Optional.empty();
    if (type.isInstance(obj)) {
      return Optional.of(type.cast(obj));
    }
    throw new IllegalArgumentException(errorMessage);
  }

  public FhirContext fhirContext() {
    return this.repository.fhirContext();
  }

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolvePlanDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource thePlanDefinition);

  public abstract IBaseResource applyPlanDefinition(T planDefinition);

  public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);

  public abstract IBaseResource transformToBundle(IBaseResource requestGroup);

  public abstract void resolveDynamicExtension(IElement requestAction, IBase resource, Object value,
      String path);

  public abstract void extractQuestionnaireResponse();

  public abstract IBaseBundle packagePlanDefinition(T thePlanDefinition, boolean theIsPut);

  public IBaseBundle packagePlanDefinition(T thePlanDefinition) {
    return packagePlanDefinition(thePlanDefinition, false);
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packagePlanDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource thePlanDefinition,
      boolean theIsPut) {
    return packagePlanDefinition(resolvePlanDefinition(theId, theCanonical, thePlanDefinition),
        theIsPut);
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType theId,
      CanonicalType theCanonical, IBaseResource thePlanDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return apply(theId, theCanonical, thePlanDefinition, patientId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, useServerData, bundle, prefetchData,
        new LibraryEngine(repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType theId,
      CanonicalType theCanonical, IBaseResource thePlanDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, LibraryEngine libraryEngine) {
    if (repository.fhirContext().getVersion().getVersion() == FhirVersionEnum.R5) {
      return applyR5(theId, theCanonical, thePlanDefinition, patientId, encounterId, practitionerId,
          organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
          parameters, useServerData, bundle, prefetchData, libraryEngine);
    }
    this.patientId = patientId;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.userType = userType;
    this.userLanguage = userLanguage;
    this.userTaskContext = userTaskContext;
    this.setting = setting;
    this.settingContext = settingContext;
    this.parameters = parameters;
    this.useServerData = useServerData;
    this.bundle = bundle;
    this.prefetchData = prefetchData;
    this.libraryEngine = libraryEngine;
    this.containResources = true;
    this.requestResources = new ArrayList<>();
    this.extractedResources = new ArrayList<>();
    extractQuestionnaireResponse();
    return transformToCarePlan(
        applyPlanDefinition(resolvePlanDefinition(theId, theCanonical, thePlanDefinition)));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource applyR5(IIdType theId,
      CanonicalType theCanonical, IBaseResource thePlanDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return applyR5(theId, theCanonical, thePlanDefinition, patientId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, useServerData, bundle, prefetchData,
        new LibraryEngine(repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource applyR5(IIdType theId,
      CanonicalType theCanonical, IBaseResource thePlanDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, LibraryEngine libraryEngine) {
    this.patientId = patientId;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.userType = userType;
    this.userLanguage = userLanguage;
    this.userTaskContext = userTaskContext;
    this.setting = setting;
    this.settingContext = settingContext;
    this.parameters = parameters;
    this.useServerData = useServerData;
    this.bundle = bundle;
    this.prefetchData = prefetchData;
    this.libraryEngine = libraryEngine;
    this.containResources = false;
    this.requestResources = new ArrayList<>();
    this.extractedResources = new ArrayList<>();
    extractQuestionnaireResponse();
    return transformToBundle(
        applyPlanDefinition(resolvePlanDefinition(theId, theCanonical, thePlanDefinition)));
  }

  public List<IBase> resolveExpression(CqfExpression expression, IBaseParameters params) {
    var result = this.libraryEngine.getExpressionResult(this.patientId, subjectType,
        expression.getExpression(), expression.getLanguage(), expression.getLibraryUrl(), params,
        this.bundle);
    if (result == null && expression.getAltExpression() != null) {
      result = this.libraryEngine.getExpressionResult(this.patientId, subjectType,
          expression.getAltExpression(),
          expression.getAltLanguage(), expression.getAltLibraryUrl(), params, this.bundle);
    }

    return result;
  }

  public void resolveDynamicValue(List<IBase> result, String path,
      IElement requestAction, IBase resource) {
    if (result == null || result.isEmpty()) {
      return;
    }

    var value = result.size() == 1 ? result.get(0) : result;
    if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
      if (repository.fhirContext().getVersion().getVersion() == FhirVersionEnum.R5) {
        throw new IllegalArgumentException(
            "Please use the priority path when setting indicator values when using FHIR R5 for CDS Hooks evaluation");
      }
      resolveDynamicExtension(requestAction, resource, value, path);
    } else if (path.startsWith("action.") || resource == null) {
      modelResolver.setValue(requestAction, path.replace("action.", ""), value);
    } else {
      modelResolver.setValue(resource, path, value);
    }
  }
}
