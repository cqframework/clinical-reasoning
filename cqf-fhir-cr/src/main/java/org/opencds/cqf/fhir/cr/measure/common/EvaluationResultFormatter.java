package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.List;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * Utility class for formatting EvaluationResult objects into human-readable strings.
 */
public class EvaluationResultFormatter {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd:HH:mm:ss";
    private static final String INDENT = "  ";

    private EvaluationResultFormatter() {
        // Static utility class
    }

    /**
     * Formats an EvaluationResult into a human-readable, indented string.
     * Formats expression results with proper indentation. Debug results and trace
     * information are not included; use {@link #format(EvaluationResult, int, boolean)}
     * with {@code includeDebugInfo=true} to include them.
     *
     * @param evaluationResult the EvaluationResult to format
     * @param baseIndent the base indentation level (number of indent units)
     * @return formatted string representation
     */
    public static String format(EvaluationResult evaluationResult, int baseIndent) {
        if (evaluationResult == null) {
            return indent(baseIndent) + "null";
        }

        Map<String, ExpressionResult> expressionResults = evaluationResult.getExpressionResults();
        if (expressionResults.isEmpty()) {
            return indent(baseIndent) + "(no expression results)";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ExpressionResult> entry : expressionResults.entrySet()) {
            String expressionName = entry.getKey();
            ExpressionResult expressionResult = entry.getValue();

            sb.append(indent(baseIndent))
                    .append("Expression: \"")
                    .append(expressionName)
                    .append("\"\n");

            if (expressionResult == null) {
                sb.append(indent(baseIndent + 1))
                        .append("(null expression result)")
                        .append("\n");
                continue;
            }

            // Format evaluated resources
            if (expressionResult.getEvaluatedResources() != null
                    && !expressionResult.getEvaluatedResources().isEmpty()) {
                sb.append(indent(baseIndent + 1)).append("Evaluated Resources:\n");
                for (var resource : expressionResult.getEvaluatedResources()) {
                    sb.append(indent(baseIndent + 2))
                            .append(formatResource(resource))
                            .append("\n");
                }
            }

            // Format value
            var value = expressionResult.getValue();
            sb.append(indent(baseIndent + 1)).append("Value: ");
            sb.append(formatValue(value)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats an EvaluationResult into a human-readable string, including debug
     * results and trace information when present and requested.
     *
     * @param evaluationResult the EvaluationResult to format
     * @param baseIndent the base indentation level (number of indent units)
     * @param includeDebugInfo whether to include debug result and trace information
     * @return formatted string representation
     */
    public static String format(EvaluationResult evaluationResult, int baseIndent, boolean includeDebugInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(format(evaluationResult, baseIndent));

        if (includeDebugInfo && evaluationResult != null) {
            if (evaluationResult.getDebugResult() != null) {
                sb.append(indent(baseIndent))
                        .append("Debug Result: ")
                        .append(evaluationResult.getDebugResult())
                        .append("\n");
            }
            if (evaluationResult.getTrace() != null) {
                sb.append(indent(baseIndent))
                        .append("Trace: ")
                        .append(evaluationResult.getTrace())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Formats an expression result value according to its type.
     * Public API for formatting values from ExpressionResult.getValue().
     *
     * @param value the value to format (may be a collection, resource, primitive, date, etc.)
     * @return formatted string representation
     */
    public static String formatExpressionValue(Value value) {
        return formatValue(value);
    }

    /**
     * Formats a value according to its type: collections, resources, primitives, dates, etc.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private static String formatValue(Value value) {
        if (value == null) {
            return "null";
        }

        // Handle iterables and collections
        if (value instanceof List list) {
            String items = StreamSupport.stream(list.spliterator(), false)
                    .map(EvaluationResultFormatter::formatSingleValue)
                    .collect(Collectors.joining(", "));
            return "[" + items + "]";
        }

        //        if (value instanceof Map<?, ?> map) {
        //            return map.entrySet().stream()
        //                    .map(entry -> "%s -> %s"
        //                            .formatted(formatSingleValue(entry.getKey()),
        // formatSingleValue(entry.getValue())))
        //                    .collect(Collectors.joining(", "));
        //        }

        return formatSingleValue(value);
    }

    /**
     * Formats a single value (non-collection) according to its type.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private static String formatSingleValue(Value value) {
        if (value == null) {
            return "null";
        }

        // Handle FHIR resources
        if (value instanceof ClassInstance classInstance) {
            return formatResource(value);
        }

        // Handle dates
        //        if (value instanceof LocalDate) {
        //            return ((LocalDate) value).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        //        }
        //        if (value instanceof LocalDateTime) {
        //            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        //        }
        //        if (value instanceof Date) {
        //            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        //            return formatter.format((Date) value);
        //        }

        // Fallback to toString for other types
        return value.toString();
    }

    /**
     * Formats a resource ID, extracting the unversioned resource type and ID.
     *
     * @param resource the resource object
     * @return formatted resource ID string (e.g., "Encounter/patient-4-encounter-1")
     */
    public static String formatResource(Value resource) {
        if (!(resource instanceof ClassInstance classInstance)) {
            return resource.toString();
        }

        var id = classInstance.get("id");
        if (id == null || StringUtils.isBlank(id.toString())) {
            return "(resource with no ID)";
        } else {
            return id.toString();
        }
    }

    /**
     * Creates an indentation string for the specified level.
     *
     * @param level the indentation level
     * @return indentation string
     */
    private static String indent(int level) {
        return INDENT.repeat(Math.max(0, level));
    }

    public static String printSubjectResources(PopulationDef populationDef, String subjectId) {
        if (populationDef == null) {
            return "{empty}";
        }

        final Set<Value> resources = populationDef.getSubjectResources().get(subjectId);

        if (CollectionUtils.isEmpty(resources)) {
            return subjectId + ": {empty}";
        }

        final String toString =
                resources.stream().map(EvaluationResultFormatter::printValue).collect(Collectors.joining(", "));

        if (StringUtils.isBlank(toString)) {
            return subjectId + ": {empty}";
        }

        return subjectId + ": " + toString;
    }

    public static String printValues(Collection<Value> values) {

        if (values == null || values.isEmpty()) {
            return "{empty}";
        }

        return values.stream().map(EvaluationResultFormatter::printValue).collect(Collectors.joining(", "));
    }

    public static String printValue(Value value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof ClassInstance classInstance && classInstance.has("id")) {
            return "%s/%s".formatted(classInstance.getTypeAsString(), classInstance.get("id"));
        }
        //        if (value instanceof IBaseResource resource) {
        //            return resource.getIdElement().getValueAsString();
        //        }

        if (value instanceof Tuple map) {
            final String toString = map.getElements().entrySet().stream()
                    .map(entry -> entry.getKey() + " -> " + printValue(entry.getValue()))
                    .collect(Collectors.joining(", "));

            if (StringUtils.isBlank(toString)) {
                return "{empty}";
            }

            return toString;
        }

        return value.toString();
    }
}
