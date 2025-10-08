package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.cql.model.NamespaceManager;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FHIR version agnostic Interface for loading NPM resources including Measures, Libraries and
 * potentially other qualifying resources.
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
 * When CQL runs and calls a custom {@link LibrarySourceProvider}, it will query all NPM packages
 * accessible by the backing implementation.
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
 * Note that, depending on the implementation, there is the real possibility of Measures
 * corresponding to the same canonical URL among multiple NPM packages.  As such, clients who
 * unintentionally add Measures with the same URL in at least two different packages may see the
 * Measure they're not expecting during an $evaluate-measure-by-url, and may file production
 * issues accordingly.
 */
public interface NpmPackageLoader {
    Logger logger = LoggerFactory.getLogger(NpmPackageLoader.class);

    String LIBRARY_URL_TEMPLATE = "%s/Library/%s";
    String LIBRARY = "Library";
    String MEASURE = "Measure";

    // effectively a no-op implementation
    NpmPackageLoader R4_DEFAULT = new NpmPackageLoader() {

        @Override
        public Optional<IBaseResource> loadNpmResource(IPrimitiveType<String> canonicalUrl) {
            return Optional.empty();
        }

        @Override
        public NpmNamespaceManager getNamespaceManager() {
            return NpmNamespaceManager.DEFAULT;
        }

        @Override
        public FhirContext getFhirContext() {
            return FhirContext.forR4Cached();
        }
    };

    /**
     * Query the NPM package repo for the resource corresponding to the provided resource class and
     * canonical URL corresponding to the NPM package repo.
     *
     * @param resourceClass        The expected class of the resource to be returned, which will
     *                             be checked via instanceof before being cast and returned.
     * @param canonicalResourceUrl The resource URL provided by the caller, corresponding to a
     *                             resource contained within one of the stored NPM packages.
     * @return The resource (any {@link IBaseResource}) corresponding to the URL.
     */
    default <T extends IBaseResource> Optional<T> loadNpmResource(
            Class<T> resourceClass, IPrimitiveType<String> canonicalResourceUrl) {

        final Optional<? extends IBaseResource> optResource = loadNpmResource(canonicalResourceUrl);

        if (optResource.isEmpty()) {
            return Optional.empty();
        }

        final IBaseResource resource = optResource.get();

        if (!resourceClass.isInstance(resource)) {
            throw new IllegalArgumentException("Expected resource to be a %s, but was a %s"
                    .formatted(resourceClass.getSimpleName(), resource.fhirType()));
        }

        return Optional.of(resourceClass.cast(resource));
    }

    default IAdapterFactory getAdapterFactory() {
        return IAdapterFactory.forFhirVersion(getFhirContext().getVersion().getVersion());
    }

    /**
     * Obtain the resource corresponding to the provided type-enclosed String URL from the NPM
     * package repo.  Implementors must figure out how to retrieve the resource by querying one
     * or more NPM packages maintained by the application.
     *
     * @param canonicalResourceUrl The type-enclosed String canonical URL of the resource to load.
     * @return The resource corresponding to the URL, if it exists.
     */
    Optional<IBaseResource> loadNpmResource(IPrimitiveType<String> canonicalResourceUrl);

    /**
     * Implementors must commit to supporting a specific FHIR version, in order to ensure that
     * the correct version of a given resource is used for both the FHIR canonical URL and the
     * returned resource.
     *
     * @return The FhirContext corresponding to the FHIR version of the NPM packages maintained
     * by the application.
     */
    FhirContext getFhirContext();

    /**
     * It's up to implementors to maintain the NamespaceManager that maintains the NamespaceInfos.
     */
    NpmNamespaceManager getNamespaceManager();

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param npmPackageLoader The NpmPackageLoader, if injected by the downstream app,
     *                           otherwise null.
     * @return Either the downstream app's NpmPackageLoader a no-op implementation.
     */
    static NpmPackageLoader getDefaultIfEmpty(@Nullable NpmPackageLoader npmPackageLoader) {
        return Optional.ofNullable(npmPackageLoader).orElse(NpmPackageLoader.R4_DEFAULT);
    }

    /**
     * Ensure the passed Library gets initialized with the NPM namespace mappings belonging
     * to this instance of NpmPackageLoader.
     *
     * @param libraryManager from the CQL Engine being used for an evaluation
     */
    default void initNamespaceMappings(LibraryManager libraryManager) {
        final List<NamespaceInfo> allNamespaceInfos = getAllNamespaceInfos();
        final NamespaceManager namespaceManager = libraryManager.getNamespaceManager();

        for (NamespaceInfo namespaceInfo : allNamespaceInfos) {
            // if we do this more than one time it won't error out subsequent times
            namespaceManager.ensureNamespaceRegistered(namespaceInfo);
        }
    }

    /**
     * @return All NamespaceInfos to map package IDs to package URLs for all NPM Packages maintained
     * for clinical-reasoning NPM package to be used to resolve cross-package Library/CQL
     * dependencies.
     */
    default List<NamespaceInfo> getAllNamespaceInfos() {
        return getNamespaceManager().getAllNamespaceInfos();
    }

    default Optional<ILibraryAdapter> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {
        return findLibraryFromUnrelatedNpmPackage(versionedIdentifier);
    }

    default Optional<ILibraryAdapter> findMatchingLibrary(ModelIdentifier modelIdentifier) {
        return findLibraryFromUnrelatedNpmPackage(modelIdentifier);
    }

    default Optional<ILibraryAdapter> findLibraryFromUnrelatedNpmPackage(VersionedIdentifier versionedIdentifier) {
        return loadLibraryByUrl(getUrl(versionedIdentifier));
    }

    default Optional<ILibraryAdapter> findLibraryFromUnrelatedNpmPackage(ModelIdentifier modelIdentifier) {
        return loadLibraryByUrl(getUrl(modelIdentifier));
    }

    /**
     * @param libraryUrl The Library URL converted from a given
     *                   withing one of the stored NPM packages.
     * @return The Measure corresponding to the URL.
     */
    default Optional<ILibraryAdapter> loadLibraryByUrl(String libraryUrl) {

        return toLibraryAdapter(
                loadNpmResource(getLibraryClass(), toPrimitiveType(libraryUrl)).orElse(null));
    }

    default Optional<ILibraryAdapter> toLibraryAdapter(IBaseResource resource) {
        if (resource == null) {
            return Optional.empty();
        }
        switch (getFhirContext().getVersion().getVersion()) {
            case R4 -> {
                if (!(resource instanceof org.hl7.fhir.r4.model.Library r4Library)) {
                    throw new IllegalArgumentException(
                            "Expected resource to be a Library, but was a " + resource.fhirType());
                }
                return Optional.of(new org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter(r4Library));
            }
            case R5 -> {
                if (!(resource instanceof org.hl7.fhir.r5.model.Library r5Library)) {
                    throw new IllegalArgumentException(
                            "Expected resource to be a Library, but was a " + resource.fhirType());
                }
                return Optional.of(new org.opencds.cqf.fhir.utility.adapter.r5.LibraryAdapter(r5Library));
            }
            default -> throw new IllegalStateException(
                    "Unsupported FHIR version: " + getFhirContext().getVersion().getVersion());
        }
    }

    default IPrimitiveType<String> toPrimitiveType(String libraryUrl) {
        switch (getFhirContext().getVersion().getVersion()) {
            case R4 -> {
                return new org.hl7.fhir.r4.model.StringType(libraryUrl);
            }
            case R5 -> {
                return new org.hl7.fhir.r5.model.StringType(libraryUrl);
            }
            default -> throw new IllegalStateException(
                    "Unsupported FHIR version: " + getFhirContext().getVersion().getVersion());
        }
    }

    default Class<? extends IBaseResource> getLibraryClass() {
        switch (getFhirContext().getVersion().getVersion()) {
            case R4 -> {
                return org.hl7.fhir.r4.model.Library.class;
            }
            case R5 -> {
                return org.hl7.fhir.r5.model.Library.class;
            }
            default -> throw new IllegalStateException(
                    "Unsupported FHIR version: " + getFhirContext().getVersion().getVersion());
        }
    }

    default IResourceAdapter toResourceAdapter(IBaseResource resource) {
        if (resource == null) {
            return null;
        }
        if (FhirVersionEnum.R4 == getFhirContext().getVersion().getVersion()) {
            switch (resource.fhirType()) {
                case LIBRARY -> {
                    return new org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter(
                            (org.hl7.fhir.r4.model.Library) resource);
                }
                case MEASURE -> {
                    return new org.opencds.cqf.fhir.utility.adapter.r4.MeasureAdapter(
                            (org.hl7.fhir.r4.model.Measure) resource);
                }
                default -> throw new IllegalArgumentException(
                        "Expected resource to be a Library or Measure, but was a " + resource.fhirType());
            }
        }

        if (FhirVersionEnum.R5 == getFhirContext().getVersion().getVersion()) {
            switch (resource.fhirType()) {
                case LIBRARY -> {
                    return new org.opencds.cqf.fhir.utility.adapter.r5.LibraryAdapter(
                            (org.hl7.fhir.r5.model.Library) resource);
                }
                case MEASURE -> {
                    return new org.opencds.cqf.fhir.utility.adapter.r5.MeasureAdapter(
                            (org.hl7.fhir.r5.model.Measure) resource);
                }
                default -> throw new IllegalArgumentException(
                        "Expected resource to be a Library or Measure, but was a " + resource.fhirType());
            }
        }

        throw new InvalidRequestException("Unsupported FHIR version: %s"
                .formatted(getFhirContext().getVersion().getVersion().toString()));
    }

    private String getUrl(VersionedIdentifier versionedIdentifier) {
        // We need this case because the CQL engine will do the right thing and populate the system
        // in the cross-package target case
        return LIBRARY_URL_TEMPLATE.formatted(versionedIdentifier.getSystem(), versionedIdentifier.getId());
    }

    private String getUrl(ModelIdentifier modelIdentifier) {
        return LIBRARY_URL_TEMPLATE.formatted(modelIdentifier.getSystem(), modelIdentifier.getId());
    }
}
