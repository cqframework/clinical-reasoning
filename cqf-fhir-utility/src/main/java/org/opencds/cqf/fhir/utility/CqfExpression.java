package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This class is used to contain the various properties of a CqfExpression with an alternate so that
 * it can be used in version agnostic logic.
 */
public class CqfExpression {

    private String language;
    private String expression;
    private String libraryUrl;
    private String altLanguage;
    private String altExpression;
    private String altLibraryUrl;
    private String name;

    public static CqfExpression of(IBaseExtension<?, ?> extension, String defaultLibraryUrl) {
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
        switch (version) {
            case DSTU3:
                return new CqfExpression(
                        "text/cql.expression", extension.getValue().toString(), defaultLibraryUrl);
            case R4:
                return CqfExpression.of((org.hl7.fhir.r4.model.Expression) extension.getValue(), defaultLibraryUrl);
            case R5:
                return CqfExpression.of((org.hl7.fhir.r5.model.Expression) extension.getValue(), defaultLibraryUrl);

            default:
                return null;
        }
    }

    public static CqfExpression of(org.hl7.fhir.r4.model.Expression expression, String defaultLibraryUrl) {
        if (expression == null) {
            return null;
        }
        var altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT);
        var altExpression =
                altExpressionExt == null ? null : (org.hl7.fhir.r4.model.Expression) altExpressionExt.getValue();
        return new CqfExpression(
                expression.getLanguage(),
                expression.getExpression(),
                expression.hasReference() ? expression.getReference() : defaultLibraryUrl,
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference() ? altExpression.getReference() : null,
                expression.getName());
    }

    public static CqfExpression of(org.hl7.fhir.r5.model.Expression expression, String defaultLibraryUrl) {
        if (expression == null) {
            return null;
        }
        var altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT);
        var altExpression =
                altExpressionExt == null ? null : (org.hl7.fhir.r5.model.Expression) altExpressionExt.getValue();
        return new CqfExpression(
                expression.getLanguage(),
                expression.getExpression(),
                expression.hasReference() ? expression.getReference() : defaultLibraryUrl,
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference() ? altExpression.getReference() : null,
                expression.getName());
    }

    public CqfExpression() {}

    public CqfExpression(String language, String expression, String libraryUrl) {
        this(language, expression, libraryUrl, null, null, null, null);
    }

    public CqfExpression(
            String language,
            String expression,
            String libraryUrl,
            String altLanguage,
            String altExpression,
            String altLibraryUrl,
            String name) {
        this.language = language;
        this.expression = expression;
        this.libraryUrl = libraryUrl;
        this.altLanguage = altLanguage;
        this.altExpression = altExpression;
        this.altLibraryUrl = altLibraryUrl;
        this.name = name;
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

    public String getLibraryUrl() {
        return libraryUrl;
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
        return altLibraryUrl;
    }

    public CqfExpression setAltLibraryUrl(String altLibraryUrl) {
        this.altLibraryUrl = altLibraryUrl;
        return this;
    }

    public ICompositeType toExpressionType(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case R4:
                return new org.hl7.fhir.r4.model.Expression()
                        .setLanguage(language)
                        .setExpression(expression)
                        .setReference(libraryUrl)
                        .setName(name);
            case R5:
                return new org.hl7.fhir.r5.model.Expression()
                        .setLanguage(language)
                        .setExpression(expression)
                        .setReference(libraryUrl)
                        .setName(name);

            default:
                return null;
        }
    }
}
