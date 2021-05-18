package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.jms.IllegalStateRuntimeException;

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
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.AttachmentBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.CarePlanActivityBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.CarePlanBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.ExtensionBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.JavaDateBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.ReferenceBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.RelatedArtifactBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.RequestGroupActionBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.builders.RequestGroupBuilder;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.helper.ContainedHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

@SuppressWarnings("unused")
public class PlanDefinitionProcessor {
  protected ActivityDefinitionProcessor activityDefinitionProcessor;
  protected LibraryProcessor libraryProcessor;
  protected OperationParametersParser operationParametersParser;
  protected FhirContext fhirContext;
  protected FhirDal fhirDal;
  protected IFhirPath fhirPath;

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionProcessor.class);

  public PlanDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
      ActivityDefinitionProcessor activityDefinitionProcessor, OperationParametersParser operationParametersParser) {
    requireNonNull(fhirContext, "fhirContext can not be null");
    requireNonNull(fhirDal, "fhirDal can not be null");
    requireNonNull(libraryProcessor, "LibraryProcessor can not be null");
    requireNonNull(operationParametersParser, "OperationParametersParser can not be null");
    this.fhirContext = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    this.fhirDal = fhirDal;
    this.libraryProcessor = libraryProcessor;
    this.activityDefinitionProcessor = activityDefinitionProcessor;
    this.operationParametersParser = operationParametersParser;
  }

  public IBaseParameters apply(IdType theId, String patientId, String encounterId, String practitionerId,
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

    CarePlanBuilder builder = new CarePlanBuilder();

    builder.buildInstantiatesCanonical(planDefinition.getIdElement().getIdPart()).buildSubject(new Reference(patientId))
        .buildStatus(CarePlan.CarePlanStatus.DRAFT);

    if (encounterId != null)
      builder.buildEncounter(new Reference(encounterId));
    if (practitionerId != null)
      builder.buildAuthor(new Reference(practitionerId));
    if (organizationId != null)
      builder.buildAuthor(new Reference(organizationId));
    if (userLanguage != null)
      builder.buildLanguage(userLanguage);

    // Each Group of actions shares a RequestGroup
    RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();

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
        userType, userLanguage, userTaskContext, setting, settingContext, requestGroupBuilder, parameters, prefetchData,
        contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, useServerData, mergeNestedCarePlans,
        prefetchDataData, prefetchDataDescription, prefetchDataKey);

    CarePlan carePlan = (CarePlan) ContainedHelper.liftContainedResourcesToParent(resolveActions(session));
    IBaseParameters returnParameters = new Parameters();
    operationParametersParser.addResourceChild(returnParameters, "return", carePlan);
    return returnParameters;
  }

  private CarePlan resolveActions(Session session) {
    for (PlanDefinition.PlanDefinitionActionComponent action : session.planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      if (meetsConditions(session, action)) {
        resolveDefinition(session, action);
        resolveDynamicActions(session, action);
      }
    }

    RequestGroup result = session.requestGroupBuilder.build();

    if (result.getId() == null) {
      result.setId(UUID.randomUUID().toString());
    }

    session.carePlanBuilder.buildContained(result)
        .buildActivity(new CarePlanActivityBuilder().buildReference(new Reference("#" + result.getId())).build());

    return session.carePlanBuilder.build();
  }

  private void resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinitionCanonicalType()) {
      logger.debug("Resolving definition " + action.getDefinitionCanonicalType().getValue());
      CanonicalType definition = action.getDefinitionCanonicalType();
      switch (getResourceName(definition)) {
        case ("PlanDefinition"):
          applyNestedPlanDefinition(session, definition);
          break;
        case ("ActivityDefinition"):
          applyActivityDefinition(session, definition);
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

  private void applyActivityDefinition(Session session, CanonicalType definition) {
    IBaseResource result;
    try {
      boolean referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        ActivityDefinition activityDefinition = (ActivityDefinition) resolveContained(session.planDefinition,
            definition.getValue());
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

      if (result.getIdElement() == null) {
        logger.warn("ActivityDefinition %s returned resource with no id, setting one", definition.getId());
        result.setId(new IdType(UUID.randomUUID().toString()));
      }

      session.requestGroupBuilder.buildContained((Resource) result).addAction(new RequestGroupActionBuilder()
          .buildResource(new Reference("#" + result.getIdElement().getIdPart())).build());

    } catch (Exception e) {
      logger.error("ERROR: ActivityDefinition %s could not be applied and threw exception %s", definition,
          e.toString());
    }
  }

  private void applyNestedPlanDefinition(Session session, CanonicalType definition) {
    CarePlan plan;
    Iterator<IBaseResource> iterator = fhirDal.searchByUrl("PlanDefinition", definition.asStringValue()).iterator();
    if (!iterator.hasNext()) {
      throw new RuntimeException("No plan definition found for definition: " + definition);
    }
    PlanDefinition planDefinition = (PlanDefinition) iterator.next();
    IBaseParameters result = apply(planDefinition.getIdElement(), session.patientId, session.encounterId,
        session.practitionerId, session.organizationId, session.userType, session.userLanguage, session.userTaskContext,
        session.setting, session.settingContext, session.mergeNestedCarePlans, session.parameters,
        session.useServerData, session.bundle, session.prefetchData, session.dataEndpoint, session.contentEndpoint,
        session.terminologyEndpoint);

    IBaseResource baseCarePlan = operationParametersParser.getResourceChild(result, "return");
    if (!(baseCarePlan instanceof CarePlan)) {
      throw new RuntimeException(
          "Invalid PlanDefinition apply result expected Parameters with return parameter CarePlan" + result);
    }
    plan = (CarePlan) baseCarePlan;

    if (plan.getId() == null) {
      plan.setId(UUID.randomUUID().toString());
    }

    // Add an action to the request group which points to this CarePlan
    session.requestGroupBuilder.buildContained(plan)
        .addAction(new RequestGroupActionBuilder().buildResource(new Reference("#" + plan.getId())).build());

    for (CanonicalType c : plan.getInstantiatesCanonical()) {
      session.carePlanBuilder.buildInstantiatesCanonical(c.getValueAsString());
    }
  }

  private void resolveDynamicActions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
      logger.info("Resolving dynamic value %s %s", dynamicValue.getPath(), dynamicValue.getExpression());

      ensureDynamicValueExpression(dynamicValue);
      if (dynamicValue.getExpression().hasLanguage()) {

        ensurePrimaryLibrary(session);
        Set<String> expressions = new HashSet<>();
        String language = dynamicValue.getExpression().getLanguage();
        Object result = evaluateConditionOrDynamicValue(dynamicValue.getExpression().getExpression(), language,
            session);
        // TODO: Rename bundle
        if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this")) {
          session.carePlanBuilder = new CarePlanBuilder((CarePlan) result);
        } else {

          // TODO - likely need more date transformations
          if (result instanceof DateTime) {
            result = new JavaDateBuilder().buildFromDateTime((DateTime) result).build();
          }

          try {
            session.carePlanBuilder.build().setProperty(dynamicValue.getPath(), (Base) result);
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
        meetsConditions(session, containedAction);
      }
    }
    for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
      ensureConditionExpression(condition);
      if (condition.getExpression().hasLanguage()) {

        ensurePrimaryLibrary(session);
        String language = condition.getExpression().getLanguage();
        Object result = evaluateConditionOrDynamicValue(condition.getExpression().getExpression(), language, session);

        if (result == null) {
          logger.warn("Expression Returned null");
          return false;
        }

        if (!(result instanceof BooleanType)) {
          logger.warn("The condition returned a non-boolean value: " + result.getClass().getSimpleName());
          continue;
        }

        if (!((BooleanType) result).booleanValue()) {
          logger.info("The result of condition expression %s is false", condition.getExpression());
          return false;
        }
      }
    }
    return true;
  }

  protected void ensurePrimaryLibrary(Session session) {
    if (session.planDefinition.getLibrary().size() != 1) {
      throw new IllegalArgumentException(
          "Session's PlanDefinition's Library must only " + "include the primary liibrary. ");
    }
  }

  protected void ensureConditionExpression(PlanDefinition.PlanDefinitionActionConditionComponent condition) {
    if (!condition.hasExpression()) {
      logger.error("Missing condition expression");
      throw new RuntimeException("Missing condition expression");
    }
  }

  protected void ensureDynamicValueExpression(PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue) {
    if (!dynamicValue.hasExpression()) {
      logger.error("Missing condition expression");
      throw new RuntimeException("Missing condition expression");
    }
  }

  private String ensureStringResult(Object result) {
    if (!(result instanceof StringType))
      throw new IllegalStateRuntimeException("Result not instance of String");
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

  protected Object evaluateConditionOrDynamicValue(String expression, String language, Session session) {
    Set<String> expressions = new HashSet<>();
    expressions.add(expression);
    Object result = null;
    // Assumption that this will evolve to contain many cases
    switch (language) {
      // case "text/cql":
      // expressionEvaluator.evaluate(new Task(),
      // dynamicValue.getExpression());
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        result = libraryProcessor.evaluate(session.planDefinition.getLibrary().get(0).asStringValue(),
            session.patientId, session.parameters, session.contentEndpoint, session.terminologyEndpoint,
            session.dataEndpoint, session.bundle, expressions);
        break;
      case "text/fhirpath": {
        if (session.bundle != null) {
          Optional<IBase> optionalResult = fhirPath.evaluateFirst(session.bundle, expression, IBase.class);
          if (optionalResult.isPresent()) {
            result = optionalResult.get();
          }
        }
        if (session.encounterId != null && result == null) {
          IBaseResource encounter = fhirDal.read(new IdType("Encounter", session.encounterId));
          if (encounter == null) {
            throw new IllegalArgumentException(
                String.format("Encounter with id: %s does not exists.", session.encounterId));
          } else {
            Optional<IBase> optionalResult = fhirPath.evaluateFirst(encounter, expression, IBase.class);
            if (optionalResult.isPresent()) {
              result = optionalResult.get();
            }
          }
        }
      }
        break;
      default:
        logger.warn("An action language other than CQL was found: " + language);
    }
    if (result != null) {
      if (result instanceof Parameters) {
        IBaseDatatype tempResult = operationParametersParser.getValueChild(((Parameters) result), expression);
        if (tempResult == null) {
          IBaseResource tempResource = operationParametersParser.getResourceChild(((Parameters) result), expression);
          result = tempResource;
        } else {
          result = tempResult;
        }
      }
    }
    return result;
  }

  protected Resource resolveContained(DomainResource resource, String id) {
    for (Resource res : resource.getContained()) {
      if (res.hasIdElement()) {
        if (res.getIdElement().getIdPart().equals(id)) {
          return res;
        }
      }
    }

    throw new RuntimeException(
        String.format("Resource %s does not contain resource with id %s", resource.fhirType(), id));
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
  public CarePlanBuilder carePlanBuilder;
  public final String encounterId;
  public final RequestGroupBuilder requestGroupBuilder;
  public IBaseParameters parameters, prefetchData;
  public IBaseResource contentEndpoint, terminologyEndpoint, dataEndpoint;
  public IBaseBundle bundle, prefetchDataData;
  public DataRequirement prefetchDataDescription;
  public Boolean useServerData, mergeNestedCarePlans;

  public Session(PlanDefinition planDefinition, CarePlanBuilder builder, String patientId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
      String setting, String settingContext, RequestGroupBuilder requestGroupBuilder, IBaseParameters parameters,
      IBaseParameters prefetchData, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint, IBaseBundle bundle, Boolean useServerData, Boolean mergeNestedCarePlans,
      IBaseBundle prefetchDataData, DataRequirement prefetchDataDescription, String prefetchDataKey) {

    this.patientId = patientId;
    this.planDefinition = planDefinition;
    this.carePlanBuilder = builder;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.userType = userType;
    this.userLanguage = userLanguage;
    this.userTaskContext = userTaskContext;
    this.setting = setting;
    this.settingContext = settingContext;
    this.requestGroupBuilder = requestGroupBuilder;
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
