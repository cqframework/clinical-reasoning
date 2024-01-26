package org.opencds.cqf.fhir.cql;

import jakarta.annotation.Nonnull;

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

    public CqfExpression() {}

    public CqfExpression(
            @Nonnull org.hl7.fhir.r4.model.Expression expression,
            String defaultLibraryUrl,
            org.hl7.fhir.r4.model.Expression altExpression) {
        this(
                expression.getLanguage(),
                expression.getExpression(),
                expression.hasReference() ? expression.getReference() : defaultLibraryUrl,
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference()
                        ? altExpression.getReference()
                        : defaultLibraryUrl);
    }

    public CqfExpression(
            @Nonnull org.hl7.fhir.r5.model.Expression expression,
            String defaultLibraryUrl,
            org.hl7.fhir.r5.model.Expression altExpression) {
        this(
                expression.getLanguage(),
                expression.getExpression(),
                expression.hasReference() ? expression.getReference() : defaultLibraryUrl,
                altExpression != null ? altExpression.getLanguage() : null,
                altExpression != null ? altExpression.getExpression() : null,
                altExpression != null && altExpression.hasReference()
                        ? altExpression.getReference()
                        : defaultLibraryUrl);
    }

    public CqfExpression(String language, String expression, String libraryUrl) {
        this(language, expression, libraryUrl, null, null, null);
    }

    public CqfExpression(
            String language,
            String expression,
            String libraryUrl,
            String altLanguage,
            String altExpression,
            String altLibraryUrl) {
        this.language = language;
        this.expression = expression;
        this.libraryUrl = libraryUrl;
        this.altLanguage = altLanguage;
        this.altExpression = altExpression;
        this.altLibraryUrl = altLibraryUrl;
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
}
