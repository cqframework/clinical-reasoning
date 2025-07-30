package org.opencds.cqf.fhir.cr.measure.common;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

// LUKETODO:  consider deleting this once all is said and done
public class EvaluationResultsDisplay {
    public static String printEvaluationResult(EvaluationResult evaluationResult) {
        if (evaluationResult == null) {
            return "null";
        }
        return "\nEvaluationResult{" + "expressionResults=\n"
                + evaluationResult.expressionResults.entrySet().stream()
                        .map(entry -> new DefaultMapEntry<>(entry.getKey(), printExpressionResult(entry.getValue())))
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"))
                + "}\n";
    }

    public static String printExpressionResult(ExpressionResult expressionResult) {
        if (expressionResult == null) {
            return "\nnull";
        }

        return "\nExpressionResult{\n    value=" + showValue(expressionResult.value()) + "\n    type="
                + showEvaluatedResources(expressionResult.evaluatedResources()) + '}';
    }

    public static String showValue(Object valueOrCollection) {
        if (valueOrCollection == null) {
            return "null";
        }
        if (valueOrCollection instanceof Iterable<?> iterable) {
            return showEvaluatedResources(iterable);
        }
        return showEvaluatedResource(valueOrCollection);
    }

    public static String showEvaluatedResources(Iterable<?> evaluatedResourcesOrSomethings) {
        return StreamSupport.stream(evaluatedResourcesOrSomethings.spliterator(), false)
                .map(EvaluationResultsDisplay::showEvaluatedResource)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String showEvaluatedResource(Object evaluatedResourceOrSomething) {
        if (evaluatedResourceOrSomething instanceof IBaseResource resource) {
            return resource.getIdElement().getValueAsString();
        } else if (evaluatedResourceOrSomething != null) {
            return evaluatedResourceOrSomething.toString();
        } else {
            return "null";
        }
    }
}
