package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Goal;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestIntent;
import org.hl7.fhir.r4.model.RequestGroup.RequestStatus;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.helper.r4.ContainedHelper;
import org.opencds.cqf.cql.evaluator.fhir.util.Clients;
import org.opencds.cqf.cql.evaluator.plandefinition.BasePlanDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.QuestionnaireItemGenerator;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.QuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.r4.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

@SuppressWarnings({"unused", "squid:S107"})
public class PlanDefinitionProcessor extends BasePlanDefinitionProcessor<PlanDefinition> {

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);

  private final ActivityDefinitionProcessor activityDefinitionProcessor;
  private final QuestionnaireProcessor questionnaireProcessor;
  private final QuestionnaireResponseProcessor questionnaireResponseProcessor;
  private QuestionnaireItemGenerator questionnaireItemGenerator;

  public PlanDefinitionProcessor(Repository repository) {
    super(repository);
    this.activityDefinitionProcessor = new ActivityDefinitionProcessor(this.repository);
    this.questionnaireProcessor = new QuestionnaireProcessor(this.repository);
    this.questionnaireResponseProcessor = new QuestionnaireResponseProcessor(this.repository);
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

  @Override
  public void extractQuestionnaireResponse() {
    if (bundle == null) {
      return;
    }

    var questionnaireResponses = ((Bundle) bundle).getEntry().stream()
        .filter(entry -> entry.getResource().fhirType()
            .equals(Enumerations.FHIRAllTypes.QUESTIONNAIRERESPONSE.toCode()))
        .map(entry -> (QuestionnaireResponse) entry.getResource()).collect(Collectors.toList());
    if (questionnaireResponses != null && !questionnaireResponses.isEmpty()) {
      for (var questionnaireResponse : questionnaireResponses) {
        var extractBundle = (Bundle) questionnaireResponseProcessor.extract(questionnaireResponse);
        extractedResources.add(questionnaireResponse);
        for (var entry : extractBundle.getEntry()) {
          ((Bundle) bundle).addEntry(entry);
          extractedResources.add(entry.getResource());
        }
      }
    }
  }

  @Override
  public PlanDefinition resolvePlanDefinition(IIdType theId) {
    var basePlanDefinition = this.repository.read(PlanDefinition.class, theId);

    requireNonNull(basePlanDefinition, "Couldn't find PlanDefinition " + theId);

    var planDefinition = castOrThrow(basePlanDefinition, PlanDefinition.class,
        "The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class")
            .orElse(null);

    logger.info("Performing $apply operation on {}", theId);

    createQuestionnaire(planDefinition.getIdElement().getIdPart());

    return planDefinition;
  }

  @Override
  public PlanDefinition resolvePlanDefinition(String url) {
    var searchResult = repository.search(Bundle.class, PlanDefinition.class,
        Searches.byUrl(url));

    if (!searchResult.hasEntry()) {
      throw new FHIRException("No plan definition found for url: " + url);
    }
    var basePlanDefinition = searchResult.getEntryFirstRep().getResource();

    var planDefinition = castOrThrow(basePlanDefinition, PlanDefinition.class,
        "The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class")
        .orElse(null);

    logger.info("Performing $apply operation on {}", url);

    createQuestionnaire(planDefinition.getIdElement().getIdPart());

    return planDefinition;
  }

  private void createQuestionnaire(String idPart){
     this.questionnaire = new Questionnaire();
    this.questionnaire.setId(new IdType(FHIRAllTypes.QUESTIONNAIRE.toCode(), idPart));
    this.questionnaireItemGenerator =
        new QuestionnaireItemGenerator(repository, patientId, parameters, bundle, libraryEngine);
  }

  @Override
  public IBaseResource applyPlanDefinition(PlanDefinition planDefinition) {
    // Each Group of actions shares a RequestGroup
    var requestGroup =
        new RequestGroup().setStatus(RequestStatus.DRAFT).setIntent(RequestIntent.PROPOSAL)
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
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained(goal);
      } else {
        goal.setIdElement(new IdType("Goal", String.valueOf(i + 1)));
        requestGroup.addExtension().setUrl(REQUEST_GROUP_EXT)
            .setValue(new Reference(goal.getIdElement()));
      }
      // Always add goals to the resource list so they can be added to the CarePlan if needed
      requestResources.add(goal);
    }

    // Create Questionnaire for the RequestGroup if using Modular Questionnaires.
    // Assuming Dynamic until a use case for modular arises

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
    var carePlan = new CarePlan().setInstantiatesCanonical(requestGroup.getInstantiatesCanonical())
        .setSubject(requestGroup.getSubject()).setStatus(CarePlan.CarePlanStatus.DRAFT)
        .setIntent(CarePlan.CarePlanIntent.PROPOSAL);
    carePlan.setId(new IdType(carePlan.fhirType(), requestGroup.getIdElement().getIdPart()));

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
        carePlan.addGoal(new Reference((Resource) goal));
      }
    }
    carePlan.addActivity().setReference(new Reference(requestGroup));
    carePlan.addContained(requestGroup);

    for (var resource : extractedResources) {
      carePlan.addSupportingInfo(new Reference((Resource) resource));
      carePlan.addContained((Resource) resource);
    }

    if (((Questionnaire) this.questionnaire).hasItem()) {
      carePlan.addContained((Resource) this.questionnaire);
    }

    return (CarePlan) ContainedHelper.liftContainedResourcesToParent(carePlan);
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
      requestGroup.getAction().get(requestGroup.getAction().size() - 1).addExtension()
          .setValue((Type) value);
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
    if (planDefinition.hasExtension(Constants.CPG_QUESTIONNAIRE_GENERATE) && action.hasInput()) {
      for (var actionInput : action.getInput()) {
        if (actionInput.hasProfile()) {
          ((Questionnaire) this.questionnaire).addItem(this.questionnaireItemGenerator
              .generateItem(actionInput, ((Questionnaire) this.questionnaire).getItem().size()));
        }
      }
    }

    if (Boolean.TRUE.equals(meetsConditions(planDefinition, requestGroup, action))) {
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
      if (action.hasDefinitionCanonicalType()) {
        var resource = resolveDefinition(planDefinition, requestGroup, action);
        if (resource != null) {
          applyAction(requestGroup, resource, action);
          requestAction.setResource(new Reference(resource.getIdElement()));
          if (Boolean.TRUE.equals(containResources)) {
            requestGroup.addContained((Resource) resource);
          } else {
            requestResources.add(resource);
          }
        }
      } else if (action.hasDefinitionUriType()) {
        var definition = action.getDefinitionUriType();
        requestAction.setResource(new Reference(definition.asStringValue()));
      }
      requestGroup.addAction(requestAction);
      resolveDynamicValues(planDefinition, requestGroup, action);
    }
  }

  private RequestGroupActionComponent createRequestAction(PlanDefinitionActionComponent action) {
    var requestAction = new RequestGroupActionComponent().setTitle(action.getTitle())
        .setDescription(action.getDescription()).setTextEquivalent(action.getTextEquivalent())
        .setCode(action.getCode()).setDocumentation(action.getDocumentation())
        .setTiming(action.getTiming());
    requestAction.setId(action.getId());

    return requestAction;
  }

  private IBaseResource resolveDefinition(PlanDefinition planDefinition, RequestGroup requestGroup,
      PlanDefinition.PlanDefinitionActionComponent action) {
    logger.debug("Resolving definition {}", action.getDefinitionCanonicalType().getValue());
    var definition = action.getDefinitionCanonicalType();
    var resourceName = resolveResourceName(definition, planDefinition);
    switch (FHIRAllTypes.fromCode(requireNonNull(resourceName))) {
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
        var searchResult = repository.search(Bundle.class, Questionnaire.class,
            Searches.byUrl(definition.asStringValue()));
        if (!searchResult.hasEntry()) {
          throw new FHIRException("No questionnaire found for definition: " + definition);
        }
        result = searchResult.getEntryFirstRep().getResource();
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
      if (referenceToContained) {
        var activityDefinition =
            (ActivityDefinition) resolveContained(planDefinition, definition.getValue());
        result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition,
            patientId, practitionerId, organizationId);
        result.setId(
            new IdType(result.fhirType(), activityDefinition.getIdPart().replaceFirst("#", "")));
      } else {
        result = this.activityDefinitionProcessor.apply(definition.asStringValue(),
            patientId, encounterId, practitionerId, organizationId, userType, userLanguage,
            userTaskContext, setting, settingContext, parameters, libraryEngine);
        var activityDefinitionId = new IdType(definition.asStringValue());
        result.setId(activityDefinitionId.getIdPart().withResourceType(result.fhirType()));
      }
    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}",
          definition, e.toString());
    }

    return result;
  }

  private IBaseResource applyNestedPlanDefinition(RequestGroup requestGroup,
      CanonicalType definition) {
    var searchResult = repository.search(Bundle.class, PlanDefinition.class,
        Searches.byUrl(definition.asStringValue()));
    if (!searchResult.hasEntry()) {
      throw new FHIRException(
          "No plan definition found for definition: " + definition.asStringValue());
    }
    var planDefinition = (PlanDefinition) searchResult.getEntryFirstRep().getResource();
    var result = (RequestGroup) applyPlanDefinition(planDefinition);

    for (var c : result.getInstantiatesCanonical()) {
      requestGroup.addInstantiatesCanonical(c.getValueAsString());
    }

    return result;
  }

  private void applyAction(RequestGroup requestGroup, IBaseResource result,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if ("Task".equals(result.fhirType())) {
      resolveTask(requestGroup, (Task) result, action);
    }
  }

  /*
   * offset -> Duration timing -> Timing ( just our use case for connectathon period periodUnit
   * frequency count ) use task code
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

    if (action.hasExtension(Constants.SDC_QUESTIONNAIRE_PREPOPULATE)) {
      resolvePrepopulateAction(action, requestGroup, task);
    }
  }

  private void resolvePrepopulateAction(PlanDefinition.PlanDefinitionActionComponent action,
      RequestGroup requestGroup, Task task) {
    var questionnaireBundles = getQuestionnairePackage(action);
    for (var questionnaireBundle : questionnaireBundles) {
      var questionnaire = (Questionnaire) questionnaireBundle.getEntryFirstRep().getResource();
      // Each bundle should contain a Questionnaire and supporting Library and ValueSet
      // resources
      var libraries = questionnaireBundle.getEntry().stream()
          .filter(e -> e.hasResource()
              && (e.getResource().fhirType().equals(Enumerations.FHIRAllTypes.LIBRARY.toCode())))
          .map(e -> (Library) e.getResource()).collect(Collectors.toList());
      var valueSets = questionnaireBundle.getEntry().stream()
          .filter(e -> e.hasResource()
              && (e.getResource().fhirType().equals(Enumerations.FHIRAllTypes.VALUESET.toCode())))
          .map(e -> (ValueSet) e.getResource()).collect(Collectors.toList());
      var additionalData =
          bundle == null ? new Bundle().setType(BundleType.COLLECTION) : ((Bundle) bundle).copy();
      libraries.forEach(library -> additionalData
          .addEntry(new Bundle.BundleEntryComponent().setResource(library)));
      valueSets.forEach(valueSet -> additionalData
          .addEntry(new Bundle.BundleEntryComponent().setResource(valueSet)));

      var oc = new OperationOutcome();
      oc.setId("prepopulate-outcome-" + questionnaire.getId());
      try {
        questionnaireProcessor.prePopulate(questionnaire, patientId, this.parameters,
            additionalData, libraryEngine);
      } catch (Exception ex) {
        var message = ex.getMessage();
        logger.error("Error encountered while attempting to prepopulate questionnaire: %s",
            message);
        oc.addIssue().setCode(OperationOutcome.IssueType.EXCEPTION)
            .setSeverity(OperationOutcome.IssueSeverity.ERROR).setDiagnostics(message);
      }
      if (!oc.getIssue().isEmpty()) {
        if (Boolean.TRUE.equals(containResources)) {
          requestGroup.addContained(oc);
          requestGroup.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getId()));
        } else {
          requestResources.add(oc);
          requestGroup.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference(oc.getIdElement()));
        }
      }
      if (Boolean.TRUE.equals(containResources)) {
        requestGroup.addContained(questionnaire);
      } else {
        requestResources.add(questionnaire);
      }
      task.setFocus(new Reference(questionnaire.getIdElement()));
      task.setFor(requestGroup.getSubject());
    }
  }

  private List<Bundle> getQuestionnairePackage(
      PlanDefinition.PlanDefinitionActionComponent action) {
    Bundle bundle = null;
    // PlanDef action should provide endpoint for $questionnaire-for-order operation as well as
    // the order id to pass
    var prepopulateExtension = action.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE);
    var parameterExtension =
        prepopulateExtension.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER);
    var parameterName = parameterExtension.getValue().toString();
    var prepopulateParameter =
        this.parameters != null ? ((Parameters) this.parameters).getParameter(parameterName) : null;
    if (prepopulateParameter == null) {
      throw new IllegalArgumentException(String.format("Parameter not found: %s ", parameterName));
    }
    var orderId = prepopulateParameter.toString();

    var questionnaireUrl = ((CanonicalType) prepopulateExtension
        .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE).getValue()).getValue();

    if (questionnaireUrl.contains("$")) {
      var urlSplit = questionnaireUrl.split("$");
      IGenericClient client = Clients.forUrl(repository.fhirContext(), urlSplit[0]);
      // Clients.registerBasicAuth(client, user, password);
      try {
        // TODO: This is not currently in use, but if it ever is we will need to determine how the
        // order and coverage resources are passed in
        Type order = null;
        Type coverage = null;
        bundle = client.operation().onType(FHIRAllTypes.QUESTIONNAIRE.toCode())
            .named("$questionnaire-package")
            .withParameters(
                new Parameters().addParameter("order", order).addParameter("coverage", coverage))
            .returnResourceType(Bundle.class).execute();
      } catch (Exception e) {
        logger.error("Error encountered calling $questionnaire-package operation: %s", e);
      }
    } else {
      var searchResult =
          repository.search(Bundle.class, Questionnaire.class, Searches.byUrl(questionnaireUrl));
      if (!searchResult.hasEntry()) {
        throw new FHIRException("No questionnaire found for definition: " + questionnaireUrl);
      }
      var questionnaire = searchResult.getEntryFirstRep().getResource();
      if (questionnaire != null) {
        bundle =
            new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(questionnaire));
      }
    }

    if (bundle == null) {
      bundle = new Bundle();
    }

    return Collections.singletonList(bundle);
  }

  private void resolveDynamicValues(PlanDefinition planDefinition, RequestGroup requestGroup,
      PlanDefinition.PlanDefinitionActionComponent action) {
    action.getDynamicValue().forEach(dynamicValue -> {
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
            dynamicValue.getExpression().getExpression(), dynamicValue.getPath(), altLanguage,
            altExpression, altPath, planDefinition.getLibrary().get(0).getValueAsString(),
            requestGroup, resolveInputParameters(action.getInput()));
      }
    });
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
          // logger.info("Found a workflow definition type for PlanDefinition {}
          // conditions should be evaluated at task execution time.",
          // planDefinition.getUrl());
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
          logger.info("The result of condition expression {} is false",
              condition.getExpression().getExpression());
          return false;
        }
        logger.info("The result of condition expression {} is true",
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

  private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
    if (dataRequirements == null)
      return new Parameters();

    var params = new Parameters();
    for (var req : dataRequirements) {
      if (!req.hasId()) {
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
