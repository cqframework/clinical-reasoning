package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;

class LibraryEvaluator {

    // private static Logger logger = LoggerFactory.getLogger(LibraryEvaluator.class);

    private CqlEvaluator cqlEvaluator;

    private CqlFhirParametersConverter cqlFhirParametersConverter;

    @Inject
    LibraryEvaluator(CqlFhirParametersConverter cqlFhirParametersConverter, CqlEvaluator cqlEvaluator) {
        this.cqlFhirParametersConverter = requireNonNull(cqlFhirParametersConverter, "cqlFhirParametersConverter can not be null");
        this.cqlEvaluator = requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
    }

    public IBaseParameters evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter,
            IBaseParameters parameters, Set<String> expressions) {

        Map<String, Object> evaluationParameters = this.cqlFhirParametersConverter.toCqlParameters(parameters);

        EvaluationResult result = this.cqlEvaluator.evaluate(libraryIdentifier, expressions, contextParameter,
                evaluationParameters);

        return this.cqlFhirParametersConverter.toFhirParameters(result);
    }
}