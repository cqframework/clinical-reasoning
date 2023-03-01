package org.opencds.cqf.cql.evaluator.plandefinition;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BasePlanDefinitionProcessor<T> {
  private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
  protected static final String ALT_EXPRESSION_EXT =
      "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";
  protected static final String REQUEST_GROUP_EXT =
      "http://fl7.org/fhir/StructureDefinition/RequestGroup-Goal";

  protected static final String subjectType = "Patient";

  protected LibraryEngine libraryEngine;
  protected ExpressionEvaluator expressionEvaluator;
  protected OperationParametersParser operationParametersParser;
  protected FhirContext fhirContext;
  protected FhirDal fhirDal;
  protected IFhirPath fhirPath;

  protected String patientId;
  protected String encounterId;
  protected String practitionerId;
  protected String organizationId;
  protected String userType;
  protected String userLanguage;
  protected String userTaskContext;
  protected String setting;
  protected String settingContext;
  protected Boolean mergeNestedCarePlans;
  protected IBaseParameters parameters;
  protected Boolean useServerData;
  protected IBaseBundle bundle;
  protected IBaseParameters prefetchData;
  protected Boolean containResources;
  protected IBaseResource questionnaire;
  protected final Collection<IBaseResource> requestResources;
  protected final Collection<IBaseResource> extractedResources;
  protected final ModelResolver modelResolver;

  protected BasePlanDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal,
      OperationParametersParser operationParametersParser) {
    requireNonNull(fhirContext, "fhirContext can not be null");
    requireNonNull(fhirDal, "fhirDal can not be null");
    requireNonNull(libraryEngine, "LibraryEngine can not be null");
    requireNonNull(operationParametersParser, "OperationParametersParser can not be null");
    this.fhirContext = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    this.fhirDal = fhirDal;
    this.operationParametersParser = operationParametersParser;
    this.requestResources = new ArrayList<>();
    this.extractedResources = new ArrayList<>();
    modelResolver = new FhirModelResolverFactory()
        .create(fhirContext.getVersion().getVersion().getFhirVersionString());
  }

  public abstract LibraryEngine buildLibraryEngine(IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);

  public abstract T resolvePlanDefinition(IIdType theId);

  public abstract IBaseResource applyPlanDefinition(T planDefinition);

  public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);

  public abstract IBaseResource transformToBundle(IBaseResource requestGroup);

  // public abstract Object resolveParameterValue(IBase value);

  public abstract void resolveCdsHooksDynamicValue(IBaseResource requestGroup, Object value,
      String path);

  // public abstract IBaseResource getSubject();

  public abstract void extractQuestionnaireResponse();

  public abstract void createDynamicQuestionnaire(String theId);

  public IBaseResource apply(IIdType theId, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, Boolean mergeNestedCarePlans,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    return apply(theId, patientId, encounterId, practitionerId, organizationId, userType,
        userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlans, parameters,
        useServerData, bundle, prefetchData,
        buildLibraryEngine(dataEndpoint, contentEndpoint, terminologyEndpoint));
  }

  public IBaseResource apply(IIdType theId, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, Boolean mergeNestedCarePlans,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, LibraryEngine libraryEngine) {
    if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R5) {
      return applyR5(theId, patientId, encounterId, practitionerId, organizationId, userType,
          userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlans, parameters,
          useServerData, bundle, prefetchData, libraryEngine);
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
    this.mergeNestedCarePlans = mergeNestedCarePlans;
    this.parameters = parameters;
    this.useServerData = useServerData;
    this.bundle = bundle;
    this.prefetchData = prefetchData;
    this.libraryEngine = libraryEngine;
    this.containResources = true;
    extractQuestionnaireResponse();
    createDynamicQuestionnaire(theId.getIdPart());
    return transformToCarePlan(applyPlanDefinition(resolvePlanDefinition(theId)));
  }

  public IBaseResource applyR5(IIdType theId, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, Boolean mergeNestedCarePlans,
      IBaseParameters parameters, Boolean useServerData, IBaseBundle bundle,
      IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    return applyR5(theId, patientId, encounterId, practitionerId, organizationId, userType,
        userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlans, parameters,
        useServerData, bundle, prefetchData,
        buildLibraryEngine(dataEndpoint, contentEndpoint, terminologyEndpoint));
  }

  public IBaseResource applyR5(IIdType theId, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, Boolean mergeNestedCarePlans,
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
    this.mergeNestedCarePlans = mergeNestedCarePlans;
    this.parameters = parameters;
    this.useServerData = useServerData;
    this.bundle = bundle;
    this.prefetchData = prefetchData;
    this.libraryEngine = libraryEngine;
    this.containResources = false;
    extractQuestionnaireResponse();
    createDynamicQuestionnaire(theId.getIdPart());
    return transformToBundle(applyPlanDefinition(resolvePlanDefinition(theId)));
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
    Object result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, expression,
        language, libraryUrl, params, this.bundle);
    if (result == null && altExpression != null) {
      validateExpressionWithPath(altLanguage, altExpression, altPath, libraryUrl);
      path = altPath;
      result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, altExpression,
          altLanguage, libraryUrl, params, this.bundle);
    }
    if (path.startsWith("action.")) {
      resolveCdsHooksDynamicValue(resource, result, path);
    }
    // backwards compatibility where CDS Hooks indicator was set with activity.extension or
    // action.extension path
    else if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
      if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R5) {
        throw new IllegalArgumentException(
            "Please use the priority path when setting indicator values when using FHIR R5 for CDS Hooks evaluation");
      }
      resolveCdsHooksDynamicValue(resource, result, path);
    } else {
      modelResolver.setValue(resource, path, result);
    }
  }

  public Object resolveCondition(String language, String expression, String altLanguage,
      String altExpression, String libraryUrl, IBaseParameters params) {
    this.libraryEngine.validateExpression(language, expression, libraryUrl);
    Object result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, expression,
        language, libraryUrl, params, this.bundle);
    if (result == null && altExpression != null) {
      this.libraryEngine.validateExpression(altLanguage, altExpression, libraryUrl);
      result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, altExpression,
          altLanguage, libraryUrl, params, this.bundle);
    }

    return result;
  }
}
