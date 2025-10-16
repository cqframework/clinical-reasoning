package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;

/**
 * Helper class to initialize CQL libraries for CQL evaluation.
 */
public class LibraryInitHandler {

    // LUKETODO: extend library evaluated context so library initialization isn't having to be built for
    // both, and takes advantage of caching
    // LUKETODO:  are we reinitializing the cache here?  if so, should we try not to, somehow?
    public static void initLibraries(CqlEngine context, List<VersionedIdentifier> libraryIdentifiers) {
        var compiledLibraries = getCompiledLibraries(libraryIdentifiers, context);

        var libraries =
                compiledLibraries.stream().map(CompiledLibrary::getLibrary).toList();

        // Add back the libraries to the stack, since we popped them off during CQL
        context.getState().init(libraries);
    }

    private static List<CompiledLibrary> getCompiledLibraries(List<VersionedIdentifier> ids, CqlEngine context) {
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
}
