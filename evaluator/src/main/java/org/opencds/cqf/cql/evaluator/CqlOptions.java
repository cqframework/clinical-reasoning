package org.opencds.cqf.cql.evaluator;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.opencds.cqf.cql.evaluator.engine.CqlEngineOptions;

public class CqlOptions {
  private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();
  private CqlEngineOptions cqlEngineOptions = CqlEngineOptions.defaultOptions();
  private boolean useEmbeddedLibraries = true;

  public CqlTranslatorOptions getCqlTranslatorOptions() {
    return this.cqlTranslatorOptions;
  }

  public void setCqlTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
    this.cqlTranslatorOptions = cqlTranslatorOptions;
  }

  public CqlEngineOptions getCqlEngineOptions() {
    return this.cqlEngineOptions;
  }

  public void setCqlEngineOptions(CqlEngineOptions cqlEngineOptions) {
    this.cqlEngineOptions = cqlEngineOptions;
  }

  public boolean useEmbeddedLibraries() {
    return this.useEmbeddedLibraries;
  }

  public void setUseEmbeddedLibraries(boolean useEmbeddedLibraries) {
    this.useEmbeddedLibraries = useEmbeddedLibraries;
  }

  public static CqlOptions defaultOptions() {
    var opt = new CqlOptions();
    // opt.getCqlTranslatorOptions().getOptions().add(Options.EnableLocators);
    // opt.getCqlTranslatorOptions().getOptions().add(Options.EnableAnnotations);
    return opt;
  }
}
