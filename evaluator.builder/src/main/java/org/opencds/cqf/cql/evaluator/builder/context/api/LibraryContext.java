package org.opencds.cqf.cql.evaluator.builder.context.api;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface LibraryContext {
    public BuilderContext withStringLibraryLoader(List<String> libraryContent);
    public BuilderContext withFileLibraryLoader(List<String> libraryContent);
    public BuilderContext withBundleLibraryLoader(IBaseBundle bundle);
    public BuilderContext withSingleStringLibraryLoader(String library);
    public BuilderContext withSingleFileLibraryLoader(String library);
    public BuilderContext withRemoteLibraryLoader(URL libraryUrl) throws IOException, InterruptedException, URISyntaxException;
    public BuilderContext withPreConfiguredLibraryLoader(LibraryLoader libraryLoader);
}
