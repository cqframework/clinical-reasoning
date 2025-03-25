package org.opencds.cqf.fhir.cql.npm;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmResourceInfoForCql;

/**
 * Convenience class to extend {@link Engines} to handle NPM package specific logic.
 */
public class EnginesNpmLibraryHandler {

    private EnginesNpmLibraryHandler() {
        // private constructor
    }

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager,
            ModelManager modelManager,
            NpmResourceInfoForCql r4NpmResourceInfoForCql,
            NpmPackageLoader npmPackageLoader) {

        npmPackageLoader.initNamespaceMappings(libraryManager);

        var loader = libraryManager.getLibrarySourceLoader();

        loader.registerProvider(new NpmLibraryProvider(r4NpmResourceInfoForCql, npmPackageLoader));

        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider(r4NpmResourceInfoForCql, npmPackageLoader));
    }
}
