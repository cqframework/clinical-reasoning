package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CqlEngineCloseable implements AutoCloseable {

    private final CqlEngine context;
    private final List<CompiledLibrary> compiledLibraries;
    private final List<Library> libraries;

    public CqlEngineCloseable(CqlEngine context, List<VersionedIdentifier> libraryVersionedIdentifiers) {
        this.context = context;
        this.compiledLibraries = getCompiledLibraries(libraryVersionedIdentifiers, context);
        this.libraries =
            compiledLibraries.stream().map(CompiledLibrary::getLibrary).toList();

        // Add back the libraries to the stack, since we popped them off during CQL
        context.getState().init(libraries);
    }

    public <T> T doStuff(Function<List<CompiledLibrary>, T> function) {
        return function.apply(this.compiledLibraries);
    }

    public List<CompiledLibrary> getCompiledLibraries() {
        return this.compiledLibraries;
    }

    @Override
    public void close() {
        popAllLibrariesFromCqlEngine(context, libraries);
    }

    public void setArgParameters(@Nullable Map<String, Object> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) {
            return;
        }

        for (CompiledLibrary lib : compiledLibraries) {
            context.getState().setParameters(lib.getLibrary(), paramMap);

            if (lib.getLibrary().getIncludes() != null) {
                lib.getLibrary()
                    .getIncludes()
                    .getDef()
                    .forEach(includeDef -> paramMap.forEach((paramKey, paramValue) -> context.getState()
                        .setParameter(includeDef.getLocalIdentifier(), paramKey, paramValue)));
            }
        }
    }

    private List<CompiledLibrary> getCompiledLibraries(List<VersionedIdentifier> ids, CqlEngine context) {
        try {
            var resolvedLibraryResults =
                context.getEnvironment().getLibraryManager().resolveLibraries(ids);

            var allErrors = resolvedLibraryResults.allErrors();
            if (resolvedLibraryResults.hasErrors() || ids.size() > allErrors.size()) {
                return resolvedLibraryResults.allCompiledLibraries();
            }

            if (ids.size() == 1) {
                final List<CqlCompilerException> cqlCompilerExceptions =
                    resolvedLibraryResults.getErrorsFor(ids.get(0));

                if (cqlCompilerExceptions.size() == 1) {
                    throw new IllegalStateException(
                        "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                            .formatted(ids.get(0).getId()),
                        cqlCompilerExceptions.get(0));
                } else {
                    throw new IllegalStateException(
                        "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                            .formatted(
                                ids.get(0).getId(),
                                cqlCompilerExceptions.stream()
                                    .map(CqlCompilerException::getMessage)
                                    .reduce((s1, s2) -> s1 + "; " + s2)
                                    .orElse("No error messages found.")));
                }
            }

            throw new IllegalStateException(
                "Unable to load CQL/ELM for libraries: %s Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                    .formatted(ids, allErrors));

        } catch (CqlIncludeException exception) {
            throw new IllegalStateException(
                "Unable to load CQL/ELM for libraries: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                    .formatted(
                        ids.stream().map(VersionedIdentifier::getId).toList()),
                exception);
        }
    }


    private void popAllLibrariesFromCqlEngine(CqlEngine context, List<Library> libraries) {
        libraries.forEach(lib -> context.getState().exitLibrary(true));
    }
}
