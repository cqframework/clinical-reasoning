package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This class is used to contain the various properties of a CqfExpression with an alternate so that
 * it can be used in version agnostic logic.
 */
public class CqfExpression {

    private String language;
    private String expression;
    private Map<String, String> referencedLibraries;
    private String libraryUrl;
    private String altLanguage;
    private String altExpression;
    private String altLibraryUrl;
    private String name;

    public static CqfExpression of(IBaseExtension<?, ?> extension, Map<String, String> referencedLibraries) {
        if (extension == null) {
            return null;
        }
        var fhirPackagePath = "org.hl7.fhir.";
        var className = extension.getClass().getCanonicalName();
        var modelSplit = className.split(fhirPackagePath);
        if (modelSplit.length < 2) {
            throw new IllegalArgumentException();
        }
        var model = modelSplit[1];
        model = model.substring(0, model.indexOf(".")).toUpperCase();
        var version = FhirVersionEnum.forVersionString(model);
        return switch (version) {
            case DSTU3 ->
                new CqfExpression("text/cql-expression", extension.getValue().toString(), referencedLibraries);
            case R4 -> CqfExpression.of((org.hl7.fhir.r4.model.Expression) extension.getValue(), referencedLibraries);
            case R5 -> CqfExpression.of((org.hl7.fhir.r5.model.Expression) extension.getValue(), referencedLibraries);
            default -> null;
        };
    }

    public static CqfExpression of(
            org.hl7.fhir.r4.model.Expression expression, Map<String, String> referencedLibraries) {
        if (expression == null) {
            return null;
        }
        var altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT);
        var altExpression =
                altExpressionExt == null ? null : (org.hl7.fhir.r4.model.Expression) altExpressionExt.getValue();
        return new CqfExpression(
                expression.getLanguage(),
                expression.getExpression(),
                referencedLibraries,
                expression.getReference(),
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference() ? altExpression.getReference() : null,
                expression.getName());
    }

    public static CqfExpression of(
            org.hl7.fhir.r5.model.Expression expression, Map<String, String> referencedLibraries) {
        if (expression == null) {
            return null;
        }
        var altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT);
        var altExpression =
                altExpressionExt == null ? null : (org.hl7.fhir.r5.model.Expression) altExpressionExt.getValue();
        return new CqfExpression(
                expression.getLanguage(),
                expression.getExpression(),
                referencedLibraries,
                expression.getReference(),
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference() ? altExpression.getReference() : null,
                expression.getName());
    }

    public CqfExpression() {}

    public CqfExpression(String language, String expression, Map<String, String> referencedLibraries) {
        this(language, expression, referencedLibraries, null, null, null, null, null);
    }

    public CqfExpression(
            String language,
            String expression,
            Map<String, String> referencedLibraries,
            String libraryUrl,
            String altLanguage,
            String altExpression,
            String altLibraryUrl,
            String name) {
        this.language = language;
        this.expression = expression;
        this.referencedLibraries = referencedLibraries;
        this.libraryUrl = libraryUrl;
        this.altLanguage = altLanguage;
        this.altExpression = altExpression;
        this.altLibraryUrl = altLibraryUrl;
        this.name = name;
    }

    private String resolveLibrary(String lang, String url, String expr) {
        // If the expression is FHIRPath or a raw CQL expression a wrapper Library will be created
        if (List.of("text/cql.expression", "text/cql-expression", "text/fhirpath")
                .contains(lang)) {
            return null;
        }
        if (expr.contains(".") && lang.equals("text/cql") && StringUtils.isBlank(libraryUrl)) {
            return null;
        }
        // If the expression has a reference use it
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        // If the expression is an identifier and has no reference there should be a single referenced Library
        if (referencedLibraries != null && !referencedLibraries.isEmpty()) {
            return referencedLibraries.values().stream().findFirst().get();
        }
        throw new IllegalArgumentException("No Library reference found for expression: %s".formatted(expr));
    }

    public String getName() {
        return name;
    }

    public CqfExpression setName(String name) {
        this.name = name;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public CqfExpression setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public CqfExpression setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public Map<String, String> getReferencedLibraries() {
        return referencedLibraries;
    }

    public CqfExpression setReferencedLibraries(Map<String, String> referencedLibraries) {
        this.referencedLibraries = referencedLibraries;
        return this;
    }

    public String getLibraryUrl() {
        return resolveLibrary(language, libraryUrl, expression);
    }

    public CqfExpression setLibraryUrl(String libraryUrl) {
        this.libraryUrl = libraryUrl;
        return this;
    }

    public String getAltLanguage() {
        return altLanguage;
    }

    public CqfExpression setAltLanguage(String altLanguage) {
        this.altLanguage = altLanguage;
        return this;
    }

    public String getAltExpression() {
        return altExpression;
    }

    public CqfExpression setAltExpression(String altExpression) {
        this.altExpression = altExpression;
        return this;
    }

    public String getAltLibraryUrl() {
        return StringUtils.isBlank(altExpression) ? null : resolveLibrary(altLanguage, altLibraryUrl, altExpression);
    }

    public CqfExpression setAltLibraryUrl(String altLibraryUrl) {
        this.altLibraryUrl = altLibraryUrl;
        return this;
    }

    public ICompositeType toExpressionType(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case R4 ->
                new org.hl7.fhir.r4.model.Expression()
                        .setLanguage(language)
                        .setExpression(expression)
                        .setReference(libraryUrl)
                        .setName(name);
            case R5 ->
                new org.hl7.fhir.r5.model.Expression()
                        .setLanguage(language)
                        .setExpression(expression)
                        .setReference(libraryUrl)
                        .setName(name);
            default -> null;
        };
    }
}
