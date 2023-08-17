package org.opencds.cqf.cql.evaluator.activitydefinition;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BaseActivityDefinitionProcessor<T> {
  private static final Logger logger =
      LoggerFactory.getLogger(BaseActivityDefinitionProcessor.class);
  public static final String TARGET_STATUS_URL =
      "http://hl7.org/fhir/us/ecr/StructureDefinition/targetStatus";
  public static final String PRODUCT_ERROR_PREAMBLE = "Product does not map to ";
  public static final String DOSAGE_ERROR_PREAMBLE = "Dosage does not map to ";
  public static final String BODYSITE_ERROR_PREAMBLE = "BodySite does not map to ";
  public static final String CODE_ERROR_PREAMBLE = "Code does not map to ";
  public static final String QUANTITY_ERROR_PREAMBLE = "Quantity does not map to ";
  public static final String MISSING_CODE_PROPERTY = "Missing required code property";
  protected static final List<String> EXCLUDED_EXTENSION_LIST = Arrays
      .asList(Constants.CPG_KNOWLEDGE_CAPABILITY, Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL);
  private final ModelResolver modelResolver;
  protected final EvaluationSettings evaluationSettings;
  protected Repository repository;

  protected String subjectId;
  protected String encounterId;
  protected String practitionerId;
  protected String organizationId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;
  protected LibraryEngine libraryEngine;

  protected BaseActivityDefinitionProcessor(Repository repository,
      EvaluationSettings evaluationSettings) {
    this.evaluationSettings =
        requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    this.repository = requireNonNull(repository, "repository can not be null");
    modelResolver = new FhirModelResolverFactory()
        .create(repository.fhirContext().getVersion().getVersion().getFhirVersionString());
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

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType theId,
      CanonicalType theCanonical, IBaseResource theActivityDefinition, String subjectId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint) {
    this.repository =
        Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);

    return apply(theId, theCanonical, theActivityDefinition, subjectId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, bundle, new LibraryEngine(this.repository, this.evaluationSettings));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType theId,
      CanonicalType theCanonical, IBaseResource theActivityDefinition, String subjectId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
    return apply(resolveActivityDefinition(theId, theCanonical, theActivityDefinition), subjectId,
        encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext,
        setting, settingContext, parameters, bundle, libraryEngine);
  }

  public IBaseResource apply(T theActivityDefinition, String subjectId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, IBaseParameters parameters,
      IBaseBundle bundle, LibraryEngine libraryEngine) {
    this.subjectId = subjectId;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.parameters = parameters;
    this.libraryEngine = libraryEngine;
    this.bundle = bundle;

    return applyActivityDefinition(theActivityDefinition);
  }

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolveActivityDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource theActivityDefinition);

  public abstract IBaseResource applyActivityDefinition(T theActivityDefinition);

  public void resolveDynamicValue(List<IBase> result, String expression, String path,
      IBaseResource resource) {
    if (result == null || result.isEmpty()) {
      return;
    }
    if (result.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "Dynamic value resolution received multiple values for expression: %s", expression));
    }

    if (path.contains(".")) {
      setNestedValue(resource, path, result.get(0));
    } else {
      modelResolver.setValue(resource, path, result.get(0));
    }
  }

  protected void setNestedValue(IBase target, String path, IBase value) {
    var def = (BaseRuntimeElementCompositeDefinition<?>) fhirContext()
        .getElementDefinition(target.getClass());
    var identifiers = path.split("\\.");
    for (int i = 0; i < identifiers.length; i++) {
      var identifier = identifiers[i];
      var isList = identifier.contains("[");
      var isLast = i == identifiers.length - 1;
      var index =
          isList ? Character.getNumericValue(identifier.charAt(identifier.indexOf("[") + 1)) : 0;
      var targetPath = isList ? identifier.replaceAll("\\[\\d\\]", "") : identifier;
      var targetDef = def.getChildByName(targetPath);

      var targetValues = targetDef.getAccessor().getValues(target);
      IBase targetValue;
      if (targetValues.size() >= index + 1 && !isLast) {
        targetValue = targetValues.get(index);
      } else {
        var elementDef = targetDef.getChildByName(targetPath);
        if (isLast) {
          targetValue = (IBase) modelResolver.as(value, elementDef.getImplementingClass(), false);
        } else {
          targetValue = elementDef.newInstance(targetDef.getInstanceConstructorArguments());
        }
        targetDef.getMutator().addValue(target, targetValue);
      }
      target = targetValue;
      if (!isLast) {
        var nextDef = fhirContext().getElementDefinition(target.getClass());
        def = (BaseRuntimeElementCompositeDefinition<?>) nextDef;
      }
    }
  }

  protected FhirContext fhirContext() {
    return repository.fhirContext();
  }
}
