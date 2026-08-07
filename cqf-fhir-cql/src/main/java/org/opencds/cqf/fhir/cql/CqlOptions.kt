package org.opencds.cqf.fhir.cql;

import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.CqlCompilerOptions.Options;
import org.cqframework.cql.cql2elm.LibraryBuilder.SignatureLevel;

// TODO: Migrate upstream to engine project. Or is it duplicated already?
public class CqlOptions {
    private CqlCompilerOptions cqlCompilerOptions = CqlCompilerOptions.defaultOptions();
    private CqlEngineOptions cqlEngineOptions = CqlEngineOptions.defaultOptions();
    private boolean useEmbeddedLibraries = true;

    public CqlCompilerOptions getCqlCompilerOptions() {
        return this.cqlCompilerOptions;
    }

    public void setCqlCompilerOptions(CqlCompilerOptions cqlCompilerOptions) {
        this.cqlCompilerOptions = cqlCompilerOptions;
    }

    public CqlEngineOptions getCqlEngineOptions() {
        return this.cqlEngineOptions;
    }

    public CqlOptions setCqlEngineOptions(CqlEngineOptions cqlEngineOptions) {
        this.cqlEngineOptions = cqlEngineOptions;
        return this;
    }

    public boolean useEmbeddedLibraries() {
        return this.useEmbeddedLibraries;
    }

    public CqlOptions setUseEmbeddedLibraries(boolean useEmbeddedLibraries) {
        this.useEmbeddedLibraries = useEmbeddedLibraries;
        return this;
    }

    public static CqlOptions defaultOptions() {
        var opt = new CqlOptions();
        opt.getCqlCompilerOptions().setSignatureLevel(SignatureLevel.All);
        opt.getCqlCompilerOptions().getOptions().add(Options.EnableLocators);
        opt.getCqlCompilerOptions().getOptions().add(Options.EnableAnnotations);
        return opt;
    }
}
