package org.opencds.cqf.cql.evaluator.plandefinition;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.helper.NestedValueResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.Contexts;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BasePlanDefinitionProcessor<T> {
  private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
  protected static final List<String> EXCLUDED_EXTENSION_LIST = Arrays
      .asList(Constants.CPG_KNOWLEDGE_CAPABILITY, Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL,
          Constants.CQFM_SOFTWARE_SYSTEM, Constants.CPG_QUESTIONNAIRE_GENERATE,
          Constants.CQFM_LOGIC_DEFINITION, Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS);

  protected final OperationParametersParser operationParametersParser;
  protected final ModelResolver modelResolver;
  protected final NestedValueResolver nestedValueResolver;
  protected Repository repository;
  protected LibraryEngine libraryEngine;
  protected Repository federatedRepository;
  protected ExpressionEvaluator expressionEvaluator;
  protected EvaluationSettings evaluationSettings;

  protected String subjectId;
  protected String encounterId;
  protected String practitionerId;
  protected String organizationId;
  protected IBaseDatatype userType;
  protected IBaseDatatype userLanguage;
  protected IBaseDatatype userTaskContext;
  protected IBaseDatatype setting;
  protected IBaseDatatype settingContext;
  protected IBaseParameters parameters;
  protected Boolean useServerData;
  protected IBaseBundle bundle;
  protected IBaseParameters prefetchData;
  protected Boolean containResources;
  protected IBaseResource questionnaire;
  protected Collection<IBaseResource> requestResources;
  protected Collection<IBaseResource> extractedResources;

  protected BasePlanDefinitionProcessor(Repository repository,
      EvaluationSettings evaluationSettings) {
    this.repository = requireNonNull(repository, "repository can not be null");
    this.evaluationSettings =
        requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    this.operationParametersParser = new OperationParametersParser(
        Contexts.getAdapterFactory(fhirContext()),
        new FhirTypeConverterFactory().create(fhirVersion()));
    this.modelResolver = new FhirModelResolverFactory()
        .create(fhirVersion().getFhirVersionString());
    this.nestedValueResolver = new NestedValueResolver(fhirContext(), modelResolver);
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

  public FhirVersionEnum fhirVersion() {
    return this.fhirContext().getVersion().getVersion();
  }

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolvePlanDefinition(
      IIdType id, CanonicalType canonical, IBaseResource planDefinition);

  public abstract T initApply(T planDefinition);

  public abstract IBaseResource applyPlanDefinition(T planDefinition);

  public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);

  public abstract IBaseResource transformToBundle(IBaseResource requestGroup);

  protected abstract void resolveDynamicExtension(IElement requestAction, IBase resource,
      Object value, String path);

  protected abstract void extractQuestionnaireResponse();

  protected abstract void addOperationOutcomeIssue(String issue);

  public abstract IBaseBundle packagePlanDefinition(T planDefinition, boolean isPut);

  public IBaseBundle packagePlanDefinition(T planDefinition) {
    return packagePlanDefinition(planDefinition, false);
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packagePlanDefinition(
      IIdType id, CanonicalType canonical, IBaseResource planDefinition,
      boolean isPut) {
    return packagePlanDefinition(resolvePlanDefinition(id, canonical, planDefinition),
        isPut);
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType id,
      CanonicalType canonical, IBaseResource planDefinition, String subject,
      String encounterId, String practitionerId, String organizationId, IBaseDatatype userType,
      IBaseDatatype userLanguage, IBaseDatatype userTaskContext, IBaseDatatype setting,
      IBaseDatatype settingContext) {
    return apply(id, canonical, planDefinition, subjectId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        null, true, null, null,
        new LibraryEngine(repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType id,
      CanonicalType canonical, IBaseResource planDefinition, String subject,
      String encounterId, String practitionerId, String organizationId, IBaseDatatype userType,
      IBaseDatatype userLanguage, IBaseDatatype userTaskContext, IBaseDatatype setting,
      IBaseDatatype settingContext, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return apply(id, canonical, planDefinition, subject, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, useServerData, bundle, prefetchData,
        new LibraryEngine(repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType id,
      CanonicalType canonical, IBaseResource planDefinition, String subject,
      String encounterId, String practitionerId, String organizationId, IBaseDatatype userType,
      IBaseDatatype userLanguage, IBaseDatatype userTaskContext, IBaseDatatype setting,
      IBaseDatatype settingContext, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, LibraryEngine libraryEngine) {
    if (fhirVersion() == FhirVersionEnum.R5) {
      return applyR5(id, canonical, planDefinition, subject, encounterId, practitionerId,
          organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
          parameters, useServerData, bundle, prefetchData, libraryEngine);
    }
    this.subjectId = subject;
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
    return transformToCarePlan(
        applyPlanDefinition(initApply(resolvePlanDefinition(id, canonical, planDefinition))));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource applyR5(IIdType id,
      CanonicalType canonical, IBaseResource planDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, IBaseDatatype userType,
      IBaseDatatype userLanguage, IBaseDatatype userTaskContext, IBaseDatatype setting,
      IBaseDatatype settingContext, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return applyR5(id, canonical, planDefinition, patientId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, useServerData, bundle, prefetchData,
        new LibraryEngine(repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource applyR5(IIdType id,
      CanonicalType canonical, IBaseResource planDefinition, String patientId,
      String encounterId, String practitionerId, String organizationId, IBaseDatatype userType,
      IBaseDatatype userLanguage, IBaseDatatype userTaskContext, IBaseDatatype setting,
      IBaseDatatype settingContext, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, LibraryEngine libraryEngine) {
    this.subjectId = patientId;
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
    return transformToBundle(
        applyPlanDefinition(initApply(resolvePlanDefinition(id, canonical, planDefinition))));
  }

  protected void resolveDynamicValue(List<IBase> result, String path,
      IElement requestAction, IBase resource) {
    if (result == null || result.isEmpty()) {
      return;
    }

    // Strip % so it is supported as defined in the spec
    path = path.replace("%", "");
    var value = result.size() == 1 ? result.get(0) : result;
    if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
      if (repository.fhirContext().getVersion().getVersion() == FhirVersionEnum.R5) {
        throw new IllegalArgumentException(
            "Please use the priority path when setting indicator values when using FHIR R5 for CDS Hooks evaluation");
      }
      resolveDynamicExtension(requestAction, resource, value, path);
    } else if (path.startsWith("action.") || resource == null) {
      modelResolver.setValue(requestAction, path.replace("action.", ""), value);
    } else if (path.contains(".")) {
      nestedValueResolver.setNestedValue(resource, path, result.get(0));
    } else {
      modelResolver.setValue(resource, path, value);
    }
  }

  // @SuppressWarnings({"unchecked", "rawtypes"})
  // protected <E extends IBaseExtension> void resolveExtensions(List<E> extensions,
  // String defaultLibraryUrl) {
  // for (var extension : extensions) {
  // var nestedExtensions = extension.getExtension();
  // if (nestedExtensions != null && !nestedExtensions.isEmpty()) {
  // resolveExtensions(nestedExtensions, defaultLibraryUrl);
  // }
  // var value = extension.getValue();
  // if (value instanceof IBaseHasExtensions) {
  // var valueExtensions = ((IBaseHasExtensions) value).getExtension();
  // if (valueExtensions != null) {
  // var expressionExtensions = valueExtensions.stream()
  // .filter(e -> e.getUrl() != null && e.getUrl().equals(Constants.CQF_EXPRESSION))
  // .collect(Collectors.toList());
  // if (expressionExtensions != null && !expressionExtensions.isEmpty()) {
  // var expression = expressionExtensions.get(0).getValue();
  // if (expression != null) {
  // var result = getExpressionResult(expression, defaultLibraryUrl, null);
  // if (result != null) {
  // extension.setValue(result);
  // }
  // }
  // }
  // }
  // }
  // }
  // }

  // protected IBaseDatatype getExpressionResult(IBaseDatatype expression, String defaultLibraryUrl,
  // IBaseDatatype altExpression) {
  // List<IBase> result = null;
  // if (expression instanceof org.hl7.fhir.r4.model.Expression) {
  // result = libraryEngine.resolveExpression(subjectId,
  // new CqfExpression((org.hl7.fhir.r4.model.Expression) expression, defaultLibraryUrl,
  // (org.hl7.fhir.r4.model.Expression) altExpression),
  // parameters, bundle);
  // }

  // if (expression instanceof org.hl7.fhir.r5.model.Expression) {
  // result = libraryEngine.resolveExpression(subjectId,
  // new CqfExpression((org.hl7.fhir.r5.model.Expression) expression, defaultLibraryUrl,
  // (org.hl7.fhir.r5.model.Expression) altExpression),
  // parameters, bundle);
  // }

  // return result != null && !result.isEmpty() ? (IBaseDatatype) result.get(0) : null;
  // }
}
