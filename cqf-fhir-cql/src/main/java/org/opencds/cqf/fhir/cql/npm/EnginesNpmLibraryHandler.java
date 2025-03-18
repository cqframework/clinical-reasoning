package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnginesNpmLibraryHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnginesNpmLibraryHandler.class);

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager,
            ModelManager modelManager,
            R4NpmPackageLoader r4NpmPackageLoader,
            R4NpmResourceHolder r4NpmResourceHolder) {

        var loader = libraryManager.getLibrarySourceLoader();

        addNamespacesToNamespaceManager(r4NpmResourceHolder, libraryManager);

        loader.registerProvider(new NpmLibraryProvider(r4NpmPackageLoader, r4NpmResourceHolder));

        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider(r4NpmPackageLoader, r4NpmResourceHolder));
    }

    // LUKETODO:  why do we need to do this?  good question for JP
    private static void addNamespacesToNamespaceManager(
            R4NpmResourceHolder r4NpmResourceHolder, LibraryManager libraryManager) {
        r4NpmResourceHolder
                .getNamespaceInfos()
                .forEach(namespaceInfo -> libraryManager.getNamespaceManager().addNamespace(namespaceInfo));
    }
}
