package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestIntent;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.activitydefinition.dstu3.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.helper.dstu3.ContainedHelper;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.BasePlanDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.QuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.dstu3.QuestionnaireResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

@SuppressWarnings({"unused", "squid:S107"})
public class PlanDefinitionProcessor extends BasePlanDefinitionProcessor<PlanDefinition> {
  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);
  protected ActivityDefinitionProcessor activityDefinitionProcessor;
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
    if (obj == null) return Optional.empty();
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
            .addDefinition(new Reference(planDefinition.getUrl()))
            .setSubject(new Reference(patientId));

    requestGroup.setId(new IdType(requestGroup.fhirType(), planDefinition.getIdElement().getIdPart()));
    if (encounterId != null)
      requestGroup.setContext(new Reference(encounterId));
    if (practitionerId != null)
      requestGroup.setAuthor(new Reference(practitionerId));
    if (organizationId != null)
      requestGroup.setAuthor(new Reference(organizationId));
    if (userLanguage != null)
      requestGroup.setLanguage(userLanguage);

    for (int i = 0; i < planDefinition.getGoal().size(); i++) {
      var goal = convertGoal(planDefinition.getGoal().get(i));
      requestGroup.addContained(goal);
      // Always add goals to the resource list so they can be added to the CarePlan if needed 
      requestResources.add(goal);
    }

    var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();

    for (var action : planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      resolveAction(planDefinition, requestGroup, metConditions, action);
    }

    return requestGroup;
  }

  @Override
  public CarePlan transformToCarePlan(IBaseResource rg) {
    var requestGroup = (RequestGroup) rg;

    var carePlan = new CarePlan()
            .setStatus(CarePlan.CarePlanStatus.DRAFT)
            .setIntent(CarePlan.CarePlanIntent.PROPOSAL)
            .setDefinition(requestGroup.getDefinition())
            .setSubject(requestGroup.getSubject());
    if (encounterId != null)
      carePlan.setContext(new Reference(encounterId));
    if (requestGroup.hasAuthor()) {
      carePlan.setAuthor(Collections.singletonList(requestGroup.getAuthor()));
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
    return null;
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

    var goalTarget = goal.hasTarget()
      ? goal.getTarget().stream().map(
              target -> {
                var myTarget = new Goal.GoalTargetComponent();
                myTarget.setDetail(target.getDetail());
                myTarget.setMeasure(target.getMeasure());
                myTarget.setDue(target.getDue());
                myTarget.setExtension(target.getExtension());
                return myTarget;
              }).collect(Collectors.toList()).get(0)
      : null;
    myGoal.setTarget(goalTarget);
    return myGoal;
  }

  private void resolveAction(PlanDefinition planDefinition, RequestGroup requestGroup, Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    // TODO: If action has inputs generate QuestionnaireItems
    if (Boolean.TRUE.equals(meetsConditions(planDefinition, requestGroup, action))) {
      if (action.hasRelatedAction()) {
        for (var relatedActionComponent : action.getRelatedAction()) {
          if (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER) && metConditions.containsKey(relatedActionComponent.getActionId())) {
            metConditions.put(action.getId(), action);
            resolveDefinition(planDefinition, requestGroup, action);
            resolveDynamicActions(planDefinition, requestGroup, action);
          }
        }
      }
      metConditions.put(action.getId(), action);
      resolveDefinition(planDefinition, requestGroup, action);
      resolveDynamicActions(planDefinition, requestGroup, action);
    }
  }

  private void resolveDefinition(PlanDefinition planDefinition, RequestGroup requestGroup, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinition()) {
      logger.debug("Resolving definition {}", action.getDefinition().getReference());
      var definition = action.getDefinition();
      var resourceName = getResourceName(definition);
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
    }
  }


  private void applyQuestionnaireDefinition(
          PlanDefinition planDefinition, RequestGroup requestGroup, Reference definition,
          PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getReference().startsWith("#");
      if (referenceToContained) {
        result = resolveContained(planDefinition, definition.getReference());
      } else {
        var iterator = fhirDal.searchByUrl("Questionnaire", definition.getReference()).iterator();
        if (!iterator.hasNext()) {
          throw new FHIRException("No questionnaire found for definition: " + definition);
        }
        result = iterator.next();
      }

      applyAction(requestGroup, result, action);
      requestGroup.addAction().setResource(new Reference(result.getIdElement()));
      requestGroup.addContained((Resource) result);

    } catch (Exception e) {
      e.printStackTrace();
      logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyActivityDefinition(PlanDefinition planDefinition, RequestGroup requestGroup, Reference definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      var referenceToContained = definition.getReference().startsWith("#");
      if (referenceToContained) {
        var activityDefinition = (ActivityDefinition) resolveContained(planDefinition, definition.getReference());
        result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition, patientId,
            practitionerId, organizationId);
        result.setId(activityDefinition.getIdElement().withResourceType(result.fhirType()));
      } else {
        var iterator = fhirDal.searchByUrl("ActivityDefinition", definition.getReference()).iterator();
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
      requestGroup.addContained((Resource) result);
    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyNestedPlanDefinition(RequestGroup requestGroup, Reference definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    var iterator = fhirDal.searchByUrl("PlanDefinition", definition.getReference()).iterator();
    if (!iterator.hasNext()) {
      throw new FHIRException("No plan definition found for definition: " + definition);
    }
    var planDefinition = (PlanDefinition) iterator.next();
    var result = (RequestGroup) applyPlanDefinition(planDefinition);

    applyAction(requestGroup, result, action);

    // Add an action to the request group which points to this CarePlan
    requestGroup.addAction().setResource(new Reference(result.getIdElement()));
    requestGroup.addContained(result);

    for (var c : result.getDefinition()) {
      requestGroup.addDefinition(c);
    }
  }

  private void applyAction(RequestGroup requestGroup, IBaseResource result, PlanDefinition.PlanDefinitionActionComponent action) {
    if ("Task".equals(result.fhirType())) {
      resolveTask(requestGroup, (Task) result, action);
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon
   * period periodUnit frequency count ) use task code
   */
  private Resource resolveTask(RequestGroup requestGroup, Task task, PlanDefinition.PlanDefinitionActionComponent action) {
    task.setId(new IdType(task.fhirType(), action.getId()));
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
        var language = new Extension();
        language.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/language");
        language.setValue(new StringType(conditionComponent.getLanguage()));
        condition.addExtension(language);
        var expression = new Extension();
        expression.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/expression");
        expression.setValue(new StringType(conditionComponent.getExpression()));
        condition.addExtension(expression);

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
    task.addBasedOn(new Reference(requestGroup));
    return task;
  }

  private void resolveDynamicActions(
          PlanDefinition planDefinition, RequestGroup requestGroup, 
          PlanDefinition.PlanDefinitionActionComponent action) {
    action.getDynamicValue().forEach(
            dynamicValue -> {
              if (dynamicValue.hasExpression()) {
                resolveDynamicValue(dynamicValue.getLanguage(),
                        dynamicValue.getExpression(), dynamicValue.getPath(),
                        null, null, null, planDefinition.getLibrary().get(0).getReference(),
                        requestGroup, resolveInputParameters(action.getInput()));
              }
            }
    );
  }

  private Boolean meetsConditions(PlanDefinition planDefinition, RequestGroup requestGroup, PlanDefinition.PlanDefinitionActionComponent action) {
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
        var result = resolveCondition(condition.getLanguage(),
                condition.getExpression(), null, null,
                planDefinition.getLibrary().get(0).getReference(),
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

  protected static String getResourceName(Reference reference) {
    if (reference.hasReference()) {
      var id = reference.getReference();
      if (id.contains("/")) {
        id = id.replace(id.substring(id.lastIndexOf("/")), "");
        return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
      }
      return null;
    }

    throw new FHIRException("Reference must have a value for resource name extraction");
  }
  
  private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    if (dataRequirements == null) return new Parameters();

    var params = new Parameters();
    for (var req : dataRequirements) {
      var resources = fhirDal.search(req.getType()).iterator();
      
      if (resources.hasNext()) {
        var index = 0;
        var found = true;
        while (resources.hasNext()) {
          var resource = (Resource)resources.next();
          var parameter = new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
          if (req.hasCodeFilter()) {
            for (var filter : req.getCodeFilter()) {
              var codeFilterParam = new Parameters();
              codeFilterParam.addParameter().setName("%resource").setResource(resource);
              if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                var valueset = fhirDal.searchByUrl("ValueSet", filter.getValueSet().getId());
                if (valueset != null && valueset.iterator().hasNext()) {
                  codeFilterParam.addParameter().setName("%valueset").setResource((Resource)valueset.iterator().next());
                  var codeFilterExpression = "%" + String.format("resource.%s.where(code.memberOf('%s'))", filter.getPath(), "%" + "valueset");
                  var codeFilterResult = expressionEvaluator.evaluate(codeFilterExpression, codeFilterParam);
                  var tempResult = operationParametersParser.getValueChild((codeFilterResult), "return");
                  if (tempResult instanceof BooleanType) {
                    found = ((BooleanType)tempResult).booleanValue();
                  }
                }
                logger.debug("Could not find ValueSet with url {} on the local server.", filter.getValueSet());
              }
            }
          }
          if (!resources.hasNext() && index == 0) {
            parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%" + req.getId()));
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
        parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%" + req.getId()));
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
