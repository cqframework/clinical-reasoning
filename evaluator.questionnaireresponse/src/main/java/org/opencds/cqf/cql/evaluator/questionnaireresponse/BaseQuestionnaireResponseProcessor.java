package org.opencds.cqf.cql.evaluator.questionnaireresponse;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;

/*
 * https://build.fhir.org/ig/HL7/sdc/OperationDefinition-QuestionnaireResponse- extract.html
 *
 * Use Name Cardinality Type IN questionnaire-response 0..1 Resource The QuestionnaireResponse to
 * extract data from. Used when the operation is invoked at the 'type' level.
 *
 * OUT return 0..1 Resource The resulting FHIR resource produced after extracting data. This will
 * either be a single resource or a Transaction Bundle that contains multiple resources. The
 * operations in the Bundle might be creates, updates and/or conditional versions of both depending
 * on the nature of the extraction mappings.
 *
 * OUT issues 0..1 OperationOutcome A list of hints and warnings about problems encountered while
 * extracting the resource(s) from the QuestionnaireResponse. If there was nothing to extract, a
 * 'success' OperationOutcome is returned with a warning and/or information messages. In situations
 * where the input is invalid or the operation otherwise fails to complete successfully, a normal
 * 'erroneous' OperationOutcome would be returned (as happens with all operations) indicating what
 * the issue was.
 */

public abstract class BaseQuestionnaireResponseProcessor<T> {
  protected static final Logger logger =
      LoggerFactory.getLogger(BaseQuestionnaireResponseProcessor.class);
  protected final EvaluationSettings evaluationSettings;
  protected final IParser parser;
  protected Repository repository;
  protected LibraryEngine libraryEngine;

  protected String patientId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;
  protected String libraryUrl;
  protected static final String subjectType = "Patient";

  protected BaseQuestionnaireResponseProcessor(Repository repository,
      EvaluationSettings evaluationSettings) {
    this.repository = requireNonNull(repository, "repository can not be null");
    this.evaluationSettings =
        requireNonNull(evaluationSettings, "evaluationSettings can not be null");

    parser = this.repository.fhirContext().newJsonParser();
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

  protected abstract IBaseBundle createResourceBundle(T questionnaireResponse,
      List<IBaseResource> resources);

  public abstract List<IBaseResource> processItems(T questionnaireResponse);

  public abstract T resolveQuestionnaireResponse(IIdType theId,
      IBaseResource theQuestionnaireResponse);

  protected abstract void setup(T theQuestionnaireResponse);

  public IBaseBundle extract(IIdType theId, IBaseResource theQuestionnaireResponse,
      IBaseParameters theParameters, IBaseBundle theBundle, LibraryEngine theLibraryEngine) {
    return extract(resolveQuestionnaireResponse(theId, theQuestionnaireResponse), theParameters,
        theBundle, theLibraryEngine == null ? new LibraryEngine(repository, evaluationSettings)
            : theLibraryEngine);
  }

  public IBaseBundle extract(T theQuestionnaireResponse, IBaseParameters theParameters,
      IBaseBundle theBundle, LibraryEngine theLibraryEngine) {
    if (theQuestionnaireResponse == null) {
      var message = "Unable to perform operation $extract.  The QuestionnaireResponse was null";
      logger.error(message);
      throw new IllegalArgumentException(message);
    }
    parameters = theParameters;
    bundle = theBundle;
    libraryEngine = theLibraryEngine;
    setup(theQuestionnaireResponse);

    var resources = processItems(theQuestionnaireResponse);

    return createResourceBundle(theQuestionnaireResponse, resources);
  }

  protected String getExtractId(T questionnaireResponse) {
    return "extract-" + ((IBaseResource) questionnaireResponse).getIdElement().getIdPart();
  }
}
