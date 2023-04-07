package org.opencds.cqf.cql.evaluator.activitydefinition;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.fhirpath.IFhirPath;

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
  private final IFhirPath fhirPath;
  private final ModelResolver modelResolver;
  protected Repository repository;

  protected String subjectId;
  protected String encounterId;
  protected String practitionerId;
  protected String organizationId;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;

  protected BaseActivityDefinitionProcessor(Repository repository) {
    requireNonNull(repository, "repository can not be null");
    this.repository = repository;
    this.fhirPath = FhirPathCache.cachedForContext(repository.fhirContext());
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
      IBaseParameters parameters, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint) {
    this.repository =
        Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);

    return apply(theId, theCanonical, theActivityDefinition, subjectId, encounterId, practitionerId,
        organizationId, userType, userLanguage, userTaskContext, setting, settingContext,
        parameters, new LibraryEngine(this.repository));
  }

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(IIdType theId,
      CanonicalType theCanonical, IBaseResource theActivityDefinition, String subjectId,
      String encounterId, String practitionerId, String organizationId, String userType,
      String userLanguage, String userTaskContext, String setting, String settingContext,
      IBaseParameters parameters, LibraryEngine libraryEngine) {
    return apply(resolveActivityDefinition(theId, theCanonical, theActivityDefinition), subjectId,
        encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext,
        setting, settingContext, parameters, libraryEngine);
  }

  public IBaseResource apply(T theActivityDefinition, String subjectId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, IBaseParameters parameters,
      LibraryEngine libraryEngine) {
    this.subjectId = subjectId;
    this.encounterId = encounterId;
    this.practitionerId = practitionerId;
    this.organizationId = organizationId;
    this.parameters = parameters;
    this.libraryEngine = libraryEngine;

    return applyActivityDefinition(theActivityDefinition);
  }

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolveActivityDefinition(
      IIdType theId, CanonicalType theCanonical, IBaseResource theActivityDefinition);

  public abstract IBaseResource applyActivityDefinition(T theActivityDefinition);

  public void resolveDynamicValue(String language, String expression, String libraryUrl,
      String path, IBaseResource resource, String subjectType) {

    var value = this.libraryEngine.getExpressionResult(this.subjectId, subjectType, expression,
        language, libraryUrl, this.parameters, null);
    if (value.size() == 1) {
      modelResolver.setValue(resource, path, value.get(0));
    } else {
      throw new IllegalArgumentException(String.format(
          "Dynamic value resolution received multiple values for expression: %s", expression));
    }
  }

}
