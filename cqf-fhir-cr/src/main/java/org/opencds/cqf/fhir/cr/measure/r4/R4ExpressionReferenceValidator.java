package org.opencds.cqf.fhir.cr.measure.r4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidationContext;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidator;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.ValidationIssue;
import org.opencds.cqf.fhir.cr.measure.common.ValidationResult;
import org.opencds.cqf.fhir.cr.measure.common.ValidationSeverity;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Validates that CQL expression names referenced in the Measure (population criteria, stratifiers,
 * supplemental data elements) exist in the primary CQL library. Parses the library's ELM JSON
 * content to extract top-level statement names. Produces {@code EXPRESSION_NOT_FOUND} warnings
 * since expressions may exist in included libraries not checked here.
 */
public class R4ExpressionReferenceValidator implements MeasureDefValidator {

    public static final String EXPRESSION_NOT_FOUND = "EXPRESSION_NOT_FOUND";
    private static final String ELM_JSON_CONTENT_TYPE = "application/elm+json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();
        var measure = (Measure) context.measure();

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            return result;
        }

        var url = measure.getLibrary().get(0).asStringValue();
        var expressionNames = extractExpressionNames(url, context);

        if (expressionNames == null) {
            // Could not parse library content; skip expression validation
            return result;
        }

        var measureDef = context.measureDef();

        // Validate population expressions
        for (int gi = 0; gi < measureDef.groups().size(); gi++) {
            GroupDef group = measureDef.groups().get(gi);

            for (PopulationDef pop : group.populations()) {
                validateExpression(
                        pop.expression(),
                        expressionNames,
                        "Measure.group[%d].population '%s'".formatted(gi, pop.id()),
                        result);
            }

            // Validate stratifier expressions
            for (StratifierDef strat : group.stratifiers()) {
                if (strat.expression() != null) {
                    validateExpression(
                            strat.expression(),
                            expressionNames,
                            "Measure.group[%d].stratifier '%s'".formatted(gi, strat.id()),
                            result);
                }
                for (var comp : strat.components()) {
                    if (comp.expression() != null) {
                        validateExpression(
                                comp.expression(),
                                expressionNames,
                                "Measure.group[%d].stratifier '%s' component '%s'".formatted(gi, strat.id(), comp.id()),
                                result);
                    }
                }
            }
        }

        // Validate SDE expressions
        for (SdeDef sde : measureDef.sdes()) {
            validateExpression(
                    sde.expression(), expressionNames, "Measure.supplementalData '%s'".formatted(sde.id()), result);
        }

        return result;
    }

    private void validateExpression(
            String expression, Set<String> expressionNames, String location, ValidationResult result) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        if (!expressionNames.contains(expression)) {
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.WARNING,
                    EXPRESSION_NOT_FOUND,
                    "Expression '%s' referenced in %s was not found in the primary CQL library"
                            .formatted(expression, location),
                    "Check that the expression name '%s' is defined in the CQL library and matches exactly (case-sensitive). "
                                    .formatted(expression)
                            + "The expression may exist in an included library.",
                    location));
        }
    }

    private Set<String> extractExpressionNames(String libraryUrl, MeasureDefValidationContext context) {
        var bundle = context.repository().search(Bundle.class, Library.class, Searches.byCanonical(libraryUrl), null);

        if (bundle.getEntry().isEmpty()) {
            return null;
        }

        var library = (Library) bundle.getEntryFirstRep().getResource();

        // Try to extract expression names from ELM JSON content
        for (Attachment content : library.getContent()) {
            if (ELM_JSON_CONTENT_TYPE.equals(content.getContentType()) && content.hasData()) {
                return parseElmExpressionNames(content.getData());
            }
        }

        // If no ELM content, try extracting from CQL content
        for (Attachment content : library.getContent()) {
            if ("text/cql".equals(content.getContentType()) && content.hasData()) {
                return parseCqlExpressionNames(content.getData());
            }
        }

        return null;
    }

    private Set<String> parseElmExpressionNames(byte[] elmData) {
        try {
            var root = OBJECT_MAPPER.readTree(elmData);
            var names = new HashSet<String>();

            // Navigate: library.statements.def[] -> extract "name" from each top-level definition
            var library = root.get("library");
            if (library == null) {
                return null;
            }

            var statements = library.get("statements");
            if (statements == null) {
                return null;
            }

            var def = statements.get("def");
            if (def == null || !def.isArray()) {
                return null;
            }

            for (JsonNode statement : def) {
                var nameNode = statement.get("name");
                if (nameNode != null && nameNode.isTextual()) {
                    names.add(nameNode.asText());
                }
            }

            return names.isEmpty() ? null : names;
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> parseCqlExpressionNames(byte[] cqlData) {
        try {
            var cql = new String(cqlData);
            var names = new HashSet<String>();

            var lines = cql.split("\n");
            for (var line : lines) {
                var trimmed = line.trim();
                if (trimmed.startsWith("define ") || trimmed.startsWith("define\t")) {
                    var afterDefine = trimmed.substring(7).trim();
                    // Remove optional "function" keyword
                    if (afterDefine.startsWith("function ")) {
                        afterDefine = afterDefine.substring(9).trim();
                    }
                    // Handle quoted name
                    if (afterDefine.startsWith("\"")) {
                        int endQuote = afterDefine.indexOf('"', 1);
                        if (endQuote > 0) {
                            names.add(afterDefine.substring(1, endQuote));
                        }
                    } else {
                        // Unquoted name ends at colon, paren, or whitespace
                        var nameEnd = afterDefine.length();
                        for (int i = 0; i < afterDefine.length(); i++) {
                            char c = afterDefine.charAt(i);
                            if (c == ':' || c == '(' || Character.isWhitespace(c)) {
                                nameEnd = i;
                                break;
                            }
                        }
                        if (nameEnd > 0) {
                            names.add(afterDefine.substring(0, nameEnd));
                        }
                    }
                }
            }

            return names.isEmpty() ? null : names;
        } catch (Exception e) {
            return null;
        }
    }
}
