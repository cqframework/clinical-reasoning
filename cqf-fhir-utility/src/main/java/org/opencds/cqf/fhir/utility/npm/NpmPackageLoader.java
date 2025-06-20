package org.opencds.cqf.fhir.utility.npm;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

/**
 * FHIR version agnostic Interface for loading NPM resources including Measures, Libraries and
 * NpmPackages as captured within {@link NpmResourceInfoForCql}.
 * <p/>
 * This javadoc documents the entire NPM package feature in the clinical-reasoning project.  Please
 * read below:
 * <p/>
 * A downstream app from clinical-reasoning will be able to maintain Measures and Libraries loaded
 * from NPM packages.  Such Measures and Libraries will, for those clients implementing this
 * feature, no longer be maintained in {@link IRepository} storage, unlike all other FHIR resources,
 * such as Patients.
 * <p/>
 * Downstream apps are responsible for loading and retrieving such packages from implementations
 * of the below interface.  Additionally, they must map all package IDs to package URLs via a
 * List of {@link NamespaceInfo}s.  This is done via the
 * {@link #initNamespaceMappings(LibraryManager)}, as due to how CQL libraries are loaded, it
 * won't work automatically.
 * <p/>
 * The {@link NpmResourceInfoForCql} class is used to capture the results of query the NPM
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
 * This workflow is meant to be triggered by a new Measure operation provider:
 * $evaluate-measure-by-url, which takes a canonical measure URL instead of a measure ID like
 * $evaluate-measure.
 * <p/>
 * Example:  Package with ID X and URL <a href='http://packageX.org'>...</a> contains Measure ABC
 * is associated with Library 123, which contains CQL that includes Library 456 from NPM Package
 * with ID Y  and URL <a href='http://packageX.org'>...</a>, which contains both the Library and
 * its CQL.  When resolve the CQL include pointing to Package ID Y, the CQL engine must be able
 * to read the namespace info and resolve ID Y to URL <a href='http://packageY.org'>...</a>.  This
 * can only be accomplished via an explicit mapping.
 * <p/>
 * Note that there is the real possibility of Measures corresponding to the same canonical URL
 * among multiple NPM packages.  As such, clients who unintentionally add Measures with the same
 * URL in at least two different packages may see the Measure they're not expecting during an
 * $evaluate-measure-by-url, and may file production issues accordingly.   This may be mitigated
 * by new APIs in IHapiPackageCacheManager.
 */
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
     * @param npmPackageLoader The NpmPackageLoader, if injected by the downstream app,
     *                           otherwise null.
     * @return Either the downstream app's NpmPackageLoaderor a no-op implementation.
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
