package org.opencds.cqf.fhir.utility.npm;

import java.util.List;
import java.util.Optional;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

// LUKETODO:  javadoc
// LUKETODO:  top level
public class NpmPackageLoaderWithCache implements NpmPackageLoader {

    private final List<NpmResourceHolder> npmResourceHolders;
    private final NpmPackageLoader npmPackageLoader;

    public static NpmPackageLoaderWithCache of(NpmResourceHolder npmResourceHolder, NpmPackageLoader npmPackageLoader) {
        return new NpmPackageLoaderWithCache(List.of(npmResourceHolder), npmPackageLoader);
    }

    public static NpmPackageLoaderWithCache of(
            List<NpmResourceHolder> npmResourceHolders, NpmPackageLoader npmPackageLoader) {
        return new NpmPackageLoaderWithCache(npmResourceHolders, npmPackageLoader);
    }

    private NpmPackageLoaderWithCache(List<NpmResourceHolder> npmResourceHolders, NpmPackageLoader npmPackageLoader) {
        this.npmResourceHolders = npmResourceHolders;
        this.npmPackageLoader = npmPackageLoader;
    }

    @Override
    public NpmResourceHolder loadNpmResources(IPrimitiveType<String> measureUrl) {
        return npmResourceHolders.stream()
                .filter(npmResourceHolder -> isMeasureUrlMatch(npmResourceHolder, measureUrl))
                .findFirst()
                .orElseGet(() -> npmPackageLoader.loadNpmResources(measureUrl));
    }

    @Override
    public Optional<ILibraryAdapter> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {
        var optLibrary = npmResourceHolders.stream()
                .map(npmResourceHolder -> npmResourceHolder.findMatchingLibrary(versionedIdentifier))
                .flatMap(Optional::stream)
                .findFirst();

        if (optLibrary.isPresent()) {
            return optLibrary;
        }

        return findLibraryFromUnrelatedNpmPackage(versionedIdentifier);
    }

    @Override
    public Optional<ILibraryAdapter> findMatchingLibrary(ModelIdentifier modelIdentifier) {
        var optLibrary = npmResourceHolders.stream()
                .map(npmResourceHolder -> npmResourceHolder.findMatchingLibrary(modelIdentifier))
                .flatMap(Optional::stream)
                .findFirst();

        if (optLibrary.isPresent()) {
            return optLibrary;
        }

        return findLibraryFromUnrelatedNpmPackage(modelIdentifier);
    }

    @Override
    public List<NamespaceInfo> getAllNamespaceInfos() {
        return npmPackageLoader.getAllNamespaceInfos();
    }

    @Override
    public Optional<ILibraryAdapter> loadLibraryByUrl(String libraryUrl) {

        var optLibrary = npmResourceHolders.stream()
                .filter(npmResourceHolder -> isLibraryUrlMatch(npmResourceHolder, libraryUrl))
                .map(NpmResourceHolder::getOptMainLibrary)
                .flatMap(Optional::stream)
                .findFirst();

        if (optLibrary.isPresent()) {
            return optLibrary;
        }

        return npmPackageLoader.loadLibraryByUrl(libraryUrl);
    }

    private static boolean isMeasureUrlMatch(NpmResourceHolder npmResourceHolder, IPrimitiveType<String> measureUrl) {
        return npmResourceHolder
                .getMeasure()
                .map(measure -> measure.getUrl().equals(measureUrl.getValue()))
                .orElse(false);
    }

    private static boolean isLibraryUrlMatch(NpmResourceHolder npmResourceHolder, String libraryUrl) {
        return npmResourceHolder
                .getMeasure()
                .map(library -> library.getUrl().equals(libraryUrl))
                .orElse(false);
    }
}
