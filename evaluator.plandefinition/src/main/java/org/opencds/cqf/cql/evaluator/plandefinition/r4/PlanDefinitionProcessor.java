package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestIntent;
import org.hl7.fhir.r4.model.RequestGroup.RequestStatus;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.helper.ContainedHelper;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;

@SuppressWarnings("unused")
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

  public PlanDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor, ExpressionEvaluator expressionEvaluator,
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

  public CarePlan apply(IdType theId, String patientId, String encounterId, String practitionerId,
      String organizationId, String userType, String userLanguage, String userTaskContext, String setting,
      String settingContext, Boolean mergeNestedCarePlans, IBaseParameters parameters, Boolean useServerData,
      IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {

    // warn if prefetchData exists
    // if no data anywhere blow up
    IBaseResource basePlanDefinition = this.fhirDal.read(theId);
    PlanDefinition planDefinition;

    if (basePlanDefinition == null) {
      throw new IllegalArgumentException("Couldn't find PlanDefinition " + theId);
    }
    if (!(basePlanDefinition instanceof PlanDefinition)) {
      throw new IllegalArgumentException(
          "The planDefinition passed to FhirDal was " + "not a valid instance of PlanDefinition.class");
    }

    planDefinition = (PlanDefinition) basePlanDefinition;

    logger.info("Performing $apply operation on PlanDefinition/" + theId);

    CarePlan builder = new CarePlan();

    builder.addInstantiatesCanonical(planDefinition.getIdElement().getIdPart()).setSubject(new Reference(patientId))
        .setStatus(CarePlan.CarePlanStatus.DRAFT);

    if (encounterId != null)
      builder.setEncounter(new Reference(encounterId));
    if (practitionerId != null)
      builder.setAuthor(new Reference(practitionerId));
    if (organizationId != null)
      builder.setAuthor(new Reference(organizationId));
    if (userLanguage != null)
      builder.setLanguage(userLanguage);

    // Each Group of actions shares a RequestGroup
    RequestGroup requestGroup = new RequestGroup().setStatus(RequestStatus.DRAFT).setIntent(RequestIntent.PROPOSAL);

    IBaseDatatype basePrefetchDataKey = operationParametersParser.getValueChild(prefetchData, "key");
    String prefetchDataKey = null;
    if (basePrefetchDataKey != null) {
      if (!(basePrefetchDataKey instanceof StringType)) {
        throw new IllegalArgumentException("prefetchData key must be a String");
      }
      prefetchDataKey = ((StringType) basePrefetchDataKey).asStringValue();
    }

    IBaseResource basePrefetchDataDescription = operationParametersParser.getResourceChild(prefetchData, "descriptor");
    DataRequirement prefetchDataDescription = null;
    if (basePrefetchDataDescription != null) {
      if (!(basePrefetchDataDescription instanceof DataRequirement)) {
        throw new IllegalArgumentException("prefetchData descriptor must be a DataRequirement");
      }
      prefetchDataDescription = ((DataRequirement) basePrefetchDataDescription);
    }

    IBaseResource basePrefetchDataData = operationParametersParser.getResourceChild(prefetchData, "data");
    IBaseBundle prefetchDataData = null;
    if (basePrefetchDataData != null) {
      if (!(basePrefetchDataData instanceof IBaseBundle)) {
        throw new IllegalArgumentException("prefetchData data must be a Bundle");
      }
      prefetchDataData = ((IBaseBundle) basePrefetchDataData);
    }

    Session session = new Session(planDefinition, builder, patientId, encounterId, practitionerId, organizationId,
        userType, userLanguage, userTaskContext, setting, settingContext, requestGroup, parameters, prefetchData,
        contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, useServerData, mergeNestedCarePlans,
        prefetchDataData, prefetchDataDescription, prefetchDataKey);

    return (CarePlan) ContainedHelper.liftContainedResourcesToParent(resolveActions(session));
  }

  private CarePlan resolveActions(Session session) {
    Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();
    for (PlanDefinition.PlanDefinitionActionComponent action : session.planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      resolveAction(session, metConditions, action);
    }

    RequestGroup result = session.requestGroup;

    if (result.getId() == null) {
      result.setId(UUID.randomUUID().toString());
    }

    session.carePlan
        .addActivity(new CarePlan.CarePlanActivityComponent().setReference(new Reference("#" + result.getId())))
        .addContained(result);

    return session.carePlan;
  }

  private void resolveAction(Session session, Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if (meetsConditions(session, action)) {
      if (action.hasRelatedAction()) {
        for (PlanDefinitionActionRelatedActionComponent relatedActionComponent : action.getRelatedAction()) {
          if (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER)) {
            if (metConditions.containsKey(relatedActionComponent.getActionId())) {
              metConditions.put(action.getId(), action);
              resolveDefinition(session, action);
              resolveDynamicActions(session, action);
            }
          }
        }
      }
      metConditions.put(action.getId(), action);
      resolveDefinition(session, action);
      resolveDynamicActions(session, action);
    }
  }

  private void resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinitionCanonicalType()) {
      logger.debug("Resolving definition " + action.getDefinitionCanonicalType().getValue());
      CanonicalType definition = action.getDefinitionCanonicalType();
      switch (getResourceName(definition)) {
        case ("PlanDefinition"):
          applyNestedPlanDefinition(session, definition, action);
          break;
        case ("ActivityDefinition"):
          applyActivityDefinition(session, definition, action);
          break;
        case ("Questionnaire"):
          throw new NotImplementedException("Questionnaire definition evaluation is not yet implemented.");
        default:
          throw new RuntimeException(String.format("Unknown action definition: ", definition));
      }
    } else if (action.hasDefinitionUriType()) {
      throw new NotImplementedException("Uri definition evaluation is not yet implemented");
    }
  }

  private void applyActivityDefinition(Session session, CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    IBaseResource result;
    try {
      boolean referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        ActivityDefinition activityDefinition = (ActivityDefinition) resolveContained(session.planDefinition, definition.getValue());
        result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition, session.patientId,
            session.practitionerId, session.organizationId, session.parameters, session.contentEndpoint,
            session.terminologyEndpoint, session.dataEndpoint);
      } else {
        Iterator<IBaseResource> iterator = fhirDal.searchByUrl("ActivityDefinition", definition.asStringValue())
            .iterator();
        if (!iterator.hasNext()) {
          throw new RuntimeException("No activity definition found for definition: " + definition);
        }
        ActivityDefinition activityDefinition = (ActivityDefinition) iterator.next();
        result = this.activityDefinitionProcessor.apply(activityDefinition.getIdElement(), session.patientId,
            session.encounterId, session.practitionerId, session.organizationId, session.userType, session.userLanguage,
            session.userTaskContext, session.setting, session.settingContext, session.parameters,
            session.contentEndpoint, session.terminologyEndpoint, session.dataEndpoint);
      }

      if (!result.getIdElement().hasValue()) {
        logger.warn("ActivityDefinition {} returned resource with no id, setting one", definition.getId());
        result.setId(new IdType(UUID.randomUUID().toString()));
      }

      applyAction(session, result, action);
      session.requestGroup.addAction(new RequestGroup.RequestGroupActionComponent()
          .setResource(new Reference("#" + result.getIdElement().getIdPart()))).addContained((Resource) result);

    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}", definition,
          e.toString());
    }
  }

  private void applyNestedPlanDefinition(Session session, CanonicalType definition,
      PlanDefinition.PlanDefinitionActionComponent action) {
    CarePlan carePlan;
    Iterator<IBaseResource> iterator = fhirDal.searchByUrl("PlanDefinition", definition.asStringValue()).iterator();
    if (!iterator.hasNext()) {
      throw new RuntimeException("No plan definition found for definition: " + definition);
    }
    PlanDefinition planDefinition = (PlanDefinition) iterator.next();
    carePlan = apply(planDefinition.getIdElement(), session.patientId, session.encounterId,
        session.practitionerId, session.organizationId, session.userType, session.userLanguage, session.userTaskContext,
        session.setting, session.settingContext, session.mergeNestedCarePlans, session.parameters,
        session.useServerData, session.bundle, session.prefetchData, session.dataEndpoint, session.contentEndpoint,
        session.terminologyEndpoint);

    if (carePlan.getId() == null) {
      carePlan.setId(UUID.randomUUID().toString());
    }
    applyAction(session, carePlan, action);

    // Add an action to the request group which points to this CarePlan
    session.requestGroup
        .addAction(new RequestGroup.RequestGroupActionComponent().setResource(new Reference("#" + carePlan.getId())))
        .addContained(carePlan);

    for (CanonicalType c : carePlan.getInstantiatesCanonical()) {
      session.carePlan.addInstantiatesCanonical(c.getValueAsString());
    }
  }

  private void applyAction(Session session, IBaseResource result, PlanDefinition.PlanDefinitionActionComponent action) {
    switch (result.fhirType()) {
      case "Task":
        result = resolveTask(session, (Task) result, action);
        break;
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon
   * period periodUnit frequency count ) use task code
   */
  private Resource resolveTask(Session session, Task task, PlanDefinition.PlanDefinitionActionComponent action) {
    task.setId(new IdType(action.getId()));
    if (!task.hasCode()) {
      if (action.hasCode()) {
        for (CodeableConcept actionCode : action.getCode()) {
          Boolean foundExecutableTaskCode = false;
          for (Coding actionCoding : actionCode.getCoding()) {
            if (actionCoding.getSystem().equals("http://aphl.org/fhir/ecr/CodeSystem/executable-task-types")) {
              foundExecutableTaskCode = true;
            }
          }
        }
      }
    }
    if (action.hasRelatedAction()) {
        List<PlanDefinitionActionRelatedActionComponent> relatedActions = action.getRelatedAction();
        for (PlanDefinitionActionRelatedActionComponent relatedAction : relatedActions) {
          Extension next = new Extension();
          next.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/next");
          if (relatedAction.hasOffset()) {
              Extension offsetExtension = new Extension();
              offsetExtension.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/offset");
              offsetExtension.setValue(relatedAction.getOffset());
              next.addExtension(offsetExtension);
          }
          Extension target = new Extension();
          Reference targetRef = new Reference("#" + relatedAction.getActionId());
          target.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/target");
          target.setValue(targetRef);
          next.addExtension(target);
          task.addExtension(next);
        }
    }

    if (action.hasCondition()) {
      List<PlanDefinitionActionConditionComponent> conditionComponents = action.getCondition();
      for (PlanDefinitionActionConditionComponent conditionComponent : conditionComponents) {
        Extension condition = new Extension();
        condition.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/condition");
        condition.setValue(conditionComponent.getExpression());
        if (conditionComponent.hasExtension(alternateExpressionExtension)) {
          condition.addExtension(conditionComponent.getExtensionByUrl(alternateExpressionExtension));
        }
        task.addExtension(condition);
      }
    }

    if (action.hasInput()) {
      List<DataRequirement> dataRequirements = action.getInput();
      for (DataRequirement dataRequirement : dataRequirements) {
        Extension input = new Extension();
        input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
        input.setValue(dataRequirement);
        task.addExtension(input);
      }
    }
    task.addBasedOn(new Reference(session.requestGroup).setType(session.requestGroup.fhirType()));
    return task;
  }

  private void resolveDynamicActions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
      logger.info("Resolving dynamic value {} {}", dynamicValue.getPath(), dynamicValue.getExpression());

      ensureDynamicValueExpression(dynamicValue);
      if (dynamicValue.getExpression().hasLanguage()) {

        String libraryToBeEvaluated = ensureLibrary(session, dynamicValue.getExpression());
        String language = dynamicValue.getExpression().getLanguage();
        Object result = evaluateConditionOrDynamicValue(dynamicValue.getExpression().getExpression(), language, libraryToBeEvaluated, session, action.getInput());
        if (result == null && dynamicValue.getExpression().hasExtension(alternateExpressionExtension)) {
          Type alternateExpressionValue = dynamicValue.getExpression().getExtensionByUrl(alternateExpressionExtension)
              .getValue();
          if (!(alternateExpressionValue instanceof Expression)) {
            throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
          }
          Expression alternateExpression = (Expression) alternateExpressionValue;
          if (alternateExpression.hasLanguage()) {
            libraryToBeEvaluated = ensureLibrary(session, alternateExpression);
            language = alternateExpression.getLanguage();
            result = evaluateConditionOrDynamicValue(alternateExpression.getExpression(), language, libraryToBeEvaluated, session, action.getInput());
          }
        }
        // TODO: Rename bundle
        if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this")) {
          session.carePlan = ((CarePlan) result);
        } else {

          // TODO - likely need more date transformations
          if (result instanceof DateTime) {
            result = Date.from(((DateTime) result).getDateTime().toInstant());
          }

          try {
            session.carePlan.setProperty(dynamicValue.getPath(), (Base) result);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Could not set path %s to value: %s", dynamicValue.getPath(), result));
          }
        }
      }
    }
  }

  private Boolean meetsConditions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasAction()) {
      for (PlanDefinition.PlanDefinitionActionComponent containedAction : action.getAction()) {
        Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();
        resolveAction(session, metConditions, containedAction);
      }
    }
    if (session.planDefinition.getType().hasCoding()) {
      List<Coding> planDefinitionTypeCoding = session.planDefinition.getType().getCoding();
      for (Coding coding : planDefinitionTypeCoding) {
        if (coding.getCode().equals("workflow-definition")) {
          // logger.info(String.format("Found a workflow definition type for PlanDefinition % conditions should be evaluated at task execution time."), session.planDefinition.getUrl());
          return true;
        }
      }
    }
    for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
      ensureConditionExpression(condition);
      if (condition.getExpression().hasLanguage()) {

        String libraryToBeEvaluated = ensureLibrary(session, condition.getExpression());
        String language = condition.getExpression().getLanguage();
        IVersionSpecificBundleFactory bundleFactory = fhirContext.newBundleFactory();
        Object result = evaluateConditionOrDynamicValue(condition.getExpression().getExpression(), language, libraryToBeEvaluated, session, action.getInput());
        if (result == null && condition.getExpression().hasExtension(alternateExpressionExtension)) {
          Type alternateExpressionValue = condition.getExpression().getExtensionByUrl(alternateExpressionExtension)
              .getValue();
          if (!(alternateExpressionValue instanceof Expression)) {
            throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
          }
          Expression alternateExpression = (Expression) alternateExpressionValue;
          if (alternateExpression.hasLanguage()) {
            libraryToBeEvaluated = ensureLibrary(session, alternateExpression);
            language = alternateExpression.getLanguage();
            result = evaluateConditionOrDynamicValue(alternateExpression.getExpression(), language, libraryToBeEvaluated, session, action.getInput());
          }
        }
        if (result == null) {
          logger.warn("Expression Returned null");
          return false;
        }

        if (!(result instanceof BooleanType)) {
          logger.warn("The condition returned a non-boolean value: " + result.getClass().getSimpleName());
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

  protected String ensureLibrary(Session session, Expression expression) {
    if (expression.hasReference()) {
      return expression.getReference();
    }
    logger.warn(String.format("No library reference for expression: %s", expression.getExpression()));
    if (session.planDefinition.getLibrary().size() == 1) {
      return session.planDefinition.getLibrary().get(0).asStringValue();
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

  private String ensureStringResult(Object result) {
    if (!(result instanceof StringType))
      throw new RuntimeException("Result not instance of String");
    return ((StringType) result).asStringValue();
  }

  protected static String getResourceName(CanonicalType canonical) {
    if (canonical.hasValue()) {
      String id = canonical.getValue();
      if (id.contains("/")) {
        id = id.replace(id.substring(id.lastIndexOf("/")), "");
        return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
      }
      return null;
    }

    throw new RuntimeException("CanonicalType must have a value for resource name extraction");
  }

  public Object getParameterComponentByName(Parameters params, String name) {
    Optional<ParametersParameterComponent> first = params.getParameter().stream().filter(x -> x.getName().equals(name)).findFirst();
    ParametersParameterComponent component = first.isPresent() ? first.get() : null;
    return component.hasValue() ? component.getValue() : component.getResource();
  }
  
  // TODO: We don't have tests for this function. 
  protected Object evaluateConditionOrDynamicValue(String expression, String language, String libraryToBeEvaluated, Session session, List<DataRequirement> dataRequirements) {
    Parameters params = resolveInputParameters(dataRequirements);
    if (session.parameters != null && session.parameters instanceof Parameters) {
      params.getParameter().addAll(((Parameters) session.parameters).getParameter());
    }

    Object result = null;
    switch (language) {
      case "text/cql":
      case "text/cql.expression":
      case "text/cql-expression": 
        result = expressionEvaluator.evaluate(expression, params);
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
        IBaseParameters resultParams = expressionEvaluator.evaluate(expression, params);
        if (resultParams instanceof Parameters) {
          result = getParameterComponentByName((Parameters) resultParams, "return");
        }
        break;
      default:
        logger.warn("An action language other than CQL was found: " + language);
    }
    if (result != null) {
      if (result instanceof Parameters) {
        result = getParameterComponentByName((Parameters) result, expression);
      }
    }
    return result;
  }

  private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    if (dataRequirements == null) return new Parameters();

    Parameters params = new Parameters();
    for (DataRequirement req : dataRequirements) {
      Iterator<IBaseResource> resources = fhirDal.search(req.getType()).iterator();
      
      if (resources != null && resources.hasNext()) {
        int index = 0;
        Boolean found = true;
        while (resources.hasNext()) {
          Resource resource = (Resource)resources.next();
          ParametersParameterComponent parameter = new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
          if (req.hasCodeFilter()) {
            for (DataRequirement.DataRequirementCodeFilterComponent filter : req.getCodeFilter()) {
              Parameters codeFilterParam = new Parameters();
              codeFilterParam.addParameter().setName("%resource").setResource(resource);
              if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                Iterable<IBaseResource> valueset = fhirDal.searchByUrl("ValueSet", filter.getValueSet());
                if (valueset != null && valueset.iterator().hasNext()) {
                  codeFilterParam.addParameter().setName("%valueset").setResource((Resource)valueset.iterator().next());
                  String codeFilterExpression = "%" + String.format("resource.%s.where(code.memberOf(\'%s\'))", filter.getPath(), "%" + "valueset");
                  IBaseParameters codeFilterResult = expressionEvaluator.evaluate(codeFilterExpression, codeFilterParam);
                  IBaseDatatype tempResult = operationParametersParser.getValueChild(((Parameters) codeFilterResult), "return");
                  if (tempResult != null && tempResult instanceof BooleanType) {
                    found = ((BooleanType)tempResult).booleanValue();
                  }
                }
                logger.debug(String.format("Could not find ValueSet with url %s on the local server.", filter.getValueSet()));
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
        ParametersParameterComponent parameter = new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
        parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%" + req.getId()));
        params.addParameter(parameter);
      }
    }
    return params;
  }

  protected Resource resolveContained(DomainResource resource, String id) {
    Optional<Resource> first = resource.getContained().stream()
        .filter(x -> x.hasIdElement())
        .filter(x -> x.getIdElement().getIdPart().equals(id))
        .findFirst();
    return first.isPresent() ? first.get() : null;
  }
}

class Session {
  public final String patientId;
  public final PlanDefinition planDefinition;
  public final String practitionerId;
  public final String organizationId;
  public final String userType;
  public final String userLanguage;
  public final String userTaskContext;
  public final String setting;
  public final String settingContext;
  public final String prefetchDataKey;
  public CarePlan carePlan;
  public final String encounterId;
  public final RequestGroup requestGroup;
  public IBaseParameters parameters, prefetchData;
  public IBaseResource contentEndpoint, terminologyEndpoint, dataEndpoint;
  public IBaseBundle bundle, prefetchDataData;
  public DataRequirement prefetchDataDescription;
  public Boolean useServerData, mergeNestedCarePlans;

  public Session(PlanDefinition planDefinition, CarePlan carePlan, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
      String setting, String settingContext, RequestGroup requestGroup, IBaseParameters parameters,
      IBaseParameters prefetchData, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint, IBaseBundle bundle, Boolean useServerData, Boolean mergeNestedCarePlans,
      IBaseBundle prefetchDataData, DataRequirement prefetchDataDescription, String prefetchDataKey) {

    this.patientId = patientId;
    this.planDefinition = planDefinition;
    this.carePlan = carePlan;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.userType = userType;
    this.userLanguage = userLanguage;
    this.userTaskContext = userTaskContext;
    this.setting = setting;
    this.settingContext = settingContext;
    this.requestGroup = requestGroup;
    this.parameters = parameters;
    this.contentEndpoint = contentEndpoint;
    this.terminologyEndpoint = terminologyEndpoint;
    this.dataEndpoint = dataEndpoint;
    this.bundle = bundle;
    this.useServerData = useServerData;
    this.mergeNestedCarePlans = mergeNestedCarePlans;
    this.prefetchDataData = prefetchDataData;
    this.prefetchDataDescription = prefetchDataDescription;
    this.prefetchData = prefetchData;
    this.prefetchDataKey = prefetchDataKey;
  }
}
