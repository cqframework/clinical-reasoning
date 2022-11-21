package org.opencds.cqf.cql.evaluator.plandefinition.r5;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Enumerations.ActionRelationshipType;
import org.hl7.fhir.r5.model.Enumerations.RequestIntent;
import org.hl7.fhir.r5.model.Enumerations.RequestStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Goal;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.PrimitiveType;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RequestGroup;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Task;
import org.hl7.fhir.r5.model.UriType;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.evaluator.activitydefinition.r5.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;

@SuppressWarnings({ "unused", "squid:S107" })
public class PlanDefinitionProcessor {
  protected ActivityDefinitionProcessor activityDefinitionProcessor;
  protected LibraryProcessor libraryProcessor;
  protected ExpressionEvaluator expressionEvaluator;
  protected OperationParametersParser operationParametersParser;
  protected FhirContext fhirContext;
  protected FhirDal fhirDal;
  protected IFhirPath fhirPath;

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);
  private static final String alternateExpressionExtension = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";
  private static final String requestGroupGoalExtension = "http://fl7.org/fhir/StructureDefinition/RequestGroup-Goal";

  public PlanDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
      ExpressionEvaluator expressionEvaluator,
      ActivityDefinitionProcessor activityDefinitionProcessor, OperationParametersParser operationParametersParser) {
    requireNonNull(fhirContext, "fhirContext can not be null");
    requireNonNull(fhirDal, "fhirDal can not be null");
    requireNonNull(libraryProcessor, "LibraryProcessor can not be null");
    requireNonNull(operationParametersParser, "OperationParametersParser can not be null");
    this.fhirContext = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    this.fhirDal = fhirDal;
    this.libraryProcessor = libraryProcessor;
    this.expressionEvaluator = expressionEvaluator;
    this.activityDefinitionProcessor = activityDefinitionProcessor;
    this.operationParametersParser = operationParametersParser;
  }

  public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type, String errorMessage) {
    if (obj == null)
      return Optional.empty();
    if (type.isInstance(obj)) {
      return Optional.of(type.cast(obj));
    }
    throw new IllegalArgumentException(errorMessage);
  }

  public Bundle apply(IdType theId, String patientId, String encounterId, String practitionerId,
      String organizationId, String userType, String userLanguage, String userTaskContext, String setting,
      String settingContext, Boolean mergeNestedCarePlans, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    var planDefinition = getPlanDefinition(theId);
    var session = createSession(patientId, encounterId, practitionerId, organizationId, userType,
        userLanguage, userTaskContext, setting, settingContext, parameters, useServerData, bundle,
        prefetchData, dataEndpoint, contentEndpoint, terminologyEndpoint);

    var requestGroup = applyPlanDefinition(planDefinition, session);

    var resultBundle = new Bundle().setType(BundleType.COLLECTION);
    resultBundle.addEntry().setResource(requestGroup);
    for (var resource : session.requestResources) {
      resultBundle.addEntry().setResource((Resource) resource);
    }

    return resultBundle;
  }

  private PlanDefinition getPlanDefinition(IdType theId) {
    var basePlanDefinition = this.fhirDal.read(theId);

    requireNonNull(basePlanDefinition, "Couldn't find PlanDefinition " + theId);

    var planDefinition = castOrThrow(basePlanDefinition, PlanDefinition.class,
        "The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class").get();

    logger.info("Performing $apply operation on PlanDefinition/{}", theId);

    return planDefinition;
  }

  private Session createSession(String patientId, String encounterId, String practitionerId,
      String organizationId, String userType, String userLanguage, String userTaskContext, String setting,
      String settingContext, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    // warn if prefetchData exists
    // if no data anywhere blow up

    var prefetchDataKey = castOrThrow(
        operationParametersParser.getValueChild(prefetchData, "key"), StringType.class,
        "prefetchData key must be a String").map(PrimitiveType::asStringValue).orElse(null);

    var prefetchDataDescription = castOrThrow(
        operationParametersParser.getResourceChild(prefetchData, "descriptor"), DataRequirement.class,
        "prefetchData descriptor must be a DataRequirement").orElse(null);

    var prefetchDataData = castOrThrow(
        operationParametersParser.getResourceChild(prefetchData, "data"), IBaseBundle.class,
        "prefetchData data must be a Bundle").orElse(null);

    var requestResources = new ArrayList<IBaseResource>();

    return new Session(requestResources, patientId, encounterId, practitionerId, organizationId,
        userType, userLanguage, userTaskContext, setting, settingContext, parameters, prefetchData,
        contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, useServerData,
        prefetchDataData, prefetchDataDescription, prefetchDataKey);
  }

  private RequestGroup applyPlanDefinition(PlanDefinition planDefinition, Session session) {
    // Each Group of actions shares a RequestGroup
    var requestGroup = new RequestGroup()
        .setStatus(RequestStatus.DRAFT.toCode())
        .setIntent(RequestIntent.PROPOSAL.toCode())
        .addInstantiatesCanonical(planDefinition.getUrl())
        .setSubject(new Reference(session.patientId));

    requestGroup.setId(new IdType(requestGroup.fhirType(), planDefinition.getIdElement().getIdPart()));
    if (session.encounterId != null)
      requestGroup.setEncounter(new Reference(session.encounterId));
    if (session.practitionerId != null)
      requestGroup.setAuthor(new Reference(session.practitionerId));
    if (session.organizationId != null)
      requestGroup.setAuthor(new Reference(session.organizationId));
    if (session.userLanguage != null)
      requestGroup.setLanguage(session.userLanguage);

    for (int i = 0; i < planDefinition.getGoal().size(); i++) {
      // TODO: This needs to be added as an extension for R4
      var goal = convertGoal(planDefinition.getGoal().get(i));
      goal.setIdElement(new IdType("Goal", String.valueOf(i + 1)));
      requestGroup.addExtension().setUrl(requestGroupGoalExtension).setValue(new Reference(goal.getIdElement()));
      session.requestResources.add(goal);
    }

    // TODO: Create Questionnaire for the RequestGroup if using Modular
    // Questionnaires. Grab the session Questionnaire if doing Dynamic.

    var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();

    for (var action : planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      resolveAction(planDefinition, requestGroup, session, metConditions, action);
    }

    return requestGroup;
    // return
    // (RequestGroup)ContainedHelper.liftContainedResourcesToParent(requestGroup);
  }

  private Goal convertGoal(PlanDefinition.PlanDefinitionGoalComponent goal) {
    var myGoal = new Goal();
    myGoal.setCategory(Collections.singletonList(goal.getCategory()));
    myGoal.setDescription(goal.getDescription());
    myGoal.setPriority(goal.getPriority());
    myGoal.setStart(goal.getStart());

    myGoal.setTarget(goal.getTarget().stream().map((target) -> {
      Goal.GoalTargetComponent myTarget = new Goal.GoalTargetComponent();
      myTarget.setDetail(target.getDetail());
      myTarget.setMeasure(target.getMeasure());
      myTarget.setDue(target.getDue());
      myTarget.setExtension(target.getExtension());
      return myTarget;
    }).collect(Collectors.toList()));
    return myGoal;
  }

  private void resolveAction(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    // TODO: If action has inputs generate QuestionnaireItems
    if (Boolean.TRUE.equals(meetsConditions(planDefinition, requestGroup, session, action))) {
      if (action.hasRelatedAction()) {
        for (var relatedActionComponent : action.getRelatedAction()) {
          if (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER)
              && metConditions.containsKey(relatedActionComponent.getId())) {
            metConditions.put(action.getId(), action);
            resolveDefinition(planDefinition, requestGroup, session, action);
            resolveDynamicActions(planDefinition, requestGroup, session, action);
          }
        }
      }
      metConditions.put(action.getId(), action);
      resolveDefinition(planDefinition, requestGroup, session, action);
      resolveDynamicActions(planDefinition, requestGroup, session, action);
    }
  }

  private void resolveDefinition(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinitionCanonicalType()) {
      logger.debug("Resolving definition {}", action.getDefinitionCanonicalType().getValue());
      var definition = action.getDefinitionCanonicalType();
      var resourceName = getResourceName(definition);
      switch (requireNonNull(resourceName)) {
        case "PlanDefinition":
          applyNestedPlanDefinition(requestGroup, session, definition, action);
          break;
        case "ActivityDefinition":
          applyActivityDefinition(planDefinition, requestGroup, session, definition, action);
          break;
        case "Questionnaire":
          applyQuestionnaireDefinition(planDefinition, requestGroup, session, definition, action);
          break;
        default:
          throw new RuntimeException(String.format("Unknown action definition: %s", definition));
      }
    } else if (action.hasDefinitionUriType()) {
      var definition = action.getDefinitionUriType();
      applyUriDefinition(requestGroup, definition, action);
    }
  }

  private void applyUriDefinition(RequestGroup requestGroup, UriType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    requestGroup
        .addAction()
        .setResource(new Reference(definition.asStringValue()))
        .setTitle(action.getTitle())
        .setDescription(action.getDescription())
        .setTextEquivalent(action.getTextEquivalent())
        .setCode(Collections.singletonList(action.getCode()))
        .setTiming(action.getTiming());
  }

  private void applyQuestionnaireDefinition(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      CanonicalType definition, PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        result = resolveContained(planDefinition, definition.getValue());
      } else {
        var iterator = fhirDal.searchByUrl("Questionnaire", definition.asStringValue()).iterator();
        if (!iterator.hasNext()) {
          throw new RuntimeException("No questionnaire found for definition: " + definition);
        }
        result = iterator.next();
      }

      applyAction(requestGroup, result, action);
      requestGroup.addAction().setResource(new Reference(result.getIdElement()));
      session.requestResources.add(result);

    } catch (Exception e) {
      e.printStackTrace();
      logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyActivityDefinition(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        var activityDefinition = (ActivityDefinition) resolveContained(planDefinition, definition.getValue());
        result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition, session.patientId,
            session.practitionerId, session.organizationId, session.parameters, session.contentEndpoint,
            session.terminologyEndpoint, session.dataEndpoint);
        result.setId(activityDefinition.getIdElement().withResourceType(result.fhirType()));
      } else {
        var iterator = fhirDal.searchByUrl("ActivityDefinition", definition.asStringValue()).iterator();
        if (!iterator.hasNext()) {
          throw new RuntimeException("No activity definition found for definition: " + definition);
        }
        var activityDefinition = (ActivityDefinition) iterator.next();
        result = this.activityDefinitionProcessor.apply(activityDefinition.getIdElement(), session.patientId,
            session.encounterId, session.practitionerId, session.organizationId, session.userType, session.userLanguage,
            session.userTaskContext, session.setting, session.settingContext, session.parameters,
            session.contentEndpoint, session.terminologyEndpoint, session.dataEndpoint);
        result.setId(activityDefinition.getIdElement().withResourceType(result.fhirType()));
      }

      applyAction(requestGroup, result, action);
      requestGroup.addAction().setResource(new Reference(result.getIdElement()));
      session.requestResources.add(result);
    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyNestedPlanDefinition(RequestGroup requestGroup, Session session, CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    var iterator = fhirDal.searchByUrl("PlanDefinition", definition.asStringValue()).iterator();
    if (!iterator.hasNext()) {
      throw new RuntimeException("No plan definition found for definition: " + definition);
    }
    var planDefinition = (PlanDefinition) iterator.next();
    var result = applyPlanDefinition(planDefinition, session);

    applyAction(requestGroup, result, action);

    // Add an action to the request group which points to this CarePlan
    requestGroup.addAction().setResource(new Reference(result.getIdElement()));
    session.requestResources.add(result);

    for (var c : result.getInstantiatesCanonical()) {
      requestGroup.addInstantiatesCanonical(c.getValueAsString());
    }
  }

  private void applyAction(RequestGroup requestGroup, IBaseResource result,
      PlanDefinition.PlanDefinitionActionComponent action) {
    switch (result.fhirType()) {
      case "Task":
        resolveTask(requestGroup, (Task) result, action);
        break;
      default:
        break;
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon
   * period periodUnit frequency count ) use task code
   */
  private Resource resolveTask(RequestGroup requestGroup, Task task,
      PlanDefinition.PlanDefinitionActionComponent action) {
    task.setId(new IdType(task.fhirType(), action.getId()));
    // What is this block of code supposed to be doing?
    // if (!task.hasCode() && action.hasCode()) {
    // for (var actionCode : action.getCode()) {
    // var foundExecutableTaskCode = false;
    // for (var actionCoding : actionCode.getCoding()) {
    // if
    // (actionCoding.getSystem().equals("http://aphl.org/fhir/ecr/CodeSystem/executable-task-types"))
    // {
    // foundExecutableTaskCode = true;
    // }
    // }
    // }
    // }
    if (action.hasRelatedAction()) {
      var relatedActions = action.getRelatedAction();
      for (var relatedAction : relatedActions) {
        var next = new Extension();
        next.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/next");
        if (relatedAction.hasOffset()) {
          var offsetExtension = new Extension();
          offsetExtension.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/offset");
          offsetExtension.setValue(relatedAction.getOffset());
          next.addExtension(offsetExtension);
        }
        var target = new Extension();
        var targetRef = new Reference(new IdType(task.fhirType(), relatedAction.getId()));
        target.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/target");
        target.setValue(targetRef);
        next.addExtension(target);
        task.addExtension(next);
      }
    }

    if (action.hasCondition()) {
      var conditionComponents = action.getCondition();
      for (var conditionComponent : conditionComponents) {
        var condition = new Extension();
        condition.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/condition");
        condition.setValue(conditionComponent.getExpression());
        if (conditionComponent.hasExtension(alternateExpressionExtension)) {
          condition.addExtension(conditionComponent.getExtensionByUrl(alternateExpressionExtension));
        }
        task.addExtension(condition);
      }
    }

    if (action.hasInput()) {
      var dataRequirements = action.getInput().stream()
          .map(PlanDefinition.PlanDefinitionActionInputComponent::getRequirement).collect(Collectors.toList());
      for (var dataRequirement : dataRequirements) {
        var input = new Extension();
        input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
        input.setValue(dataRequirement);
        task.addExtension(input);
      }
    }
    task.addBasedOn(new Reference(requestGroup));
    return task;
  }

  private void resolveDynamicActions(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      PlanDefinition.PlanDefinitionActionComponent action) {
    for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
      logger.info("Resolving dynamic value {} {}", dynamicValue.getPath(), dynamicValue.getExpression());

      ensureDynamicValueExpression(dynamicValue);
      if (dynamicValue.getExpression().hasLanguage()) {

        var libraryToBeEvaluated = ensureLibrary(planDefinition, dynamicValue.getExpression());
        var language = dynamicValue.getExpression().getLanguage();
        var requirements = action.getInput().stream()
            .map(PlanDefinition.PlanDefinitionActionInputComponent::getRequirement).collect(Collectors.toList());
        var result = evaluateConditionOrDynamicValue(dynamicValue.getExpression().getExpression(), language,
            libraryToBeEvaluated, session, requirements);
        if (result == null && dynamicValue.getExpression().hasExtension(alternateExpressionExtension)) {
          var alternateExpressionValue = dynamicValue.getExpression().getExtensionByUrl(alternateExpressionExtension)
              .getValue();
          if (!(alternateExpressionValue instanceof Expression)) {
            throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
          }
          var alternateExpression = (Expression) alternateExpressionValue;
          if (alternateExpression.hasLanguage()) {
            libraryToBeEvaluated = ensureLibrary(planDefinition, alternateExpression);
            language = alternateExpression.getLanguage();
            result = evaluateConditionOrDynamicValue(alternateExpression.getExpression(), language,
                libraryToBeEvaluated, session, requirements);
          }
        }

        // TODO - likely need more date transformations
        if (result instanceof DateTime) {
          result = Date.from(((DateTime) result).getDateTime().toInstant());
        }

        // TODO: Rename bundle
        if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this")) {
          requestGroup = ((RequestGroup) result);
        } else if (dynamicValue.hasPath()
            && (dynamicValue.getPath().startsWith("action") || dynamicValue.getPath().startsWith("%action"))) {
          try {
            action.setProperty(dynamicValue.getPath().substring(dynamicValue.getPath().indexOf(".") + 1),
                (Base) result);
          } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format("Could not set path %s to value: %s", dynamicValue.getPath(), result));
          }
        } else {
          try {
            requestGroup.setProperty(dynamicValue.getPath(), (Base) result);
          } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                String.format("Could not set path %s to value: %s", dynamicValue.getPath(), result));
          }
        }
      }
    }
  }

  private Boolean meetsConditions(PlanDefinition planDefinition, RequestGroup requestGroup, Session session,
      PlanDefinition.PlanDefinitionActionComponent action) {
    // Should we be resolving child actions regardless of whether the conditions are
    // met?
    if (action.hasAction()) {
      for (var containedAction : action.getAction()) {
        var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();
        resolveAction(planDefinition, requestGroup, session, metConditions, containedAction);
      }
    }
    if (planDefinition.getType().hasCoding()) {
      var planDefinitionTypeCoding = planDefinition.getType().getCoding();
      for (var coding : planDefinitionTypeCoding) {
        if (coding.getCode().equals("workflow-definition")) {
          // logger.info(String.format("Found a workflow definition type for
          // PlanDefinition % conditions should be evaluated at task execution time."),
          // session.planDefinition.getUrl());
          return true;
        }
      }
    }
    for (var condition : action.getCondition()) {
      ensureConditionExpression(condition);
      if (condition.getExpression().hasLanguage()) {

        var libraryToBeEvaluated = ensureLibrary(planDefinition, condition.getExpression());
        var language = condition.getExpression().getLanguage();
        var requirements = action.getInput().stream()
            .map(PlanDefinition.PlanDefinitionActionInputComponent::getRequirement).collect(Collectors.toList());
        var result = evaluateConditionOrDynamicValue(condition.getExpression().getExpression(), language,
            libraryToBeEvaluated, session, requirements);
        if (result == null && condition.getExpression().hasExtension(alternateExpressionExtension)) {
          var alternateExpressionValue = condition.getExpression().getExtensionByUrl(alternateExpressionExtension)
              .getValue();
          if (!(alternateExpressionValue instanceof Expression)) {
            throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
          }
          var alternateExpression = (Expression) alternateExpressionValue;
          if (alternateExpression.hasLanguage()) {
            libraryToBeEvaluated = ensureLibrary(planDefinition, alternateExpression);
            language = alternateExpression.getLanguage();
            result = evaluateConditionOrDynamicValue(alternateExpression.getExpression(), language,
                libraryToBeEvaluated, session, requirements);
          }
        }
        if (result == null) {
          logger.warn("Expression Returned null");
          return false;
        }

        if (!(result instanceof BooleanType)) {
          logger.warn("The condition returned a non-boolean value: {}", result.getClass().getSimpleName());
          continue;
        }

        if (!((BooleanType) result).booleanValue()) {
          logger.info("The result of condition id {} expression language {} is false", condition.getId(),
              condition.getExpression().getLanguage());
          return false;
        }
      }
    }
    return true;
  }

  protected String ensureLibrary(PlanDefinition planDefinition, Expression expression) {
    if (expression.hasReference()) {
      return expression.getReference();
    }
    logger.warn("No library reference for expression: {}", expression.getExpression());
    if (planDefinition.getLibrary().size() == 1) {
      return planDefinition.getLibrary().get(0).asStringValue();
    }
    logger.warn("No primary library defined");
    return null;
  }

  protected void ensureConditionExpression(PlanDefinition.PlanDefinitionActionConditionComponent condition) {
    if (!condition.hasExpression()) {
      logger.error("Missing condition expression");
      throw new RuntimeException("Missing condition expression");
    }
  }

  protected void ensureDynamicValueExpression(PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue) {
    if (!dynamicValue.hasExpression()) {
      logger.error("Missing dynamicValue expression");
      throw new RuntimeException("Missing dynamicValue expression");
    }
  }

  // private String ensureStringResult(Object result) {
  // if (!(result instanceof StringType))
  // throw new RuntimeException("Result not instance of String");
  // return ((StringType) result).asStringValue();
  // }

  protected static String getResourceName(CanonicalType canonical) {
    if (canonical.hasValue()) {
      var id = canonical.getValue();
      if (id.contains("/")) {
        id = id.replace(id.substring(id.lastIndexOf("/")), "");
        return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
      }
      return null;
    }

    throw new RuntimeException("CanonicalType must have a value for resource name extraction");
  }

  public Object getParameterComponentByName(Parameters params, String name) {
    var first = params.getParameter().stream().filter(x -> x.getName().equals(name)).findFirst();
    var component = first.isPresent() ? first.get() : new ParametersParameterComponent();
    return component.hasValue() ? component.getValue() : component.getResource();
  }

  protected Object evaluateConditionOrDynamicValue(String expression, String language, String libraryToBeEvaluated,
      Session session, List<DataRequirement> dataRequirements) {
    var params = resolveInputParameters(dataRequirements);
    if (session.parameters instanceof Parameters) {
      params.getParameter().addAll(((Parameters) session.parameters).getParameter());
    }

    Object result = null;
    switch (language) {
      case "text/cql":
      case "text/cql.expression":
      case "text/cql-expression":
        result = expressionEvaluator.evaluate(expression, params);
        // The expression is assumed to be the parameter component name used in
        // getParameterComponentByName()
        // The expression evaluator creates a library with a single expression defined
        // as "return"
        expression = "return";
        break;
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        result = libraryProcessor.evaluate(libraryToBeEvaluated, session.patientId, session.parameters,
            session.contentEndpoint, session.terminologyEndpoint, session.dataEndpoint, session.bundle,
            Collections.singleton(expression));
        break;
      case "text/fhirpath":
        List<IBase> outputs;
        try {
          outputs = fhirPath.evaluate(null, expression, IBase.class);
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
    if (result instanceof Parameters) {
      result = getParameterComponentByName((Parameters) result, expression);
    }
    return result;
  }

  private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    if (dataRequirements == null)
      return new Parameters();

    var params = new Parameters();
    for (var req : dataRequirements) {
      var resources = fhirDal.search(req.getType().name()).iterator();

      if (resources.hasNext()) {
        var index = 0;
        var found = true;
        while (resources.hasNext()) {
          var resource = (Resource) resources.next();
          var parameter = new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
          if (req.hasCodeFilter()) {
            for (var filter : req.getCodeFilter()) {
              var codeFilterParam = new Parameters();
              codeFilterParam.addParameter().setName("%resource").setResource(resource);
              if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                var valueset = fhirDal.searchByUrl("ValueSet", filter.getValueSet());
                if (valueset != null && valueset.iterator().hasNext()) {
                  codeFilterParam.addParameter().setName("%valueset")
                      .setResource((Resource) valueset.iterator().next());
                  var codeFilterExpression = "%"
                      + String.format("resource.%s.where(code.memberOf('%s'))", filter.getPath(), "%" + "valueset");
                  var codeFilterResult = expressionEvaluator.evaluate(codeFilterExpression, codeFilterParam);
                  var tempResult = operationParametersParser.getValueChild((codeFilterResult), "return");
                  if (tempResult instanceof BooleanType) {
                    found = ((BooleanType) tempResult).booleanValue();
                  }
                }
                logger.debug("Could not find ValueSet with url {} on the local server.", filter.getValueSet());
              }
            }
          }
          if (!resources.hasNext() && index == 0) {
            parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                new ParameterDefinition().setMax("*").setName("%" + req.getId()));
            if (found) {
              parameter.setResource(resource);
            }
          } else {
            if (!found) {
              index++;
              continue;
            }
            parameter.setResource(resource);
          }
          params.addParameter(parameter);
          index++;
        }
      } else {
        var parameter = new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
        parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
            new ParameterDefinition().setMax("*").setName("%" + req.getId()));
        params.addParameter(parameter);
      }
    }
    return params;
  }

  protected Resource resolveContained(DomainResource resource, String id) {
    var first = resource.getContained().stream()
        .filter(Resource::hasIdElement)
        .filter(x -> x.getIdElement().getIdPart().equals(id))
        .findFirst();
    return first.orElse(null);
  }
}
