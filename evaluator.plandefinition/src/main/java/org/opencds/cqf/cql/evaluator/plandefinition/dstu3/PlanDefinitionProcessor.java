package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;
import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.SearchHelper.searchRepositoryByCanonical;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Goal;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.ParameterDefinition;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RequestGroup;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestIntent;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestStatus;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.activitydefinition.dstu3.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.helper.dstu3.ContainedHelper;
import org.opencds.cqf.cql.evaluator.fhir.util.Clients;
import org.opencds.cqf.cql.evaluator.library.CqfExpression;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.ExpressionEngine;
import org.opencds.cqf.cql.evaluator.plandefinition.BasePlanDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.QuestionnaireItemGenerator;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.QuestionnaireProcessor;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.dstu3.QuestionnaireResponseProcessor;
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
            .equals(Enumerations.FHIRAllTypes.QUESTIONNAIRERESPONSE.toCode()))
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
        "The planDefinition passed to Repository was not a valid instance of PlanDefinition.class")
            .orElse(null);

    logger.info("Performing $apply operation on {}", theId);

    this.questionnaire = new Questionnaire();
    this.questionnaire
        .setId(new IdType(FHIRAllTypes.QUESTIONNAIRE.toCode(), planDefinition.getIdPart()));
    this.questionnaireItemGenerator =
        new QuestionnaireItemGenerator(repository, patientId, parameters, bundle, libraryEngine);

    return planDefinition;
  }

  @Override
  public IBaseResource applyPlanDefinition(PlanDefinition planDefinition) {
    // Each Group of actions shares a RequestGroup
    var requestGroup = new RequestGroup().setStatus(RequestStatus.DRAFT)
        .setIntent(RequestIntent.PROPOSAL).addDefinition(new Reference(planDefinition.getUrl()))
        .setSubject(new Reference(patientId));

    requestGroup
        .setId(new IdType(requestGroup.fhirType(), planDefinition.getIdElement().getIdPart()));
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

    // Create Questionnaire for the RequestGroup if using Modular Questionnaires.
    // Assuming Dynamic until a use case for modular arises

    var metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();

    var defaultLibraryUrl =
        planDefinition.getLibrary() == null || planDefinition.getLibrary().isEmpty() ? null
            : planDefinition.getLibrary().get(0).getReference();
    for (var action : planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      requestGroup.addAction(
          resolveAction(defaultLibraryUrl, planDefinition, requestGroup, metConditions, action));
    }

    return requestGroup;
  }

  @Override
  public CarePlan transformToCarePlan(IBaseResource rg) {
    var requestGroup = (RequestGroup) rg;
    var carePlan = new CarePlan().setStatus(CarePlan.CarePlanStatus.DRAFT)
        .setIntent(CarePlan.CarePlanIntent.PROPOSAL).setDefinition(requestGroup.getDefinition())
        .setSubject(requestGroup.getSubject());
    carePlan.setId(new IdType(carePlan.fhirType(), requestGroup.getIdElement().getIdPart()));

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
    return null;
  }

  @Override
  public void resolveDynamicExtension(IElement requestAction, IBase resource, Object value,
      String path) {
    if (path.equals("activity.extension") || path.equals("action.extension")) {
      // default to adding extension to last action
      ((Element) requestAction).addExtension().setValue((Type) value);
    }
  }

  private Goal convertGoal(PlanDefinition.PlanDefinitionGoalComponent goal) {
    var myGoal = new Goal();
    myGoal.setCategory(Collections.singletonList(goal.getCategory()));
    myGoal.setDescription(goal.getDescription());
    myGoal.setPriority(goal.getPriority());
    myGoal.setStart(goal.getStart());

    var goalTarget = goal.hasTarget() ? goal.getTarget().stream().map(target -> {
      var myTarget = new Goal.GoalTargetComponent();
      myTarget.setDetail(target.getDetail());
      myTarget.setMeasure(target.getMeasure());
      myTarget.setDue(target.getDue());
      myTarget.setExtension(target.getExtension());
      return myTarget;
    }).collect(Collectors.toList()).get(0) : null;
    myGoal.setTarget(goalTarget);
    return myGoal;
  }

  private RequestGroupActionComponent resolveAction(String defaultLibraryUrl,
      PlanDefinition planDefinition,
      RequestGroup requestGroup,
      Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions,
      PlanDefinition.PlanDefinitionActionComponent action) {
    if ((getExtensionByUrl(planDefinition, Constants.CPG_QUESTIONNAIRE_GENERATE) != null)
        && action.hasInput()) {
      for (var actionInput : action.getInput()) {
        if (actionInput.hasProfile()) {
          ((Questionnaire) this.questionnaire).addItem(this.questionnaireItemGenerator
              .generateItem(actionInput, ((Questionnaire) this.questionnaire).getItem().size()));
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
      if (action.hasDefinition()) {
        resource = resolveDefinition(planDefinition, requestGroup, action);
        if (resource != null) {
          applyAction(requestGroup, resource, action);
          requestAction.setResource(new Reference(resource.getIdElement()));
          if (Boolean.TRUE.equals(containResources)) {
            requestGroup.addContained((Resource) resource);
          } else {
            requestResources.add(resource);
          }
        }
      }
      resolveDynamicValues(defaultLibraryUrl, requestAction, resource, action);

      return requestAction;
    }

    return null;
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
    logger.debug("Resolving definition {}", action.getDefinition().getReference());
    var definition = action.getDefinition();
    var resourceName = getResourceName(definition);
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
      Reference definition) {
    IBaseResource result = null;
    try {
      var referenceToContained = definition.getReference().startsWith("#");
      if (referenceToContained) {
        result = resolveContained(planDefinition, definition.getReference());
      } else {
        result = searchRepositoryByCanonical(repository, new StringType(definition.getReference()));
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}",
          definition, e.toString());
    }

    return result;
  }

  private IBaseResource applyActivityDefinition(PlanDefinition planDefinition,
      Reference definition) {
    IBaseResource result = null;
    try {
      var referenceToContained = definition.getReference().startsWith("#");
      var activityDefinition = (ActivityDefinition) (referenceToContained
          ? resolveContained(planDefinition, definition.getReference())
          : searchRepositoryByCanonical(repository, new StringType(definition.getReference())));
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

  private IBaseResource applyNestedPlanDefinition(RequestGroup requestGroup, Reference definition) {
    var planDefinition = (PlanDefinition) searchRepositoryByCanonical(repository,
        new StringType(definition.getReference()));
    var result = (RequestGroup) applyPlanDefinition(planDefinition);

    for (var c : result.getDefinition()) {
      requestGroup.addDefinition(c);
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
    task.setFor(requestGroup.getSubject());

    resolvePrepopulateAction(action, requestGroup, task);
  }

  private void resolvePrepopulateAction(PlanDefinition.PlanDefinitionActionComponent action,
      RequestGroup requestGroup, Task task) {
    if (action.hasExtension(Constants.SDC_QUESTIONNAIRE_PREPOPULATE)) {
      var questionnaireBundles = getQuestionnairePackage(
          (Extension) getExtensionByUrl(action, Constants.SDC_QUESTIONNAIRE_PREPOPULATE));
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
            requestGroup.addExtension(new Extension().setUrl(Constants.EXT_CRMI_MESSAGES)
                .setValue(new Reference("#" + oc.getId())));
          } else {
            requestResources.add(oc);
            requestGroup.addExtension(new Extension().setUrl(Constants.EXT_CRMI_MESSAGES)
                .setValue(new Reference(oc.getIdElement())));
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
  }

  private List<Bundle> getQuestionnairePackage(Extension prepopulateExtension) {
    Bundle bundle = null;
    // PlanDef action should provide endpoint for $questionnaire-for-order operation as well as
    // the order id to pass
    var parameterExtension =
        getExtensionByUrl(prepopulateExtension, Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER);
    if (parameterExtension == null) {
      throw new IllegalArgumentException(String.format("Required extension for %s not found.",
          Constants.SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER));
    }
    var parameterName = parameterExtension.getValue().toString();
    var prepopulateParameter = this.parameters != null
        ? ((Parameters) this.parameters).getParameter().stream()
            .filter(p -> p.getName().equals(parameterName)).collect(Collectors.toList()).get(0)
        : null;
    if (prepopulateParameter == null) {
      throw new IllegalArgumentException(String.format("Parameter not found: %s ", parameterName));
    }
    var orderId = prepopulateParameter.toString();

    var questionnaireExtension =
        getExtensionByUrl(prepopulateExtension, Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE);
    if (questionnaireExtension == null) {
      throw new IllegalArgumentException(String.format("Required extension for %s not found.",
          Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE));
    }

    if (questionnaireExtension.getValue().fhirType().equals(FHIRAllTypes.URI.toCode())) {
      var questionnaire =
          searchRepositoryByCanonical(repository, (UriType) questionnaireExtension.getValue());
      if (questionnaire != null) {
        bundle =
            new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(questionnaire));
      }
    } else if (questionnaireExtension.getValue().fhirType().equals(FHIRAllTypes.STRING.toCode())) {
      // Assuming package operation endpoint if the extension is using valueString instead of
      // valueUri
      bundle = callQuestionnairePackageOperation(
          ((StringType) questionnaireExtension.getValue()).getValueAsString());
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
      Type order = null;
      Type coverage = null;
      bundle = client.operation().onType(FHIRAllTypes.QUESTIONNAIRE.toCode()).named('$' + operation)
          .withParameters(new Parameters()
              .addParameter(new ParametersParameterComponent().setName("order").setValue(order))
              .addParameter(
                  new ParametersParameterComponent().setName("coverage").setValue(coverage)))
          .returnResourceType(Bundle.class).execute();
    } catch (Exception e) {
      logger.error("Error encountered calling $questionnaire-package operation: %s", e);
    }

    return bundle;
  }

  private CqfExpression getCqfExpression(String language, String expression,
      String defaultLibraryUrl) {
    return new CqfExpression().setExpression(expression)
        .setLanguage(language)
        .setLibraryUrl(defaultLibraryUrl);
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
              resolveExpression(getCqfExpression(dynamicValue.getLanguage(),
                  dynamicValue.getExpression(), defaultLibraryUrl), inputParams);
          resolveDynamicValue(result, dynamicValue.getPath(), requestAction, resource);
        } catch (Exception e) {
          var message = String.format("DynamicValue expression %s encountered exception: %s",
              dynamicValue.getExpression(), e.getMessage());
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
              resolveExpression(getCqfExpression(condition.getLanguage(), condition.getExpression(),
                  defaultLibraryUrl), inputParams);
          result = results == null || results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
          var message = String.format("Condition expression %s encountered exception: %s",
              condition.getExpression(), e.getMessage());
          logger.error(message);
        }
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
          logger.debug("The result of condition expression {} is false", condition.getExpression());
          return false;
        }
        logger.debug("The result of condition expression {} is true", condition.getExpression());
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
    if (dataRequirements == null)
      return new Parameters();

    var params = new Parameters();
    for (var req : dataRequirements) {
      if (!req.hasId()) {
        continue;
      }

      var resources = repository.search(Bundle.class, IBaseResource.class, Searches.ALL);

      var ee = new ExpressionEngine(repository, evaluationSettings);

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
                    Searches.byUrl(filter.getValueSetStringType().getValue()));
                if (valueSets.hasEntry()) {
                  codeFilterParam.addParameter().setName("%valueset")
                      .setResource(valueSets.getEntryFirstRep().getResource());
                  var codeFilterExpression =
                      "%" + String.format("resource.%s.where(code.memberOf('%s'))",
                          filter.getPath(), "%valueset");
                  var codeFilterResult =
                      ee.evaluate(codeFilterExpression, codeFilterParam);
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
