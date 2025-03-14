package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.r4.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnginesNpmLibraryHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnginesNpmLibraryHandler.class);

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager,
            ModelManager modelManager,
            NpmResourceHolderGetter npmResourceHolderGetter,
            NpmResourceHolder npmResourceHolder) {

        var loader = libraryManager.getLibrarySourceLoader();
        // LUKETODO:  hwo to handle this?
        // LUKETODO:  only the main Library at this point, no need for multiples
        var optMainLibrary = npmResourceHolder.getOptMainLibrary();

        addNamespacesToNamespaceManager(npmResourceHolder, libraryManager);

        // LUKETODO:  figure out how to properly derive the FHIR version later
        // LUKETODO:  what's this reader for?
        var reader = new org.cqframework.fhir.npm.LibraryLoader(FhirVersionEnum.R4.getFhirVersionString());

        final Library library = optMainLibrary.orElse(null);

        loader.registerProvider(new NpmLibraryProvider2(npmResourceHolderGetter, npmResourceHolder));

        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider2(npmResourceHolderGetter, npmResourceHolder));
    }

    // LUKETODO:  why do we need to do this?  good question for JP
    private static void addNamespacesToNamespaceManager(
            NpmResourceHolder npmResourceHolder, LibraryManager libraryManager) {
        npmResourceHolder
                .getNamespaceInfos()
                .forEach(namespaceInfo -> libraryManager.getNamespaceManager().addNamespace(namespaceInfo));
    }
}
