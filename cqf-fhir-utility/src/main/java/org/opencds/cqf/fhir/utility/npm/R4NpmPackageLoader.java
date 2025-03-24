package org.opencds.cqf.fhir.utility.npm;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;

/**
 * Interface for loading NPM resources including Measures, Libraries and NpmPackages as captured
 * within {@link R4NpmResourceInfoForCql}.
 */
// LUKETODO:  document the entire feature in a really long javadoc
public interface R4NpmPackageLoader {

    // Default:  return NamespaceInfos.
    R4NpmPackageLoader DEFAULT = List::of;

    default R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl) {
        return R4NpmResourceInfoForCql.EMPTY;
    }

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param r4NpmPackageLoader The NpmResourceHolderGetter, if injected by the downstream app,
     *                           otherwise null.
     * @return Either the downstream app's NpmResourceHolderGetter or a no-op implementation.
     */
    static R4NpmPackageLoader getDefaultIfEmpty(@Nullable R4NpmPackageLoader r4NpmPackageLoader) {
        return Optional.ofNullable(r4NpmPackageLoader).orElse(R4NpmPackageLoader.DEFAULT);
    }

    default Optional<Library> loadLibraryByUrl(String url) {
        return Optional.empty();
    }

    // LUKETODO:  javadoc and what are the implications for implementing this
    default void initNamespaceMappings(LibraryManager libraryManager) {
        getAllNamespaceInfos()
                .forEach(info -> libraryManager.getNamespaceManager().addNamespace(info));
    }

    List<NamespaceInfo> getAllNamespaceInfos();
}
