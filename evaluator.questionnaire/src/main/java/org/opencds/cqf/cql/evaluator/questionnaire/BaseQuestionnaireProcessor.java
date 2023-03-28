package org.opencds.cqf.cql.evaluator.questionnaire;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.fhirpath.IFhirPath;

public abstract class BaseQuestionnaireProcessor<T> {
  protected static final Logger logger = LoggerFactory.getLogger(BaseQuestionnaireProcessor.class);

  protected LibraryEngine libraryEngine;
  protected Repository repository;
  protected IFhirPath fhirPath;

  protected String patientId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;
  protected static final String subjectType = "Patient";

  protected BaseQuestionnaireProcessor(Repository repository) {
    this.repository = repository;
    this.fhirPath = FhirPathCache.cachedForContext(repository.fhirContext());
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

  public abstract <CanonicalType extends IPrimitiveType<String>> T resolveQuestionnaire(
      IIdType theId, CanonicalType theCanonical, IBaseResource theQuestionnaire);

  public <CanonicalType extends IPrimitiveType<String>> T prePopulate(IIdType theId,
      CanonicalType theCanonical, IBaseResource questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return prePopulate(resolveQuestionnaire(theId, theCanonical, questionnaire), patientId,
        parameters, bundle, new LibraryEngine(repository));
  }

  public abstract T prePopulate(T questionnaire, String patientId, IBaseParameters parameters,
      IBaseBundle bundle, LibraryEngine libraryEngine);

  public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(IIdType theId,
      CanonicalType theCanonical, IBaseResource questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
    return populate(resolveQuestionnaire(theId, theCanonical, questionnaire), patientId, parameters,
        bundle, new LibraryEngine(repository));
  }

  public abstract IBaseResource populate(T questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine);

  public abstract T generateQuestionnaire(String theId);

  public abstract IBaseBundle packageQuestionnaire(T theQuestionnaire);

  public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
      IIdType theId, CanonicalType theCanonical, IBaseResource questionnaire) {
    return packageQuestionnaire(resolveQuestionnaire(theId, theCanonical, questionnaire));
  }
}
