package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cql.ClassInstanceHelper.getId;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * Utility class for formatting EvaluationResult objects into human-readable strings.
 */
public class EvaluationResultFormatter {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd:HH:mm:ss";
    private static final String INDENT = "  ";
    private static final String SEPARATOR = "----------------------------------------";

    private EvaluationResultFormatter() {
        // Static utility class
    }

    /**
     * Formats an EvaluationResult into a human-readable, indented string.
     * Formats expression results with proper indentation. Debug results and trace
     * information are not included; use {@link #format(CqlEvaluationResult, int, boolean)}
     * with {@code includeDebugInfo=true} to include them.
     *
     * @param evaluationResult the EvaluationResult to format
     * @param baseIndent the base indentation level (number of indent units)
     * @return formatted string representation
     */
    public static String format(CqlEvaluationResult evaluationResult, int baseIndent) {
        if (evaluationResult == null) {
            return indent(baseIndent) + "null";
        }

        var expressionResults = evaluationResult.getExpressionResults();
        if (expressionResults.isEmpty()) {
            return indent(baseIndent) + "(no expression results)";
        }

        StringBuilder sb = new StringBuilder();
        for (var expressionResult : expressionResults) {
            var expressionName = expressionResult.expressionName();

            sb.append(indent(baseIndent))
                    .append("Expression: \"")
                    .append(expressionName)
                    .append("\"\n");

            // Format evaluated resources
            if (expressionResult.evaluatedResources() != null
                    && !expressionResult.evaluatedResources().isEmpty()) {
                sb.append(indent(baseIndent + 1)).append("Evaluated Resources:\n");
                for (var resource : expressionResult.evaluatedResources()) {
                    sb.append(indent(baseIndent + 2))
                            .append(formatResource(resource))
                            .append("\n");
                }
            }

            // Format value
            // Object value = expressionResult.raw();
            sb.append(indent(baseIndent + 1)).append("Value: ");
            sb.append(formatValue(expressionResult)).append("\n");
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
    public static String format(CqlEvaluationResult evaluationResult, int baseIndent, boolean includeDebugInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(format(evaluationResult, baseIndent));

        if (includeDebugInfo && evaluationResult != null) {
            if (evaluationResult.getResult().getDebugResult() != null) {
                sb.append(indent(baseIndent))
                        .append("Debug Result: ")
                        .append(evaluationResult.getResult().getDebugResult())
                        .append("\n");
            }
            if (evaluationResult.getResult().getTrace() != null) {
                sb.append(indent(baseIndent))
                        .append("Trace: ")
                        .append(evaluationResult.getResult().getTrace())
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
    public static String formatExpressionValue(Object value) {
        return formatValue(CqlExpressionValue.ofRaw(null, value, null));
    }

    /**
     * Formats a value according to its type: collections, resources, primitives, dates, etc.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private static String formatValue(CqlExpressionValue value) {
        // var wrapper = CqlExpressionValue.ofRaw(null, value, null);
        if (value == null || value.isNull()) {
            return "null";
        }

        // Handle iterables and collections
        if (value.isIterable()) {
            String items = StreamSupport.stream(value.asIterable().spliterator(), false)
                    .map(EvaluationResultFormatter::formatSingleValue)
                    .collect(Collectors.joining(", "));
            return "[" + items + "]";
        }

        return value.asMap()
                .map(map -> map.entrySet().stream()
                        .map(entry -> "%s -> %s"
                                .formatted(formatSingleValue(entry.getKey()), formatSingleValue(entry.getValue())))
                        .collect(Collectors.joining(", ")))
                .orElseGet(() -> formatSingleValue(value));
    }

    /**
     * Formats a single value (non-collection) according to its type.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private static String formatSingleValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof CqlExpressionValue expressionValue) {
            return formatSingleValue(expressionValue.raw());
        }

        // Handle FHIR resources
        if (value instanceof IBaseResource) {
            return formatResource(value);
        }

        // Handle dates
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        }
        if (value instanceof Date) {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
            return formatter.format((Date) value);
        }
        if (value instanceof org.opencds.cqf.cql.engine.runtime.Date cqlDate) {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
            return formatter.format(cqlDate.toJavaDate());
        }
        if (value instanceof ClassInstance classInstance) {
            var id = getId(classInstance);
            if (StringUtils.isNotBlank(id)) {
                return id;
            }
        }

        // CQL-5 primitive SimpleValues render their plain underlying value, not the CQL-quoted
        // toString() form (a CQL String prints as 'x', an Integer/Decimal carry no quoting issue but
        // are handled uniformly here for consistency).
        if (value instanceof org.opencds.cqf.cql.engine.runtime.String cqlString) {
            return cqlString.getValue();
        }
        if (value instanceof org.opencds.cqf.cql.engine.runtime.Boolean cqlBoolean) {
            return String.valueOf(cqlBoolean.getValue());
        }
        if (value instanceof org.opencds.cqf.cql.engine.runtime.Integer cqlInteger) {
            return String.valueOf(cqlInteger.getValue());
        }
        if (value instanceof org.opencds.cqf.cql.engine.runtime.Long cqlLong) {
            return String.valueOf(cqlLong.getValue());
        }
        if (value instanceof org.opencds.cqf.cql.engine.runtime.Decimal cqlDecimal) {
            return String.valueOf(cqlDecimal.getValue());
        }

        // Fallback to toString for other types
        return value.toString();
    }

    /**
     * Formats a resource ID, extracting the unversioned resource type and ID.
     *
     * @param resource the resource object
     * @return formatted resource ID string (e.g., "Encounter/patient-4-encounter-1")
     */
    public static String formatResource(Object resource) {
        if (resource instanceof ClassInstance classInstance) {
            var id = getId(classInstance);
            if (StringUtils.isNotBlank(id)) {
                return id;
            }
        }
        if (!(resource instanceof IBaseResource baseResource)) {
            return resource.toString();
        }

        if (baseResource.getIdElement() == null || baseResource.getIdElement().getValue() == null) {
            return "(resource with no ID)";
        }

        return baseResource.getIdElement().toUnqualifiedVersionless().getValue();
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

    /**
     * Formats evaluation results for a single measure, with separator lines between subjects.
     *
     * @param measureId the measure ID for the header
     * @param evaluationResults map of subject ID to EvaluationResult
     * @return formatted string
     */
    public static String formatMeasureEvaluationResults(
            String measureId, Map<String, CqlEvaluationResult> evaluationResults) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append("Evaluation Results for Measure: ").append(measureId).append("\n");
        sb.append(SEPARATOR).append("\n");

        if (evaluationResults.isEmpty()) {
            sb.append("  (no evaluation results available)\n");
        } else {
            boolean first = true;
            for (var entry : evaluationResults.entrySet()) {
                if (!first) {
                    sb.append("  ").append(SEPARATOR).append("\n");
                }
                first = false;
                sb.append("  Subject: ").append(entry.getKey()).append("\n");
                sb.append(format(entry.getValue(), 2));
            }
        }

        sb.append(SEPARATOR).append("\n");
        return sb.toString();
    }

    public static Object printSubjectResources(PopulationDef populationDef, String subjectId) {
        if (populationDef == null) {
            return "{empty}";
        }

        final Set<CqlExpressionValue> resources =
                populationDef.getSubjectResources().get(subjectId);

        if (resources == null || resources.isEmpty()) {
            return subjectId + ": {empty}";
        }

        final String toString = resources.stream()
                .filter(Objects::nonNull)
                .map(CqlExpressionValue::raw)
                .map(EvaluationResultFormatter::printValue)
                .collect(Collectors.joining(", "));

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

    public static String printValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof ClassInstance classInstance) {
            var id = getId(classInstance);
            if (StringUtils.isNotBlank(id)) {
                return id;
            }
        }

        if (value instanceof IBaseResource resource) {
            return resource.getIdElement().getValueAsString();
        }

        return CqlExpressionValue.ofRaw(null, value, null)
                .asMap()
                .map(map -> {
                    final String toString = map.entrySet().stream()
                            .map(entry -> printValue(entry.getKey()) + " -> " + printValue(entry.getValue()))
                            .collect(Collectors.joining(", "));
                    return StringUtils.isBlank(toString) ? "{empty}" : toString;
                })
                .orElseGet(value::toString);
    }
}
