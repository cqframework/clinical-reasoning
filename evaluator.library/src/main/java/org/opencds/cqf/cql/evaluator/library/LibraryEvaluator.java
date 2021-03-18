package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;

public class LibraryEvaluator {

    // private static Logger logger =
    // LoggerFactory.getLogger(LibraryEvaluator.class);

    private CqlEvaluator cqlEvaluator;

    private CqlFhirParametersConverter cqlFhirParametersConverter;

    public LibraryEvaluator(CqlFhirParametersConverter cqlFhirParametersConverter, CqlEvaluator cqlEvaluator) {
        this.cqlFhirParametersConverter = requireNonNull(cqlFhirParametersConverter,
                "cqlFhirParametersConverter can not be null");
        this.cqlEvaluator = requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
    }

    /**
     * This function takes a Library identifier, a context Parameter, FHIR
     * Parameters, and a set of expressions to evaluate. It maps the input FHIR
     * parameters to to their appropriate types and evaluates the CQL library and
     * expressions specified. It then returns the CQL results as a FHIR Parameters
     * resource.
     * 
     * @param libraryIdentifier The Library to evaluate
     * @param contextParameter The context to use for evaluation (e.g. Patient=1234)
     * @param parameters The Library parameters (e.g. "Measurement Period", "Product Line", etc.)
     * @param expressions The expression of the Library to evaluate. If omitted all expressions are evaluated
     * @return The result of evaluation as FHIR Parameters
     */
    public IBaseParameters evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter,
            IBaseParameters parameters, Set<String> expressions) {

        Map<String, Object> evaluationParameters = this.cqlFhirParametersConverter.toCqlParameters(parameters);

        EvaluationResult result = this.cqlEvaluator.evaluate(libraryIdentifier, expressions, contextParameter,
                evaluationParameters);

        return this.cqlFhirParametersConverter.toFhirParameters(result);
    }
}