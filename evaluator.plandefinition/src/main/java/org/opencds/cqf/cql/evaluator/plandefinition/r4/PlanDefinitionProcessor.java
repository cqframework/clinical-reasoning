package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.questionnaireresponse.Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE;
import static org.opencds.cqf.cql.evaluator.questionnaireresponse.Constants.SDC_QUESTIONNAIRE_PREPOPULATE;

import java.util.*;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.RequestGroup.RequestIntent;
import org.hl7.fhir.r4.model.RequestGroup.RequestStatus;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.helper.r4.ContainedHelper;
import org.opencds.cqf.cql.evaluator.fhir.util.Clients;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.BasePlanDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.QuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.r4.QuestionnaireResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

@SuppressWarnings({"unused", "squid:S107"})
public class PlanDefinitionProcessor extends BasePlanDefinitionProcessor<PlanDefinition> {

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);

  private final ActivityDefinitionProcessor activityDefinitionProcessor;
  private final QuestionnaireProcessor questionnaireProcessor;
  private final QuestionnaireResponseProcessor questionnaireResponseProcessor;

  public PlanDefinitionProcessor(
          FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
          ExpressionEvaluator expressionEvaluator, ActivityDefinitionProcessor activityDefinitionProcessor,
          OperationParametersParser operationParametersParser) {
    super(fhirContext, fhirDal, libraryProcessor, expressionEvaluator, operationParametersParser);
    this.activityDefinitionProcessor = activityDefinitionProcessor;
    this.questionnaireProcessor = new QuestionnaireProcessor(fhirContext, fhirDal, libraryProcessor, expressionEvaluator);
    this.questionnaireResponseProcessor = new QuestionnaireResponseProcessor(fhirContext, fhirDal);
  }

  public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type, String errorMessage) {
    if (obj == null)
      return Optional.empty();
    if (type.isInstance(obj)) {
      return Optional.of(type.cast(obj));
    }
    throw new IllegalArgumentException(errorMessage);
  }

  @Override
  public void extractQuestionnaireResponse() {
    var questionnaireResponses = ((Bundle) bundle).getEntry().stream()
            .filter(entry -> entry.getResource().fhirType() == Enumerations.FHIRAllTypes.QUESTIONNAIRERESPONSE.toCode())
            .map(entry -> (QuestionnaireResponse) entry.getResource())
            .collect(Collectors.toList());
    if (questionnaireResponses != null && questionnaireResponses.size() > 0) {
      for (var questionnaireResponse : questionnaireResponses) {
        var extractedResources = (Bundle) questionnaireResponseProcessor.extract(questionnaireResponse);
        for (var entry : extractedResources.getEntry()) {
          ((Bundle) bundle).addEntry(entry);
        }
      }
    }
  }

  @Override
  public PlanDefinition resolvePlanDefinition(IIdType theId) {
    var basePlanDefinition = this.fhirDal.read(theId);

    requireNonNull(basePlanDefinition, "Couldn't find PlanDefinition " + theId);

    var planDefinition = castOrThrow(basePlanDefinition, PlanDefinition.class,
            "The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class").orElse(null);

    logger.info("Performing $apply operation on PlanDefinition/{}", theId);

    return planDefinition;
  }

  @Override
  public IBaseResource applyPlanDefinition(PlanDefinition planDefinition) {
    // Each Group of actions shares a RequestGroup
    var requestGroup = new RequestGroup()
            .setStatus(RequestStatus.DRAFT)
            .setIntent(RequestIntent.PROPOSAL)
            .addInstantiatesCanonical(planDefinition.getUrl())
            .setSubject(new Reference(patientId));

    requestGroup.setId(new IdType(requestGroup.fhirType(), planDefinition.getIdElement().getIdPart()));
    if (encounterId != null)
      requestGroup.setEncounter(new Reference(encounterId));
    if (practitionerId != null)
      requestGroup.setAuthor(new Reference(practitionerId));
    if (organizationId != null)
      requestGroup.setAuthor(new Reference(organizationId));
    if (userLanguage != null)
      requestGroup.setLanguage(userLanguage);

    for (int i = 0; i < planDefinition.getGoal().size(); i++) {
      var goal = convertGoal(planDefinition.getGoal().get(i));
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained(goal);
      } else {
        goal.setIdElement(new IdType("Goal", String.valueOf(i + 1)));
        requestGroup.addExtension().setUrl(REQUEST_GROUP_EXT).setValue(new Reference(goal.getIdElement()));
      }
      // Always add goals to the resource list so they can be added to the CarePlan if needed
      requestResources.add(goal);
    }

    // TODO: Create Questionnaire for the RequestGroup if using Modular
    // Questionnaires. Grab the session Questionnaire if doing Dynamic.

    var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();

    for (var action : planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      resolveAction(planDefinition, requestGroup, metConditions, action);
    }

    return requestGroup;
  }

  @Override
  public CarePlan transformToCarePlan(IBaseResource rg) {
    RequestGroup requestGroup = (RequestGroup) rg; 
    var carePlan = new CarePlan()
            .setInstantiatesCanonical(requestGroup.getInstantiatesCanonical())
            .setSubject(requestGroup.getSubject())
            .setStatus(CarePlan.CarePlanStatus.DRAFT)
            .setIntent(CarePlan.CarePlanIntent.PROPOSAL);

    if (requestGroup.hasEncounter()) {
      carePlan.setEncounter(requestGroup.getEncounter());
    }
    if (requestGroup.hasAuthor()) {
      carePlan.setAuthor(requestGroup.getAuthor());
    }
    if (requestGroup.getLanguage() != null) {
      carePlan.setLanguage(requestGroup.getLanguage());
    }
    for (var goal : requestResources) {
      if (goal.fhirType().equals("Goal")) {
        carePlan.addGoal(new Reference((Resource)goal));
      }
    }
    carePlan.addActivity().setReference(new Reference("#" + requestGroup.getIdElement().getIdPart()));
    carePlan.addContained(requestGroup);

    return (CarePlan)ContainedHelper.liftContainedResourcesToParent(carePlan);
  }

  @Override
  public IBaseResource transformToBundle(IBaseResource requestGroup) {
    var resultBundle = new Bundle().setType(BundleType.COLLECTION);
    resultBundle.addEntry().setResource((Resource) requestGroup);
    for (var resource : requestResources) {
      resultBundle.addEntry().setResource((Resource) resource);
    }

    return resultBundle;
  }

  @Override
  public Object resolveParameterValue(IBase value) {
    if (value == null) return null;
    return ((Parameters.ParametersParameterComponent) value).getValue();
  }

  @Override
  public void resolveCdsHooksDynamicValue(IBaseResource rg, Object value, String path) {
    RequestGroup requestGroup = (RequestGroup) rg;
    int matchCount = StringUtils.countMatches(path, "action.");
    if (!requestGroup.hasAction()) {
      for (int i = 0; i < matchCount; ++i) {
        requestGroup.addAction();
      }
    }
    if (path.equals("activity.extension") || path.equals("action.extension")) {
      // default to adding extension to last action
      requestGroup.getAction().get(requestGroup.getAction().size() - 1).addExtension().setValue((Type) value);
      return;
    }
    if (requestGroup.hasAction() && requestGroup.getAction().size() < matchCount) {
      for (int i = matchCount - requestGroup.getAction().size(); i < matchCount; ++i) {
        requestGroup.addAction();
      }
    }
    modelResolver.setValue(requestGroup.getAction().get(matchCount - 1),
            path.replace("action.", ""), value);
  }

  @Override
  public IBaseResource getSubject() {
    return this.fhirDal.read(new IdType("Patient", this.patientId));
  }

  private Goal convertGoal(PlanDefinition.PlanDefinitionGoalComponent goal) {
    var myGoal = new Goal();
    myGoal.setCategory(Collections.singletonList(goal.getCategory()));
    myGoal.setDescription(goal.getDescription());
    myGoal.setPriority(goal.getPriority());
    myGoal.setStart(goal.getStart());

    myGoal.setTarget(goal.getTarget().stream().map(target -> {
      Goal.GoalTargetComponent myTarget = new Goal.GoalTargetComponent();
      myTarget.setDetail(target.getDetail());
      myTarget.setMeasure(target.getMeasure());
      myTarget.setDue(target.getDue());
      myTarget.setExtension(target.getExtension());
      return myTarget;
    }).collect(Collectors.toList()));
    return myGoal;
  }

  private void resolveAction(PlanDefinition planDefinition, RequestGroup requestGroup,
      Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    // TODO: If action has inputs generate QuestionnaireItems

    if (Boolean.TRUE.equals(meetsConditions(planDefinition, requestGroup, action))) {
      if (action.hasRelatedAction()) {
        for (var relatedActionComponent : action.getRelatedAction()) {
          if (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER)
              && metConditions.containsKey(relatedActionComponent.getActionId())) {
            metConditions.put(action.getId(), action);
            resolveDefinition(planDefinition, requestGroup, action);
            resolveDynamicValues(planDefinition, requestGroup, action);
          }
        }
      }
      metConditions.put(action.getId(), action);
      resolveDefinition(planDefinition, requestGroup, action);
      resolveDynamicValues(planDefinition, requestGroup, action);
    }
  }

  private void resolveDefinition(PlanDefinition planDefinition, RequestGroup requestGroup,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinitionCanonicalType()) {
      logger.debug("Resolving definition {}", action.getDefinitionCanonicalType().getValue());
      var definition = action.getDefinitionCanonicalType();
      var resourceName = resolveResourceName(definition, planDefinition);
      switch (requireNonNull(resourceName)) {
        case "PlanDefinition":
          applyNestedPlanDefinition(requestGroup, definition, action);
          break;
        case "ActivityDefinition":
          applyActivityDefinition(planDefinition, requestGroup, definition, action);
          break;
        case "Questionnaire":
          applyQuestionnaireDefinition(planDefinition, requestGroup, definition, action);
          break;
        default:
          throw new FHIRException(String.format("Unknown action definition: %s", definition));
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
        .setCode(action.getCode())
        .setTiming(action.getTiming());
  }

  private void applyQuestionnaireDefinition(PlanDefinition planDefinition, RequestGroup requestGroup,
      CanonicalType definition, PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        result = resolveContained(planDefinition, definition.getValue());
      } else {
        var iterator = fhirDal.searchByUrl("Questionnaire", definition.asStringValue()).iterator();
        if (!iterator.hasNext()) {
          throw new FHIRException("No questionnaire found for definition: " + definition);
        }
        result = iterator.next();
      }

      applyAction(requestGroup, result, action);
      requestGroup.addAction().setResource(new Reference(result.getIdElement()));
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained((Resource) result);
      } else {
        requestResources.add(result);
      }
      // TODO: Add Contained or Add to RequestResources?
      // requestGroup.addContained((Resource) result);

    } catch (Exception e) {
      e.printStackTrace();
      logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyActivityDefinition(PlanDefinition planDefinition, RequestGroup requestGroup,
      CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        var activityDefinition = (ActivityDefinition) resolveContained(planDefinition, definition.getValue());
        result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition, patientId,
            practitionerId, organizationId);
        result.setId(new IdType(result.fhirType(), activityDefinition.getIdPart().replaceFirst("#", "")));
      } else {
        var iterator = fhirDal.searchByUrl("ActivityDefinition", definition.asStringValue()).iterator();
        if (!iterator.hasNext()) {
          throw new FHIRException("No activity definition found for definition: " + definition);
        }
        var activityDefinition = (ActivityDefinition) iterator.next();
        result = this.activityDefinitionProcessor.apply(activityDefinition.getIdElement(), patientId,
            encounterId, practitionerId, organizationId, userType, userLanguage,
            userTaskContext, setting, settingContext, parameters,
            contentEndpoint, terminologyEndpoint, dataEndpoint);
        result.setId(activityDefinition.getIdElement().withResourceType(result.fhirType()));
      }

      applyAction(requestGroup, result, action);
      requestGroup.addAction().setResource(new Reference(result.getIdElement()));
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained((Resource)result);
      } else {
        requestResources.add(result);
      }
    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyNestedPlanDefinition(RequestGroup requestGroup, CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    var iterator = fhirDal.searchByUrl("PlanDefinition", definition.asStringValue()).iterator();
    if (!iterator.hasNext()) {
      throw new FHIRException("No plan definition found for definition: " + definition);
    }
    var planDefinition = (PlanDefinition) iterator.next();
    var result = (RequestGroup) applyPlanDefinition(planDefinition);

    applyAction(requestGroup, result, action);

    // Add an action to the request group which points to this request group
    requestGroup.addAction().setResource(new Reference(result.getIdElement()));
    if (Boolean.TRUE.equals(containResources)) {
      requestGroup.addContained(result);
    } else {
      requestResources.add(result);
    }

    for (var c : result.getInstantiatesCanonical()) {
      requestGroup.addInstantiatesCanonical(c.getValueAsString());
    }
  }

  private void applyAction(RequestGroup requestGroup, IBaseResource result,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if ("Task".equals(result.fhirType())) {
      resolveTask(requestGroup, (Task) result, action);
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon
   * period periodUnit frequency count ) use task code
   */
  private void resolveTask(RequestGroup requestGroup, Task task,
                           PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasId()) {
      task.setId(new IdType(task.fhirType(), action.getId()));
    }
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
        var targetRef = new Reference(new IdType(task.fhirType(), relatedAction.getActionId()));
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
        if (conditionComponent.hasExtension(ALT_EXPRESSION_EXT)) {
          condition.addExtension(conditionComponent.getExtensionByUrl(ALT_EXPRESSION_EXT));
        }
        task.addExtension(condition);
      }
    }

    if (action.hasInput()) {
      var dataRequirements = action.getInput();
      for (var dataRequirement : dataRequirements) {
        var input = new Extension();
        input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
        input.setValue(dataRequirement);
        task.addExtension(input);
      }
    }

    task.addBasedOn(new Reference(requestGroup).setType(requestGroup.fhirType()));
    task.setFor(requestGroup.getSubject());

    if (action.hasExtension(SDC_QUESTIONNAIRE_PREPOPULATE)) {
      resolvePrepopulateAction(action, requestGroup, task);
    }
  }

  private void resolvePrepopulateAction(PlanDefinition.PlanDefinitionActionComponent action, RequestGroup requestGroup, Task task) {
    var questionnaireBundles = getQuestionnaireForOrder(action);
    for (var questionnaireBundle : questionnaireBundles) {
      // Each bundle should contain a Questionnaire and supporting Library and ValueSet resources
      var questionnaire = (Questionnaire) questionnaireBundle.getEntry().get(0).getResource();
      var libraries = questionnaireBundle.getEntry().stream()
              .filter(e -> e.hasResource() && (e.getResource().fhirType() == Enumerations.FHIRAllTypes.LIBRARY.toCode()))
              .map(e -> (Library) e.getResource())
              .collect(Collectors.toList());
      var valueSets = questionnaireBundle.getEntry().stream()
              .filter(e -> e.hasResource() && (e.getResource().fhirType() == Enumerations.FHIRAllTypes.VALUESET.toCode()))
              .map(e -> (ValueSet) e.getResource())
              .collect(Collectors.toList());
      var additionalData = ((Bundle) bundle).copy();
      libraries.forEach(library -> { additionalData.addEntry(new Bundle.BundleEntryComponent().setResource(library)); });
      valueSets.forEach(valueSet -> { additionalData.addEntry(new Bundle.BundleEntryComponent().setResource(valueSet)); });
      var prepopResult = questionnaireProcessor.prePopulate(questionnaire, patientId, this.parameters, additionalData, dataEndpoint, contentEndpoint, terminologyEndpoint);
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained((Resource) prepopResult);
      } else {
        requestResources.add(prepopResult);
      }
      task.setFocus(new Reference(prepopResult.getIdElement()));
      task.setFor(requestGroup.getSubject());
    }
  }

  private List<Bundle> getQuestionnaireForOrder(PlanDefinition.PlanDefinitionActionComponent action) {
    Bundle bundle = null;
    // PlanDef action should provide endpoint for $questionnaire-for-order operation as well as the order id to pass
    var prepopulateExtension = action.getExtensionByUrl(SDC_QUESTIONNAIRE_PREPOPULATE);
    var parameterName = prepopulateExtension.getValue().toString();
    var prepopulateParameter = this.parameters != null ? ((Parameters) this.parameters).getParameter(parameterName) : null;
    if (prepopulateParameter == null) {
      throw new IllegalArgumentException(String.format("Parameter not found: %s ", parameterName));
    }
    var orderId = prepopulateParameter.toString();

    var questionnaireUrl = ((CanonicalType) action.getExtensionByUrl(SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE).getValue()).getValue();

    if (questionnaireUrl.contains("$")) {
      var urlSplit = questionnaireUrl.split("$");
      IGenericClient client = Clients.forUrl(fhirContext, urlSplit[0]);
      // Clients.registerBasicAuth(client, user, password);
      try {
        bundle = client.operation().onInstance(new IdType("Patient"))
                .named("$" + urlSplit[1])
                .withParameters(new Parameters().addParameter("order", orderId))
                .returnResourceType(Bundle.class)
                .execute();
      } catch (Exception e) {
        logger.error("Error encountered calling $Questionnaire-for-Order operation: %s", e);
      }
    } else {
      var questionnaires = this.fhirDal.searchByUrl("Questionnaire", questionnaireUrl);
      var iterator = questionnaires.iterator();
      if (!iterator.hasNext()) {
        throw new FHIRException("No questionnaire found for definition: " + questionnaireUrl);
      }
      var questionnaire = iterator.next();
      if (questionnaire != null) {
        bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource((Resource) questionnaire));
      }
    }

    if (bundle == null) {
      bundle = new Bundle();
    }

    return Collections.singletonList(bundle);
  }

  private void resolveDynamicValues(
          PlanDefinition planDefinition, RequestGroup requestGroup,
          PlanDefinition.PlanDefinitionActionComponent action) {
    action.getDynamicValue().forEach(
            dynamicValue -> {
              if (dynamicValue.hasExpression()) {
                String altLanguage = null;
                String altExpression = null;
                String altPath = null;
                if (dynamicValue.hasExtension(ALT_EXPRESSION_EXT)) {
                  Extension altExtension = dynamicValue.getExtensionByUrl(ALT_EXPRESSION_EXT);
                  if (altExtension.hasValue() && altExtension.getValue() instanceof Expression) {
                    Expression altExpressionRes = (Expression) altExtension.getValue();
                    if (altExpressionRes.hasExpression()) {
                      altLanguage = altExpressionRes.getLanguage();
                      altExpression = altExpressionRes.getExpression();
                      altPath = dynamicValue.getPath();
                    }
                  }
                }
                resolveDynamicValue(dynamicValue.getExpression().getLanguage(),
                        dynamicValue.getExpression().getExpression(), dynamicValue.getPath(),
                        altLanguage, altExpression, altPath, planDefinition.getLibrary().get(0).getValueAsString(),
                        requestGroup, resolveInputParameters(action.getInput()));
              }
            }
    );
  }

  private Boolean meetsConditions(PlanDefinition planDefinition, RequestGroup requestGroup,
      PlanDefinition.PlanDefinitionActionComponent action) {
    // Should we be resolving child actions regardless of whether the conditions are met?
    if (action.hasAction()) {
      for (var containedAction : action.getAction()) {
        var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();
        resolveAction(planDefinition, requestGroup, metConditions, containedAction);
      }
    }
    if (planDefinition.getType().hasCoding()) {
      var planDefinitionTypeCoding = planDefinition.getType().getCoding();
      for (var coding : planDefinitionTypeCoding) {
        if (coding.getCode().equals("workflow-definition")) {
//           logger.info("Found a workflow definition type for PlanDefinition {} conditions should be evaluated at task execution time.", planDefinition.getUrl());
          return true;
        }
      }
    }
    for (var condition : action.getCondition()) {
      if (condition.hasExpression()) {
        String altLanguage = null;
        String altExpression = null;
        if (condition.hasExtension(ALT_EXPRESSION_EXT)) {
          Extension altExtension = condition.getExtensionByUrl(ALT_EXPRESSION_EXT);
          if (altExtension.hasValue() && altExtension.getValue() instanceof Expression) {
            Expression altExpressionRes = (Expression) altExtension.getValue();
            if (altExpressionRes.hasExpression()) {
              altLanguage = altExpressionRes.getLanguage();
              altExpression = altExpressionRes.getExpression();
            }
          }
        }
        var result = resolveCondition(condition.getExpression().getLanguage(),
                condition.getExpression().getExpression(), altLanguage, altExpression,
                planDefinition.getLibrary().get(0).getValueAsString(),
                resolveInputParameters(action.getInput()));
        if (result == null) {
          logger.warn("Condition expression {} returned null", condition.getExpression());
          return false;
        }
        if (!(result instanceof BooleanType)) {
          logger.warn("The condition expression {} returned a non-boolean value: {}",
                  condition.getExpression(), result.getClass().getSimpleName());
          continue;
        }
        if (!((BooleanType) result).booleanValue()) {
          logger.info("The result of condition expression {} is false", condition.getExpression());
          return false;
        }
      }
    }
    return true;
  }

  protected String resolveResourceName(CanonicalType canonical, MetadataResource resource) {
    if (canonical.hasValue()) {
      var id = canonical.getValue();
      if (id.contains("/")) {
        id = id.replace(id.substring(id.lastIndexOf("/")), "");
        return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
      }
      else if (id.startsWith("#")){
        return resolveContained(resource, id).getResourceType().name();
      }
      return null;
    }

    throw new FHIRException("CanonicalType must have a value for resource name extraction");
  }

  private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    if (dataRequirements == null)
      return new Parameters();

    var params = new Parameters();
    for (var req : dataRequirements) {
      var resources = fhirDal.search(req.getType()).iterator();

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
