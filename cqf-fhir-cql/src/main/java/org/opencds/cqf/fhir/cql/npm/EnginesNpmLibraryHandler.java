package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnginesNpmLibraryHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnginesNpmLibraryHandler.class);

    private static final String TEXT_CQL = "text/cql";

    public static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager, ModelManager modelManager, NpmResourceHolder npmResourceHolder) {

        var loader = libraryManager.getLibrarySourceLoader();
        // LUKETODO:  hwo to handle this?
        // LUKETODO:  only the main Library at this point, no need for multiples
        var optMainLibrary = npmResourceHolder.getOptMainLibrary();

        // LUKETODO:  figure out how to properly derive the FHIR version later
        var reader = new org.cqframework.fhir.npm.LibraryLoader(FhirVersionEnum.R4.getFhirVersionString());

        // LUKETODO:  if we have the exact match for the version, then pass it down

        //        byte[] getLibrarySource(VersionedIdentifier identifier) {
        //            var url = toUrl(identifier); // TODO
        //            var results = npmSearch.byUrl(url);
        //            var lib = latestVersion(results); // pass in "latest"
        //
        //            for (var a : lib.attachments) {
        //                if (a.contentType == "text/cql") {
        //                    return a.data;
        //                }
        //            }
        //
        //            return null;
        //        final String url = toUrl(versionedIdentifier);
        //        }

        loader.registerProvider(versionedIdentifier -> {
            if (optMainLibrary.isEmpty()) {
                return null;
            }

            final Library library = optMainLibrary.get();
            if (!doesLibraryMatch(versionedIdentifier, library)) {
                return null;
            }

            final List<Attachment> content = library.getContent();

            final Optional<Attachment> optCqlData = content.stream()
                    .filter(c -> c.getContentType().equals(TEXT_CQL))
                    .findFirst();

            if (optCqlData.isEmpty()) {
                return null;
            }

            final Attachment attachment = optCqlData.get();

            return new ByteArrayInputStream(attachment.getData());
        });

        modelManager.getModelInfoLoader().registerModelInfoProvider(modelIdentifier -> {
            if (optMainLibrary.isEmpty()) {
                return null;
            }

            final Library library = optMainLibrary.get();

            if (!doesLibraryMatch(modelIdentifier, library)) {
                return null;
            }

            final List<Attachment> content = library.getContent();

            final Optional<Attachment> optCqlData = content.stream()
                    .filter(c -> c.getContentType().equals("application/xml"))
                    .findFirst();

            if (optCqlData.isEmpty()) {
                return null;
            }

            final Attachment attachment = optCqlData.get();

            final InputStream inputStream = new ByteArrayInputStream(attachment.getData());

            return JAXB.unmarshal(inputStream, ModelInfo.class);
        });
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

    private static boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier, Library libraryCandidate) {
        // LUKETODO:  this doesn't work

        if (versionedIdentifier.getId().equals(libraryCandidate.getIdPart())) {
            final Optional<Attachment> optCqlData = libraryCandidate.getContent().stream()
                    .filter(content -> content.getContentType().equals(TEXT_CQL))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static boolean doesLibraryMatch(ModelIdentifier modelIdentifier, Library libraryCandidate) {
        if (modelIdentifier.getId().equals(libraryCandidate.getIdPart())) {
            final Optional<Attachment> optCqlData = libraryCandidate.getContent().stream()
                    .filter(content -> content.getContentType().equals(TEXT_CQL))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }
}
