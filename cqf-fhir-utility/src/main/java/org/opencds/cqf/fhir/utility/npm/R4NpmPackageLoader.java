package org.opencds.cqf.fhir.utility.npm;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.api.Repository;

/**
 * Interface for loading NPM resources including Measures, Libraries and NpmPackages as captured
 * within {@link R4NpmResourceInfoForCql}.
 * <p/>
 * This javadoc documents the entire NPM package feature in the clinical-reasoning project.  Please
 * read below:
 * <p/>
 * A downstream app from clinical-reasoning will be able to maintain Measures and Libraries loaded
 * from NPM packages.  Such Measures and Libraries will, for those clients implementing this
 * feature, no longer be maintained in {@link Repository} storage, unlike all other FHIR resources,
 * such as Patients.
 * <p/>
 * Downstream apps are responsible for loading and retrieving such packages from implementations
 * of the below interface.  Additionally, they must map all package IDs to package URLs via a
 * List of {@link NamespaceInfo}s.  This is done via the
 * {@link #initNamespaceMappings(LibraryManager)}, as due to how CQL libraries are loaded, it
 * won't work automatically.
 * <p/>
 * The {@link R4NpmResourceInfoForCql} class is used to capture the results of query the NPM
 * package with a given measure URL.  It's effectively a container for the Measure, its directly
 * associated Library, and its NPM package information.  In theory, there could be more than one
 * NPM package for a given Measure.  When CQL runs and calls a custom {@link LibrarySourceProvider},
 * it will first check to see if the directly associated Library matches the provided
 * {@link VersionedIdentifier}.  If not, it will query all NPM packages within the
 * R4NpmResourceInfoForCql to find the Library.  And if there is still no match, it will pass the
 * VersionedIdentifier, and build a URL from the system and ID before calling
 * {@link #loadLibraryByUrl(String)} to load that Library from another package, with the
 * VersionedIdentifier already resolved correctly with the help of the NamespaceInfos provided above.
 * The implementor is responsible for implementing loadLibraryByUrl to properly return the Library
 * from any packages maintained by the application.
 * <p/>
 * The above should also work with multiple layers of includes across packages.
 * <p/>
 * Example:  Package with ID X and URL <a href='http://packageX.org'>...</a> contains Measure ABC
 * is associated with Library 123, which contains CQL that includes Library 456 from NPM Package
 * with ID Y  and URL <a href='http://packageX.org'>...</a>, which contains both the Library and
 * its CQL.  When resolve the CQL include pointing to Package ID Y, the CQL engine must be able
 * to read the namespace info and resolve ID Y to URL <a href='http://packageY.org'>...</a>.  This
 * can only be accomplished via an explicit mapping.
 */
public interface R4NpmPackageLoader {

    R4NpmPackageLoader DEFAULT = new R4NpmPackageLoader() {
        @Override
        public R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl) {
            return R4NpmResourceInfoForCql.EMPTY;
        }

        @Override
        public List<NamespaceInfo> getAllNamespaceInfos() {
            return List.of();
        }

        @Override
        public Optional<Library> loadLibraryByUrl(String libraryUrl) {
            return Optional.empty();
        }
    };

    /**
     * @param measureUrl The Measure URL provided by the caller, corresponding to a Measure contained
     *                   withing one of the stored NPM packages.
     * @return The Measure corresponding to the URL.
     */
    R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl);

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

    default Optional<Library> findMatchingLibrary(
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql, VersionedIdentifier versionedIdentifier) {
        return r4NpmResourceInfoForCql
                .findMatchingLibrary(versionedIdentifier)
                .or(() -> findLibraryFromUnrelatedNpmPackage(versionedIdentifier));
    }

    default Optional<Library> findMatchingLibrary(
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql, ModelIdentifier modelIdentifier) {
        return r4NpmResourceInfoForCql
                .findMatchingLibrary(modelIdentifier)
                .or(() -> findLibraryFromUnrelatedNpmPackage(modelIdentifier));
    }

    default Optional<Library> findLibraryFromUnrelatedNpmPackage(VersionedIdentifier versionedIdentifier) {
        return loadLibraryByUrl(getUrl(versionedIdentifier));
    }

    default Optional<Library> findLibraryFromUnrelatedNpmPackage(ModelIdentifier modelIdentifier) {
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
    Optional<Library> loadLibraryByUrl(String libraryUrl);
}
