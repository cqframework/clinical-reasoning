package org.opencds.cqf.cql.evaluator.builder.context.interface;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;

import java.util.EnumSet;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslator.Options;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface LibraryContext {

    public BuilderContext withLibraryLoader(List<String> libraries);
    public BuilderContext withLibraryLoader(IBaseBundle bundleOfLibraries);
    public BuilderContext withLibraryLoader(String library);
    public BuilderContext withLibraryLoader(LibraryLoader libraryLoader);
    public EnumSet<Options> getTranslatorOptions();
    public BuilderContext setTranslatorOptions(EnumSet<Options> translatorOptions);
}
