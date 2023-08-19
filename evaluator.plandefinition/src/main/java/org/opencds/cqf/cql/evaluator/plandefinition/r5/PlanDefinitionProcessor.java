package org.opencds.cqf.cql.evaluator.plandefinition.r5;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.r5.SearchHelper.searchRepositoryByCanonical;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CarePlan;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Element;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Enumerations.RequestIntent;
import org.hl7.fhir.r5.model.Enumerations.RequestStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Goal;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionInputComponent;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RequestOrchestration;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.Task;
import org.hl7.fhir.r5.model.UrlType;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.cql.evaluator.activitydefinition.r5.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.util.Clients;
import org.opencds.cqf.cql.evaluator.library.CqfExpression;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.plandefinition.BasePlanDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.r5.QuestionnaireItemGenerator;
import org.opencds.cqf.cql.evaluator.questionnaire.r5.QuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.r5.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.IElement;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@SuppressWarnings({"unused", "squid:S107"})
public class PlanDefinitionProcessor extends BasePlanDefinitionProcessor<PlanDefinition> {

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);

  private final ActivityDefinitionProcessor activityDefinitionProcessor;
  private final QuestionnaireProcessor questionnaireProcessor;
  private final QuestionnaireResponseProcessor questionnaireResponseProcessor;
  private QuestionnaireItemGenerator questionnaireItemGenerator;

  public PlanDefinitionProcessor(Repository repository) {
    this(repository, EvaluationSettings.getDefault());
  }

  public PlanDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
    super(repository, evaluationSettings);
    this.activityDefinitionProcessor =
        new ActivityDefinitionProcessor(this.repository, evaluationSettings);
    this.questionnaireProcessor = new QuestionnaireProcessor(this.repository, evaluationSettings);
    this.questionnaireResponseProcessor = new QuestionnaireResponseProcessor(this.repository);
  }

  @Override
  public void extractQuestionnaireResponse() {
    if (bundle == null) {
      return;
    }

    var questionnaireResponses = ((Bundle) bundle).getEntry().stream()
        .filter(entry -> entry.getResource().fhirType()
            .equals(Enumerations.FHIRTypes.QUESTIONNAIRERESPONSE.toCode()))
        .map(entry -> (QuestionnaireResponse) entry.getResource()).collect(Collectors.toList());
    if (questionnaireResponses != null && !questionnaireResponses.isEmpty()) {
      for (var questionnaireResponse : questionnaireResponses) {
        var extractBundle = (Bundle) questionnaireResponseProcessor.extract(questionnaireResponse,
            parameters, bundle, libraryEngine);
        extractedResources.add(questionnaireResponse);
        for (var entry : extractBundle.getEntry()) {
          ((Bundle) bundle).addEntry(entry);
          extractedResources.add(entry.getResource());
        }
      }
    }
  }


  @Override
  public Bundle packagePlanDefinition(PlanDefinition thePlanDefinition, boolean theIsPut) {
    var bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);

    return bundle;
  }

  @Override
  public <C extends IPrimitiveType<String>> PlanDefinition resolvePlanDefinition(IIdType theId,
      C theCanonical, IBaseResource thePlanDefinition) {
    var basePlanDefinition = thePlanDefinition;
    if (basePlanDefinition == null) {
      basePlanDefinition = theId != null ? this.repository.read(PlanDefinition.class, theId)
          : searchRepositoryByCanonical(repository, theCanonical);
    }

    requireNonNull(basePlanDefinition, "Couldn't find PlanDefinition " + theId);

    var planDefinition = castOrThrow(basePlanDefinition, PlanDefinition.class,
        "The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class")
            .orElse(null);

    logger.info("Performing $apply operation on {}", planDefinition.getIdElement());

    this.questionnaire = new Questionnaire();
    this.questionnaire
        .setId(new IdType(FHIRTypes.QUESTIONNAIRE.toCode(), planDefinition.getIdPart()));
    this.questionnaireItemGenerator =
        new QuestionnaireItemGenerator(repository, patientId, parameters, bundle, libraryEngine);

    return planDefinition;
  }

  @Override
  public IBaseResource applyPlanDefinition(PlanDefinition planDefinition) {
    // Each Group of actions shares a RequestOrchestration
    var requestGroup = new RequestOrchestration().setStatus(RequestStatus.DRAFT)
        .setIntent(RequestIntent.PROPOSAL)
        .addInstantiatesCanonical(planDefinition.getUrl()).setSubject(new Reference(patientId));

    requestGroup
        .setId(new IdType(requestGroup.fhirType(), planDefinition.getIdElement().getIdPart()));
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
      goal.setIdElement(new IdType("Goal", String.valueOf(i + 1)));
      requestGroup.addExtension().setUrl(Constants.PERTAINS_TO_GOAL)
          .setValue(new Reference(goal.getIdElement()));
      requestResources.add(goal);
    }

    // Create Questionnaire for the RequestOrchestration if using Modular Questionnaires.
    // Assuming Dynamic until a use case for modular arises

    var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();

    var defaultLibraryUrl =
        planDefinition.getLibrary() == null || planDefinition.getLibrary().isEmpty() ? null
            : planDefinition.getLibrary().get(0).getValue();
    for (var action : planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      requestGroup.addAction(
          resolveAction(defaultLibraryUrl, planDefinition, requestGroup, metConditions, action));
    }

    return requestGroup;
  }

  @Override
  public CarePlan transformToCarePlan(IBaseResource rg) {
    return null;
  }

  @Override
  public IBaseResource transformToBundle(IBaseResource requestGroup) {
    var resultBundle = new Bundle().setType(BundleType.COLLECTION);
    resultBundle.setId(requestGroup.getIdElement().getIdPart());
    resultBundle.addEntry().setResource((Resource) requestGroup);
    for (var resource : requestResources) {
      resultBundle.addEntry().setResource((Resource) resource);
    }
    for (var resource : extractedResources) {
      resultBundle.addEntry().setResource((Resource) resource);
    }
    if (((Questionnaire) this.questionnaire).hasItem()) {
      resultBundle.addEntry().setResource((Resource) this.questionnaire);
    }

    return resultBundle;
  }

  @Override
  public void resolveDynamicExtension(IElement requestAction, IBase resource, Object value,
      String path) {
    if (path.equals("activity.extension") || path.equals("action.extension")) {
      // default to adding extension to last action
      ((Element) requestAction).addExtension().setValue((DataType) value);
    }
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

  private RequestOrchestrationActionComponent resolveAction(String defaultLibraryUrl,
      PlanDefinition planDefinition, RequestOrchestration requestGroup,
      Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if (planDefinition.hasExtension(Constants.CPG_QUESTIONNAIRE_GENERATE) && action.hasInput()) {
      for (var actionInput : action.getInput()) {
        if (actionInput.hasRequirement() && actionInput.getRequirement().hasProfile()) {
          ((Questionnaire) this.questionnaire).addItem(this.questionnaireItemGenerator.generateItem(
              actionInput.getRequirement(), ((Questionnaire) this.questionnaire).getItem().size()));
        }
      }
    }

    if (Boolean.TRUE.equals(meetsConditions(defaultLibraryUrl, action))) {
      // TODO: Figure out why this was here and what it was trying to do
      // if (action.hasRelatedAction()) {
      // for (var relatedActionComponent : action.getRelatedAction()) {
      // if (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER)
      // && metConditions.containsKey(relatedActionComponent.getActionId())) {
      // metConditions.put(action.getId(), action);
      // resolveDefinition(planDefinition, requestGroup, action);
      // resolveDynamicValues(planDefinition, requestGroup, action);
      // }
      // }
      // }
      metConditions.put(action.getId(), action);
      var requestAction = createRequestAction(action);
      if (action.hasAction()) {
        for (var containedAction : action.getAction()) {
          requestAction.addAction(
              resolveAction(defaultLibraryUrl, planDefinition, requestGroup, metConditions,
                  containedAction));
        }
      }
      IBaseResource resource = null;
      if (action.hasDefinitionCanonicalType()) {
        resource = resolveDefinition(planDefinition, requestGroup, action);
        if (resource != null) {
          applyAction(requestGroup, resource, action);
          requestAction.setResource(new Reference(resource.getIdElement()));
          requestResources.add(resource);
        }
      } else if (action.hasDefinitionUriType()) {
        var definition = action.getDefinitionUriType();
        requestAction.setResource(new Reference(definition.asStringValue()));
      }
      resolveDynamicValues(defaultLibraryUrl, requestAction, resource, action);

      return requestAction;
    }

    return null;
  }

  private RequestOrchestrationActionComponent createRequestAction(
      PlanDefinitionActionComponent action) {
    var requestAction = new RequestOrchestrationActionComponent().setTitle(action.getTitle())
        .setDescription(action.getDescription()).setTextEquivalent(action.getTextEquivalent())
        .setCode(Collections.singletonList(action.getCode()))
        .setDocumentation(action.getDocumentation()).setTiming(action.getTiming());
    requestAction.setId(action.getId());

    return requestAction;
  }

  private IBaseResource resolveDefinition(PlanDefinition planDefinition,
      RequestOrchestration requestGroup,
      PlanDefinition.PlanDefinitionActionComponent action) {
    logger.debug("Resolving definition {}", action.getDefinitionCanonicalType().getValue());
    var definition = action.getDefinitionCanonicalType();
    var resourceName = resolveResourceName(definition, planDefinition);
    switch (FHIRTypes.fromCode(requireNonNull(resourceName))) {
      case PLANDEFINITION:
        return applyNestedPlanDefinition(requestGroup, definition);
      case ACTIVITYDEFINITION:
        return applyActivityDefinition(planDefinition, definition);
      case QUESTIONNAIRE:
        return applyQuestionnaireDefinition(planDefinition, definition);
      default:
        throw new FHIRException(String.format("Unknown action definition: %s", definition));
    }
  }

  private IBaseResource applyQuestionnaireDefinition(PlanDefinition planDefinition,
      CanonicalType definition) {
    IBaseResource result = null;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        result = resolveContained(planDefinition, definition.getValue());
      } else {
        result = searchRepositoryByCanonical(repository, definition);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}",
          definition, e.toString());
    }

    return result;
  }

  private IBaseResource applyActivityDefinition(PlanDefinition planDefinition,
      CanonicalType definition) {
    IBaseResource result = null;
    try {
      var referenceToContained = definition.getValue().startsWith("#");
      var activityDefinition = (ActivityDefinition) (referenceToContained
          ? resolveContained(planDefinition, definition.getValue())
          : searchRepositoryByCanonical(repository, definition));
      result = this.activityDefinitionProcessor.apply(activityDefinition, patientId, encounterId,
          practitionerId, organizationId, userType, userLanguage, userTaskContext, setting,
          settingContext, parameters, libraryEngine);
      result.setId(referenceToContained
          ? new IdType(result.fhirType(), activityDefinition.getIdPart().replaceFirst("#", ""))
          : activityDefinition.getIdElement().withResourceType(result.fhirType()));
    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}",
          definition, e.toString());
    }

    return result;
  }

  private IBaseResource applyNestedPlanDefinition(RequestOrchestration requestGroup,
      CanonicalType definition) {
    var planDefinition = (PlanDefinition) searchRepositoryByCanonical(repository, definition);
    var result = (RequestOrchestration) applyPlanDefinition(planDefinition);

    for (var c : result.getInstantiatesCanonical()) {
      requestGroup.addInstantiatesCanonical(c.getValueAsString());
    }

    return result;
  }

  private void applyAction(RequestOrchestration requestGroup, IBaseResource result,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if ("Task".equals(result.fhirType())) {
      resolveTask(requestGroup, (Task) result, action);
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon period periodUnit
   * frequency count ) use task code
   */
  private void resolveTask(RequestOrchestration requestGroup, Task task,
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
        if (conditionComponent.hasExtension(Constants.ALT_EXPRESSION_EXT)) {
          condition
              .addExtension(conditionComponent.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT));
        }
        task.addExtension(condition);
      }
    }

    if (action.hasInput()) {
      var dataRequirements = action.getInput().stream()
          .map(PlanDefinition.PlanDefinitionActionInputComponent::getRequirement)
          .collect(Collectors.toList());
      for (var dataRequirement : dataRequirements) {
        var input = new Extension();
        input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
        input.setValue(dataRequirement);
        task.addExtension(input);
      }
    }

    task.addBasedOn(new Reference(requestGroup).setType(requestGroup.fhirType()));
    task.setFor(requestGroup.getSubject());

    resolvePrepopulateAction(action, requestGroup, task);
  }

  private void resolvePrepopulateAction(PlanDefinition.PlanDefinitionActionComponent action,
      RequestOrchestration requestGroup, Task task) {
    if (action.hasExtension(Constants.SDC_QUESTIONNAIRE_PREPOPULATE)) {
      var questionnaireBundles = getQuestionnairePackage(
          action.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE));
      for (var questionnaireBundle : questionnaireBundles) {
        var toPopulate = (Questionnaire) questionnaireBundle.getEntryFirstRep().getResource();
        // Each bundle should contain a Questionnaire and supporting Library and ValueSet
        // resources
        var libraries = questionnaireBundle.getEntry().stream()
            .filter(e -> e.hasResource()
                && (e.getResource().fhirType().equals(Enumerations.FHIRTypes.LIBRARY.toCode())))
            .map(e -> (Library) e.getResource()).collect(Collectors.toList());
        var valueSets = questionnaireBundle.getEntry().stream()
            .filter(e -> e.hasResource()
                && (e.getResource().fhirType().equals(Enumerations.FHIRTypes.VALUESET.toCode())))
            .map(e -> (ValueSet) e.getResource()).collect(Collectors.toList());
        var additionalData =
            bundle == null ? new Bundle().setType(BundleType.COLLECTION) : ((Bundle) bundle).copy();
        libraries.forEach(library -> additionalData
            .addEntry(new Bundle.BundleEntryComponent().setResource(library)));
        valueSets.forEach(valueSet -> additionalData
            .addEntry(new Bundle.BundleEntryComponent().setResource(valueSet)));

        var populatedQuestionnaire =
            questionnaireProcessor.prePopulate(toPopulate, patientId, this.parameters,
                additionalData, libraryEngine);
        requestResources.add(populatedQuestionnaire);
        task.setFocus(new Reference(
            new IdType(FHIRTypes.QUESTIONNAIRE.toCode(), populatedQuestionnaire.getIdPart())));
        task.setFor(requestGroup.getSubject());
      }
    }
  }

  private List<Bundle> getQuestionnairePackage(Extension prepopulateExtension) {
    Bundle bundle = null;
    // PlanDef action should provide endpoint for $questionnaire-for-order operation as well as
    // the order id to pass
    var parameterExtension =
        prepopulateExtension.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER);
    if (parameterExtension == null) {
      throw new IllegalArgumentException(String.format("Required extension for %s not found.",
          Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER));
    }
    var parameterName = parameterExtension.getValue().toString();
    var prepopulateParameter =
        this.parameters != null ? ((Parameters) this.parameters).getParameter(parameterName) : null;
    if (prepopulateParameter == null) {
      throw new IllegalArgumentException(String.format("Parameter not found: %s ", parameterName));
    }
    var orderId = prepopulateParameter.toString();

    var questionnaireExtension =
        prepopulateExtension.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE);
    if (questionnaireExtension == null) {
      throw new IllegalArgumentException(String.format("Required extension for %s not found.",
          Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE));
    }

    if (questionnaireExtension.getValue().hasType(FHIRTypes.CANONICAL.toCode())) {
      var questionnaire = searchRepositoryByCanonical(repository,
          (CanonicalType) questionnaireExtension.getValue());
      if (questionnaire != null) {
        bundle =
            new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(questionnaire));
      }
    } else if (questionnaireExtension.getValue().hasType(FHIRTypes.URL.toCode())) {
      // Assuming package operation endpoint if the extension is using valueUrl instead of
      // valueCanonical
      bundle = callQuestionnairePackageOperation(
          ((UrlType) questionnaireExtension.getValue()).getValueAsString());
    }

    if (bundle == null) {
      bundle = new Bundle();
    }

    return Collections.singletonList(bundle);
  }

  private Bundle callQuestionnairePackageOperation(String url) {
    String baseUrl = null;
    String operation = null;
    if (url.contains("$")) {
      var urlSplit = url.split("$");
      baseUrl = urlSplit[0];
      operation = urlSplit[1];
    } else {
      baseUrl = url;
      operation = "questionnaire-package";
    }

    Bundle bundle = null;
    IGenericClient client = Clients.forUrl(repository.fhirContext(), baseUrl);
    // Clients.registerBasicAuth(client, user, password);
    try {
      // TODO: This is not currently in use, but if it ever is we will need to determine how the
      // order and coverage resources are passed in
      DataType order = null;
      DataType coverage = null;
      bundle = client.operation().onType(FHIRTypes.QUESTIONNAIRE.toCode()).named('$' + operation)
          .withParameters(
              new Parameters().addParameter("order", order).addParameter("coverage", coverage))
          .returnResourceType(Bundle.class).execute();
    } catch (Exception e) {
      logger.error("Error encountered calling $questionnaire-package operation: %s", e);
    }

    return bundle;
  }

  private CqfExpression getCqfExpression(Expression expression, String defaultLibraryUrl,
      Extension altExtension) {
    var result = new CqfExpression().setExpression(expression.getExpression())
        .setLanguage(expression.getLanguage())
        .setLibraryUrl(expression.hasReference() ? expression.getReference() : defaultLibraryUrl);

    if (altExtension != null && altExtension.hasValue()
        && altExtension.getValue() instanceof Expression) {
      final var altExpressionRes = (Expression) altExtension.getValue();
      if (altExpressionRes.hasExpression()) {
        result.setAltLanguage(altExpressionRes.getLanguage())
            .setAltExpression(altExpressionRes.getExpression())
            .setAltLibraryUrl(altExpressionRes.hasReference() ? altExpressionRes.getReference()
                : defaultLibraryUrl);
      }
    }
    return result;
  }

  private void resolveDynamicValues(String defaultLibraryUrl, IElement requestAction,
      IBase resource, PlanDefinition.PlanDefinitionActionComponent action) {
    action.getDynamicValue().forEach(dynamicValue -> {
      if (dynamicValue.hasExpression()) {
        Parameters inputParams = resolveInputParameters(action.getInput());
        if (parameters != null) {
          inputParams.getParameter().addAll(((Parameters) parameters).getParameter());
        }
        List<IBase> result = null;
        try {
          result =
              resolveExpression(getCqfExpression(dynamicValue.getExpression(), defaultLibraryUrl,
                  dynamicValue.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT)), inputParams);
          resolveDynamicValue(result, dynamicValue.getPath(), requestAction, resource);
        } catch (Exception e) {
          var message = String.format("DynamicValue expression %s encountered exception: %s",
              dynamicValue.getExpression().getExpression(), e.getMessage());
          logger.error(message);
        }
      }
    });
  }

  private Boolean meetsConditions(String defaultLibraryUrl,
      PlanDefinition.PlanDefinitionActionComponent action) {
    for (var condition : action.getCondition()) {
      if (condition.hasExpression()) {
        Parameters inputParams = resolveInputParameters(action.getInput());
        if (parameters != null) {
          inputParams.getParameter().addAll(((Parameters) parameters).getParameter());
        }
        IBase result = null;
        try {
          var results =
              resolveExpression(getCqfExpression(condition.getExpression(), defaultLibraryUrl,
                  condition.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT)), inputParams);
          result = results == null || results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
          var message = String.format("Condition expression %s encountered exception: %s",
              condition.getExpression(), e.getMessage());
          logger.error(message);
        }
        if (result == null) {
          logger.warn("Condition expression {} returned null",
              condition.getExpression().getExpression());
          return false;
        }
        if (!(result instanceof BooleanType)) {
          logger.warn("The condition expression {} returned a non-boolean value: {}",
              condition.getExpression().getExpression(), result.getClass().getSimpleName());
          continue;
        }
        if (!((BooleanType) result).booleanValue()) {
          logger.debug("The result of condition expression {} is false",
              condition.getExpression().getExpression());
          return false;
        }
        logger.debug("The result of condition expression {} is true",
            condition.getExpression().getExpression());
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
      } else if (id.startsWith("#")) {
        return resolveContained(resource, id).getResourceType().name();
      }
      return null;
    }

    throw new FHIRException("CanonicalType must have a value for resource name extraction");
  }

  private Parameters resolveInputParameters(List<PlanDefinitionActionInputComponent> inputs) {
    if (inputs == null)
      return new Parameters();

    var params = new Parameters();
    for (var input : inputs) {
      var req = input.getRequirement();
      if (req == null || !req.hasId()) {
        continue;
      }

      var resources = repository.search(Bundle.class, IBaseResource.class, Searches.ALL);

      if (resources.hasEntry()) {
        var found = true;
        for (var resource : resources.getEntry().stream().map(e -> e.getResource())
            .collect(Collectors.toList())) {
          var parameter =
              new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
          if (req.hasCodeFilter()) {
            for (var filter : req.getCodeFilter()) {
              var codeFilterParam = new Parameters();
              codeFilterParam.addParameter().setName("%resource").setResource(resource);
              if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                var valueSets = repository.search(Bundle.class, ValueSet.class,
                    Searches.byUrl(filter.getValueSet()));
                if (valueSets.hasEntry()) {
                  codeFilterParam.addParameter().setName("%valueset")
                      .setResource(valueSets.getEntryFirstRep().getResource());
                  var codeFilterExpression =
                      "%" + String.format("resource.%s.where(code.memberOf('%s'))",
                          filter.getPath(), "%valueset");
                  var codeFilterResult =
                      expressionEvaluator.evaluate(codeFilterExpression, codeFilterParam);
                  var tempResult =
                      operationParametersParser.getValueChild((codeFilterResult), "return");
                  if (tempResult instanceof BooleanType) {
                    found = ((BooleanType) tempResult).booleanValue();
                  }
                }
                logger.debug("Could not find ValueSet with url {} on the local server.",
                    filter.getValueSet());
              }
            }
          }
          if (resources.getEntry().size() == 1) {
            parameter.addExtension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                new ParameterDefinition().setMax("*").setName("%" + req.getId()));
            if (found) {
              parameter.setResource(resource);
            }
          } else {
            if (!found) {
              continue;
            }
            parameter.setResource(resource);
          }
          params.addParameter(parameter);
        }
      } else {
        var parameter =
            new ParametersParameterComponent().setName("%" + String.format("%s", req.getId()));
        parameter.addExtension(
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
            new ParameterDefinition().setMax("*").setName("%" + req.getId()));
        params.addParameter(parameter);
      }
    }
    return params;
  }

  protected Resource resolveContained(DomainResource resource, String id) {
    var first = resource.getContained().stream().filter(Resource::hasIdElement)
        .filter(x -> x.getIdElement().getIdPart().equals(id)).findFirst();
    return first.orElse(null);
  }
}
