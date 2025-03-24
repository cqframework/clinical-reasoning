package org.opencds.cqf.fhir.cql.npm;

import java.util.Map;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;

public class EnginesNpmLibraryHandler {

    //        "org.opencds.npm.cross.package.target",
    // LUKETODO:  figure out exactly how this is supposed to work
    private static final Map<String, String> NAMESPACE_MAPPINGS =
            Map.of("opencds.crosspackagetarget", "http://cross.package.target.npm.opencds.org");

    private EnginesNpmLibraryHandler() {
        // private constructor
    }

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager,
            ModelManager modelManager,
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql,
            R4NpmPackageLoader r4NpmPackageLoader) {

        addNamespacesToNamespaceManager(libraryManager);

        var loader = libraryManager.getLibrarySourceLoader();

        addNamespacesToNamespaceManager(r4NpmResourceInfoForCql, libraryManager);

        loader.registerProvider(new NpmLibraryProvider(r4NpmResourceInfoForCql, r4NpmPackageLoader));

        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider(r4NpmResourceInfoForCql, r4NpmPackageLoader));
    }

    private static void addNamespacesToNamespaceManager(
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql, LibraryManager libraryManager) {
        r4NpmResourceInfoForCql
                .getNamespaceInfos()
                .forEach(namespaceInfo -> libraryManager.getNamespaceManager().addNamespace(namespaceInfo));
    }

    // LUKETODO:  figure out some sort of injection mechanism
    // LUKETODO:  figure out some sort of lazy loading mechanism
    private static void addNamespacesToNamespaceManager(LibraryManager libraryManager) {
        NAMESPACE_MAPPINGS.forEach(
                (key, value) -> libraryManager.getNamespaceManager().addNamespace(key, value));
    }
}
