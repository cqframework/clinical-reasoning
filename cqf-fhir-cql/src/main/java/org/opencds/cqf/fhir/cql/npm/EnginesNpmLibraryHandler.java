package org.opencds.cqf.fhir.cql.npm;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;

public class EnginesNpmLibraryHandler {

    private EnginesNpmLibraryHandler() {
        // private constructor
    }

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager,
            ModelManager modelManager,
            R4NpmResourceInfoForCql r4NpmResourceInfoForCql,
            R4NpmPackageLoader r4NpmPackageLoader) {

        r4NpmPackageLoader.initNamespaceMappings(libraryManager);

        var loader = libraryManager.getLibrarySourceLoader();

        loader.registerProvider(new NpmLibraryProvider(r4NpmResourceInfoForCql, r4NpmPackageLoader));

        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider(r4NpmResourceInfoForCql, r4NpmPackageLoader));
    }
}
