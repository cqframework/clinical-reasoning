package org.opencds.cqf.cql.evaluator.builder.context;

import org.opencds.cqf.cql.execution.LibraryLoader;

import java.util.Objects;

import org.cqframework.cql.cql2elm.CqlTranslator.Options;

public class BuilderLibraryContext {
    // All the different ways we can load libraries:
    // 1. A pre-constructed library loader, and a reference to the primary library
    // 2. Strings of CQL content (and if there's more than one library, a reference to the primary library)
    // 3. A remote library repository, and reference to the primary library
    // 4. A filesystem with library content, and a reference to the primary library
    // 5. A Bundle with FHIR Libraries

    public LibraryLoader buildLibraryLoader() {
        return null;
    }

    private Options translatorOptions;
    public Options getTranslatorOptions() {
        return this.translatorOptions;
    }

    public void setTranslatorOptions(Options translatorOptions) {
        Objects.requireNonNull(translatorOptions, "translatorOptions can not be null.");
        this.translatorOptions = translatorOptions;
    }
}