package org.opencds.cqf.cql.evaluator.library;

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
