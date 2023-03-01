package org.opencds.cqf.cql.evaluator.questionnaire;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

public abstract class BaseQuestionnaireProcessor<T> {
  protected static final Logger logger = LoggerFactory.getLogger(BaseQuestionnaireProcessor.class);

  protected LibraryEngine libraryEngine;
  protected ExpressionEvaluator expressionEvaluator;
  protected FhirContext fhirContext;
  protected FhirDal fhirDal;
  protected IFhirPath fhirPath;

  protected String patientId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;

  protected BaseQuestionnaireProcessor(FhirContext fhirContext, FhirDal fhirDal) {
    this.fhirContext = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    this.fhirDal = fhirDal;
  }

  public abstract LibraryEngine buildLibraryEngine(IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);

  public abstract T prePopulate(T questionnaire, String patientId, IBaseParameters parameters,
      IBaseBundle bundle, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint);

  public abstract T prePopulate(T questionnaire, String patientId, IBaseParameters parameters,
      IBaseBundle bundle, LibraryEngine libraryEngine);

  public abstract IBaseResource populate(T questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);

  public abstract IBaseResource populate(T questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine);

  public abstract T generateQuestionnaire(String theId, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint);

  public abstract T generateQuestionnaire(String theId, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine);
}
