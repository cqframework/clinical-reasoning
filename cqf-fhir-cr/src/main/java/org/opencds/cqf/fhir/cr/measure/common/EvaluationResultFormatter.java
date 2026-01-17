package org.opencds.cqf.fhir.cr.measure.common;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
        if (expressionResults == null || expressionResults.isEmpty()) {
            return indent(baseIndent) + "(no expression results)";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ExpressionResult> entry : expressionResults.entrySet()) {
            String expressionName = entry.getKey();
            ExpressionResult expressionResult = entry.getValue();

            sb.append(indent(baseIndent))
                    .append("Expression: ")
                    .append(expressionName)
                    .append("\n");

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
                            .append(formatResourceId(resource))
                            .append("\n");
                }
            }

            // Format value
            Object value = expressionResult.getValue();
            sb.append(indent(baseIndent + 1)).append("Value: ");
            sb.append(formatValue(value, baseIndent + 2)).append("\n");
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
        return formatValue(value, 0);
    }

    /**
     * Formats a value according to its type: collections, resources, primitives, dates, etc.
     *
     * @param value the value to format
     * @param indentLevel the indentation level for multi-line values
     * @return formatted string representation
     */
    private static String formatValue(Object value, int indentLevel) {
        if (value == null) {
            return "null";
        }

        // Handle iterables and collections
        if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) value;
            String items = StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> formatSingleValue(item))
                    .collect(Collectors.joining(", "));
            return "[" + items + "]";
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
            return formatResourceId(value);
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

        // Handle primitives and strings
        if (isPrimitiveOrString(value)) {
            return value.toString();
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
    private static String formatResourceId(Object resource) {
        if (!(resource instanceof IBaseResource)) {
            return resource.toString();
        }

        IBaseResource baseResource = (IBaseResource) resource;
        if (baseResource.getIdElement() == null || baseResource.getIdElement().getValue() == null) {
            return "(resource with no ID)";
        }

        return baseResource.getIdElement().toUnqualifiedVersionless().getValue();
    }

    /**
     * Checks if a value is a primitive type or String.
     *
     * @param value the value to check
     * @return true if primitive or String, false otherwise
     */
    private static boolean isPrimitiveOrString(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value.getClass().isPrimitive();
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
}
