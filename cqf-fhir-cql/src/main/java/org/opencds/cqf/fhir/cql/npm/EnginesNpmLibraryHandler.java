package org.opencds.cqf.fhir.cql.npm;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;

public class EnginesNpmLibraryHandler {

    private EnginesNpmLibraryHandler() {
        // private constructor
    }

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager, ModelManager modelManager, R4NpmResourceInfoForCql r4NpmResourceInfoForCql) {

        var loader = libraryManager.getLibrarySourceLoader();

        addNamespacesToNamespaceManager(r4NpmResourceInfoForCql, libraryManager);

        loader.registerProvider(new NpmLibraryProvider(r4NpmResourceInfoForCql));

        modelManager.getModelInfoLoader().registerModelInfoProvider(new NpmModelInfoProvider(r4NpmResourceInfoForCql));
    }

    private static void addNamespacesToNamespaceManager(
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql, LibraryManager libraryManager) {
        r4NpmResourceInfoForCql
                .getNamespaceInfos()
                .forEach(namespaceInfo -> libraryManager.getNamespaceManager().addNamespace(namespaceInfo));
    }
}
