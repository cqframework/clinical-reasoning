package org.opencds.cqf.fhir.cr.measure.common;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

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
     * Ignores debugResults and formats expression results with proper indentation.
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
                for (Object resource : expressionResult.getEvaluatedResources()) {
                    sb.append(indent(baseIndent + 2))
                            .append(formatResource(resource))
                            .append("\n");
                }
            }

            // Format value
            Object value = expressionResult.getValue();
            sb.append(indent(baseIndent + 1)).append("Value: ");
            sb.append(formatValue(value)).append("\n");
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
        return formatValue(value);
    }

    /**
     * Formats a value according to its type: collections, resources, primitives, dates, etc.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        // Handle iterables and collections
        if (value instanceof Iterable<?> iterable) {
            String items = StreamSupport.stream(iterable.spliterator(), false)
                    .map(EvaluationResultFormatter::formatSingleValue)
                    .collect(Collectors.joining(", "));
            return "[" + items + "]";
        }

        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> "%s -> %s"
                            .formatted(formatSingleValue(entry.getKey()), formatSingleValue(entry.getValue())))
                    .collect(Collectors.joining(", "));
        }

        return formatSingleValue(value);
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

    public static Object printSubjectResources(PopulationDef populationDef, String subjectId) {
        if (populationDef == null) {
            return "{empty}";
        }

        final Set<Object> resources = populationDef.getSubjectResources().get(subjectId);

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

    public static String printValues(Collection<Object> values) {

        if (values == null || values.isEmpty()) {
            return "{empty}";
        }

        return values.stream().map(EvaluationResultFormatter::printValue).collect(Collectors.joining(", "));
    }

    public static String printValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof IBaseResource resource) {
            return resource.getIdElement().getValueAsString();
        }

        if (value instanceof Map<?, ?> map) {
            final String toString = map.entrySet().stream()
                    .map(entry -> printValue(entry.getKey()) + " -> " + printValue(entry.getValue()))
                    .collect(Collectors.joining(", "));

            if (StringUtils.isBlank(toString)) {
                return "{empty}";
            }

            return toString;
        }

        return value.toString();
    }
}
