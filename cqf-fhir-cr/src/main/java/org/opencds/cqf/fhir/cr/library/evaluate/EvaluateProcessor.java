package org.opencds.cqf.fhir.cr.library.evaluate;

import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.cql.EvaluationSettings;

@SuppressWarnings("UnstableApiUsage")
public class EvaluateProcessor implements IEvaluateProcessor {
    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;

    public EvaluateProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.evaluationSettings = evaluationSettings;
    }

    public IBaseParameters evaluate(EvaluateRequest request) {
        try {
            return request.getLibraryEngine()
                    .evaluate(
                            request.getLibraryAdapter().getCanonical(),
                            request.getSubject(),
                            request.getParameters(),
                            request.getRawParameters(),
                            request.getData(),
                            null,
                            request.getExpression());
        } catch (Exception e) {
            request.logException(e.getMessage());
            return newParameters(
                    repository.fhirContext(),
                    newPart(repository.fhirContext(), "evaluation error", request.getOperationOutcome()));
        }
    }
}
