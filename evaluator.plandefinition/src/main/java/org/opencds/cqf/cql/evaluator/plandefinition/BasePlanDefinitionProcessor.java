package org.opencds.cqf.cql.evaluator.plandefinition;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
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
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.Contexts;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BasePlanDefinitionProcessor<T> {
  private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
  protected static final String ALT_EXPRESSION_EXT =
      "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";
  protected static final String REQUEST_GROUP_EXT =
      "http://fl7.org/fhir/StructureDefinition/RequestGroup-Goal";

  protected static final String REQUEST_ORCHESTRATION_EXT =
      "http://fl7.org/fhir/StructureDefinition/RequestOrchestration-Goal";

  protected static final String subjectType = "Patient";

  protected final OperationParametersParser operationParametersParser;
  protected final ModelResolver modelResolver;
  protected final IFhirPath fhirPath;
  protected Repository repository;
  protected LibraryEngine libraryEngine;
  protected ExpressionEvaluator expressionEvaluator;

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

  protected BasePlanDefinitionProcessor(Repository repository) {
    requireNonNull(repository, "localRepository can not be null");
    this.repository = repository;
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

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolvePlanDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource thePlanDefinition);

  public abstract IBaseResource applyPlanDefinition(T planDefinition);

  public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);

  public abstract IBaseResource transformToBundle(IBaseResource requestGroup);

  public abstract void resolveCdsHooksDynamicValue(IBaseResource requestGroup, Object value,
      String path);

  public abstract void extractQuestionnaireResponse();

  public abstract IBaseBundle packagePlanDefinition(T thePlanDefinition, boolean theIsPut);

  public IBaseBundle packagePlanDefinition(T thePlanDefinition) {
    return packagePlanDefinition(thePlanDefinition, false);
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packagePlanDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource thePlanDefinition) {
    return packagePlanDefinition(resolvePlanDefinition(theId, theCanonical, thePlanDefinition));
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
        parameters, useServerData, bundle, prefetchData, new LibraryEngine(repository));
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
        parameters, useServerData, bundle, prefetchData, new LibraryEngine(repository));
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

  public void validateExpressionWithPath(String language, String expression, String path,
      String libraryUrl) {
    this.libraryEngine.validateExpression(language, expression, libraryUrl);
    if (path == null) {
      logger.error("Missing element path for the Expression");
      throw new IllegalArgumentException("Missing element path for the Expression");
    }
  }

  public void resolveDynamicValue(String language, String expression, String path,
      String altLanguage, String altExpression, String altPath, String libraryUrl,
      IBaseResource resource, IBaseParameters params) {
    validateExpressionWithPath(language, expression, path, libraryUrl);
    var result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, expression,
        language, libraryUrl, params, this.bundle);
    if (result == null && altExpression != null) {
      validateExpressionWithPath(altLanguage, altExpression, altPath, libraryUrl);
      path = altPath;
      result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, altExpression,
          altLanguage, libraryUrl, params, this.bundle);
    }
    if (result != null && result.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "Dynamic value resolution received multiple values for expression: %s", expression));
    }
    var value = result == null || result.isEmpty() ? null : result.get(0);
    if (path.startsWith("action.")) {
      resolveCdsHooksDynamicValue(resource, value, path);
    }
    // backwards compatibility where CDS Hooks indicator was set with activity.extension or
    // action.extension path
    else if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
      if (repository.fhirContext().getVersion().getVersion() == FhirVersionEnum.R5) {
        throw new IllegalArgumentException(
            "Please use the priority path when setting indicator values when using FHIR R5 for CDS Hooks evaluation");
      }
      resolveCdsHooksDynamicValue(resource, value, path);
    } else {
      modelResolver.setValue(resource, path, value);
    }
  }

  public IBase resolveCondition(String language, String expression, String altLanguage,
      String altExpression, String libraryUrl, IBaseParameters params) {
    this.libraryEngine.validateExpression(language, expression, libraryUrl);
    var result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, expression,
        language, libraryUrl, params, this.bundle);
    if (result == null && altExpression != null) {
      this.libraryEngine.validateExpression(altLanguage, altExpression, libraryUrl);
      result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, altExpression,
          altLanguage, libraryUrl, params, this.bundle);
    }
    if (result != null && result.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "Dynamic value resolution received multiple values for expression: %s", expression));
    }

    return result == null || result.isEmpty() ? null : result.get(0);
  }
}
