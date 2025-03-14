package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.elm.r1.VersionedIdentifier;
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
    }

    // LUKETODO:  somehow pass the main Library and the NpmResourceHolderGetter, or just the NpmResourceHolderGetter?
    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager, ModelManager modelManager, NpmResourceHolder npmResourceHolder) {

        var loader = libraryManager.getLibrarySourceLoader();
        // LUKETODO:  hwo to handle this?
        // LUKETODO:  only the main Library at this point, no need for multiples
        var optMainLibrary = npmResourceHolder.getOptMainLibrary();

        // LUKETODO:  figure out how to properly derive the FHIR version later
        // LUKETODO:  what's this reader for?
        var reader = new org.cqframework.fhir.npm.LibraryLoader(FhirVersionEnum.R4.getFhirVersionString());

        final Library library = optMainLibrary.orElse(null);

        loader.registerProvider(new NpmLibraryProvider(library));

        //        modelManager.getModelInfoLoader().registerModelInfoProvider(new NpmModelInfoProvider(library));
    }

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager, ModelManager modelManager, NpmResourceHolderGetter npmResourceHolderGetter) {

        var loader = libraryManager.getLibrarySourceLoader();

        loader.registerProvider(new NpmLibraryProvider2(npmResourceHolderGetter));

        //        modelManager.getModelInfoLoader().registerModelInfoProvider(new
        // NpmModelInfoProvider2(npmResourceHolderGetter));
    }

    private static String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
    }
}
