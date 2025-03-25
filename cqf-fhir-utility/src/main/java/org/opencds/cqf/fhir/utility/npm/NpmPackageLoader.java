package org.opencds.cqf.fhir.utility.npm;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public interface NpmPackageLoader {

    NpmPackageLoader DEFAULT = new NpmPackageLoader() {
        @Override
        public NpmResourceInfoForCql loadNpmResources(IPrimitiveType<String> measureUrl) {
            return NpmResourceInfoForCql.EMPTY;
        }

        @Override
        public List<NamespaceInfo> getAllNamespaceInfos() {
            return List.of();
        }

        @Override
        public Optional<ILibraryAdapter> loadLibraryByUrl(String libraryUrl) {
            return Optional.empty();
        }
    };

    /**
     * @param measureUrl The Measure URL provided by the caller, corresponding to a Measure contained
     *                   withing one of the stored NPM packages.
     * @return The Measure corresponding to the URL.
     */
    NpmResourceInfoForCql loadNpmResources(IPrimitiveType<String> measureUrl);

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param npmPackageLoader The NpmResourceHolderGetter, if injected by the downstream app,
     *                           otherwise null.
     * @return Either the downstream app's NpmResourceHolderGetter or a no-op implementation.
     */
    static NpmPackageLoader getDefaultIfEmpty(@Nullable NpmPackageLoader npmPackageLoader) {
        return Optional.ofNullable(npmPackageLoader).orElse(NpmPackageLoader.DEFAULT);
    }

    default void initNamespaceMappings(LibraryManager libraryManager) {
        getAllNamespaceInfos()
                .forEach(info -> libraryManager.getNamespaceManager().addNamespace(info));
    }

    /**
     * It's up to implementors to maintain the implementation that returns these NamespaceInfos.
     *
     * @return All NamespaceInfos to map package IDs to package URLs for all NPM Packages maintained
     * for clinical-reasoning NPM package to be used to resolve cross-package Library/CQL
     * dependencies.
     */
    List<NamespaceInfo> getAllNamespaceInfos();

    default Optional<ILibraryAdapter> findMatchingLibrary(
            NpmResourceInfoForCql npmResourceInfoForCql, VersionedIdentifier versionedIdentifier) {
        return npmResourceInfoForCql
                .findMatchingLibrary(versionedIdentifier)
                .or(() -> findLibraryFromUnrelatedNpmPackage(versionedIdentifier));
    }

    default Optional<ILibraryAdapter> findMatchingLibrary(
            NpmResourceInfoForCql npmResourceInfoForCql, ModelIdentifier modelIdentifier) {
        return npmResourceInfoForCql
                .findMatchingLibrary(modelIdentifier)
                .or(() -> findLibraryFromUnrelatedNpmPackage(modelIdentifier));
    }

    default Optional<ILibraryAdapter> findLibraryFromUnrelatedNpmPackage(VersionedIdentifier versionedIdentifier) {
        return loadLibraryByUrl(getUrl(versionedIdentifier));
    }

    default Optional<ILibraryAdapter> findLibraryFromUnrelatedNpmPackage(ModelIdentifier modelIdentifier) {
        return loadLibraryByUrl(getUrl(modelIdentifier));
    }

    private String getUrl(VersionedIdentifier versionedIdentifier) {
        return "%s/Library/%s".formatted(versionedIdentifier.getSystem(), versionedIdentifier.getId());
    }

    static String getUrl(ModelIdentifier modelIdentifier) {
        return "%s/Library/%s".formatted(modelIdentifier.getSystem(), modelIdentifier.getId());
    }

    /**
     * @param libraryUrl The Library URL converted from a given
     *                   withing one of the stored NPM packages.
     * @return The Measure corresponding to the URL.
     */
    Optional<ILibraryAdapter> loadLibraryByUrl(String libraryUrl);
}
