package org.opencds.cqf.cql.evaluator.plandefinition;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.ParametersUtil;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BasePlanDefinitionProcessor<T> {
   private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
   protected static final String ALT_EXPRESSION_EXT = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";
   protected static final String REQUEST_GROUP_EXT = "http://fl7.org/fhir/StructureDefinition/RequestGroup-Goal";

   protected LibraryProcessor libraryProcessor;
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
   protected IBaseResource dataEndpoint;
   protected IBaseResource contentEndpoint;
   protected IBaseResource terminologyEndpoint;
   protected Boolean containResources;
   protected final Collection<IBaseResource> requestResources;
   protected final ModelResolver modelResolver;

   protected BasePlanDefinitionProcessor(
           FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
           ExpressionEvaluator expressionEvaluator, OperationParametersParser operationParametersParser) {
      requireNonNull(fhirContext, "fhirContext can not be null");
      requireNonNull(fhirDal, "fhirDal can not be null");
      requireNonNull(libraryProcessor, "LibraryProcessor can not be null");
      requireNonNull(operationParametersParser, "OperationParametersParser can not be null");
      this.fhirContext = fhirContext;
      this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
      this.fhirDal = fhirDal;
      this.libraryProcessor = libraryProcessor;
      this.expressionEvaluator = expressionEvaluator;
      this.operationParametersParser = operationParametersParser;
      this.requestResources = new ArrayList<>();
      modelResolver = new FhirModelResolverFactory().create(fhirContext.getVersion().getVersion().getFhirVersionString());
   }

   public abstract T resolvePlanDefinition(IIdType theId);
   public abstract IBaseResource applyPlanDefinition(T planDefinition);
   public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);
   public abstract IBaseResource transformToBundle(IBaseResource requestGroup);
   public abstract Object resolveParameterValue(IBase value);
   public abstract void resolveCdsHooksDynamicValue(IBaseResource requestGroup, Object value, String path);
   public abstract IBaseResource getSubject();
   public abstract void extractQuestionnaireResponse();

   public IBaseResource apply(
           IIdType theId, String patientId, String encounterId, String practitionerId,
           String organizationId, String userType, String userLanguage, String userTaskContext,
           String setting, String settingContext, Boolean mergeNestedCarePlans, IBaseParameters parameters,
           Boolean useServerData, IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint,
           IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
      if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R5) {
         return applyR5(theId, patientId, encounterId, practitionerId, organizationId, userType,
                 userLanguage, userTaskContext, setting, settingContext, mergeNestedCarePlans, parameters,
                 useServerData, bundle, prefetchData, dataEndpoint, contentEndpoint, terminologyEndpoint);
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
      this.dataEndpoint = dataEndpoint;
      this.contentEndpoint = contentEndpoint;
      this.terminologyEndpoint = terminologyEndpoint;
      this.containResources = true;
      extractQuestionnaireResponse();
      return transformToCarePlan(applyPlanDefinition(resolvePlanDefinition(theId)));
   }

   public IBaseResource applyR5(
           IIdType theId, String patientId, String encounterId, String practitionerId,
           String organizationId, String userType, String userLanguage, String userTaskContext,
           String setting, String settingContext, Boolean mergeNestedCarePlans, IBaseParameters parameters,
           Boolean useServerData, IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint,
           IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
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
      this.dataEndpoint = dataEndpoint;
      this.contentEndpoint = contentEndpoint;
      this.terminologyEndpoint = terminologyEndpoint;
      this.containResources = false;
      extractQuestionnaireResponse();
      return transformToBundle(applyPlanDefinition(resolvePlanDefinition(theId)));
   }

   public IBase getExpressionResult(
           String expression, String language, String libraryToBeEvaluated, IBaseParameters params) {
      validateExpression(language, expression);
      IBase result = null;
      IBaseParameters parametersResult;
      switch (language) {
         case "text/cql":
         case "text/cql.expression":
         case "text/cql-expression":
            parametersResult = expressionEvaluator.evaluate(expression, params);
            // The expression is assumed to be the parameter component name
            // The expression evaluator creates a library with a single expression defined as "return"
            expression = "return";
            result = (IBase) resolveParameterValue(ParametersUtil.getNamedParameter(
                    fhirContext, parametersResult, expression).orElse(null));
            break;
         case "text/cql-identifier":
         case "text/cql.identifier":
         case "text/cql.name":
         case "text/cql-name":
            parametersResult = libraryProcessor.evaluate(libraryToBeEvaluated, patientId, parameters,
                    contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, Collections.singleton(expression));
            result = (IBase) resolveParameterValue(ParametersUtil.getNamedParameter(
                    fhirContext, parametersResult, expression).orElse(null));
            break;
         case "text/fhirpath":
            List<IBase> outputs;
            try {
               outputs = fhirPath.evaluate(getSubject(), expression, IBase.class);
            } catch (FhirPathExecutionException e) {
               throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
            }
            if (outputs != null && outputs.size() == 1) {
               result = outputs.get(0);
            } else {
               throw new IllegalArgumentException(
                       "Expected only one value when evaluating FHIRPath expression: " + expression);
            }
            break;
         default:
            logger.warn("An action language other than CQL was found: {}", language);
      }

      return result;
   }

   public void validateExpression(String language, String expression) {
      if (language == null) {
         logger.error("Missing language type for the Expression");
         throw new IllegalArgumentException("Missing language type for the Expression");
      }
      else if (expression == null) {
         logger.error("Missing expression for the Expression");
         throw new IllegalArgumentException("Missing expression for the Expression");
      }
   }

   public void validateExpressionWithPath(String language, String expression, String path) {
      validateExpression(language, expression);
      if (path == null) {
         logger.error("Missing element path for the Expression");
         throw new IllegalArgumentException("Missing element path for the Expression");
      }
   }

   public void resolveDynamicValue(
           String language, String expression, String path, String altLanguage, String altExpression,
           String altPath, String libraryUrl, IBaseResource resource, IBaseParameters params) {
      validateExpressionWithPath(language, expression, path);
      Object result = getExpressionResult(expression, language, libraryUrl, params);
      if (result == null && altExpression != null) {
         validateExpressionWithPath(altLanguage, altExpression, altPath);
         path = altPath;
         result = getExpressionResult(altExpression, altLanguage, libraryUrl, params);
      }
      if (path.startsWith("action.")) {
         resolveCdsHooksDynamicValue(resource, result, path);
      }
      // backwards compatibility where CDS Hooks indicator was set with activity.extension or action.extension path
      else if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
         if (fhirContext.getVersion().getVersion() == FhirVersionEnum.R5) {
            throw new IllegalArgumentException("Please use the priority path when setting indicator values when using FHIR R5 for CDS Hooks evaluation");
         }
         resolveCdsHooksDynamicValue(resource, result, path);
      }
      else {
         modelResolver.setValue(resource, path, result);
      }
   }

   public Object resolveCondition(
           String language, String expression, String altLanguage, String altExpression,
           String libraryUrl, IBaseParameters params) {
      validateExpression(language, expression);
      Object result = getExpressionResult(expression, language, libraryUrl, params);
      if (result == null && altExpression != null) {
         validateExpression(altLanguage, altExpression);
         result = getExpressionResult(altExpression, altLanguage, libraryUrl, params);
      }

      return result;
   }
}
