package org.opencds.cqf.cql.evaluator.activitydefinition;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
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
  private final FhirContext fhirContext;
  private final IFhirPath fhirPath;
  private final ModelResolver modelResolver;
  protected Repository repository;

  protected BaseActivityDefinitionProcessor(FhirContext fhirContext, Repository repository) {
    requireNonNull(fhirContext, "fhirContext can not be null");
    requireNonNull(repository, "repository can not be null");
    this.fhirContext = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    this.repository = repository;
    modelResolver = new FhirModelResolverFactory()
        .create(fhirContext.getVersion().getVersion().getFhirVersionString());
  }

  protected String subjectId;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;

  public IBaseResource apply(IIdType theId, String subjectId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, IBaseParameters parameters,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint) {
    this.repository = Repositories.proxy(fhirContext, repository, dataEndpoint, contentEndpoint,
        terminologyEndpoint);

    return apply(theId, subjectId, encounterId, practitionerId, organizationId, userType,
        userLanguage, userTaskContext, setting, settingContext, parameters,
        new LibraryEngine(fhirContext, this.repository));
  }

  @SuppressWarnings("unchecked")
  public IBaseResource apply(IIdType theId, String subjectId, String encounterId,
      String practitionerId, String organizationId, String userType, String userLanguage,
      String userTaskContext, String setting, String settingContext, IBaseParameters parameters,
      LibraryEngine libraryEngine) {
    this.subjectId = subjectId;
    this.parameters = parameters;
    this.libraryEngine = libraryEngine;
    T activityDefinition = (T) this.repository.read(ActivityDefinition.class, theId);
    if (activityDefinition == null) {
      throw new IllegalArgumentException("Couldn't find ActivityDefinition " + theId);
    }
    return resolveActivityDefinition(activityDefinition, subjectId, practitionerId, organizationId);
  }

  public abstract IBaseResource resolveActivityDefinition(T activityDefinition, String patientId,
      String practitionerId, String organizationId);

  public void resolveDynamicValue(String language, String expression, String libraryUrl,
      String path, IBaseResource resource, String subjectType) {

    var value = this.libraryEngine.getExpressionResult(this.subjectId, subjectType, expression,
        language, libraryUrl, this.parameters, null);
    modelResolver.setValue(resource, path, value);
  }

}
