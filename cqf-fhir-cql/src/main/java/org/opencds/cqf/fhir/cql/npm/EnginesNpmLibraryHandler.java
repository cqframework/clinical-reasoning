package org.opencds.cqf.fhir.cql.npm;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;

/**
 * Convenience class to extend {@link Engines} to handle NPM package specific logic.
 */
public class EnginesNpmLibraryHandler {

    private EnginesNpmLibraryHandler() {
        // private constructor
    }

    public static void registerNpmPackageLoader(
            LibraryManager libraryManager, ModelManager modelManager, NpmPackageLoader npmPackageLoader) {

        npmPackageLoader.initNamespaceMappings(libraryManager);

        var loader = libraryManager.getLibrarySourceLoader();

        loader.registerProvider(new NpmLibraryProvider(npmPackageLoader));

        modelManager.getModelInfoLoader().registerModelInfoProvider(new NpmModelInfoProvider(npmPackageLoader));
    }
}
