package org.opencds.cqf.fhir.cql.npm;

import jakarta.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;

// LUKETODO:  javadoc
public class NpmModelInfoProvider2 implements ModelInfoProvider {

    private final NpmResourceHolderGetter npmResourceHolderGetter;

    public NpmModelInfoProvider2(NpmResourceHolderGetter npmResourceHolderGetter) {
        this.npmResourceHolderGetter = npmResourceHolderGetter;
    }

    @Override
    public ModelInfo load(ModelIdentifier modelIdentifier) {
        final String url = toUrl(modelIdentifier);

        final Optional<Library> optLoadedLibrary = npmResourceHolderGetter.loadLibrary(url);

        final Optional<Attachment> optCqlData = optLoadedLibrary.map(Library::getContent)
            .stream()
            .flatMap(Collection::stream)
            .filter(attachment -> "application/xml".equals(attachment.getContentType()))
            .findFirst();

        if (optCqlData.isEmpty()) {
            return null;
        }

        final Attachment attachment = optCqlData.get();

        final InputStream inputStream = new ByteArrayInputStream(attachment.getData());

        return JAXB.unmarshal(inputStream, ModelInfo.class);
    }

    private static boolean doesLibraryMatch(ModelIdentifier modelIdentifier, Library libraryCandidate) {
        return LibraryMatcher.doesLibraryMatch(modelIdentifier.getId(), libraryCandidate);
    }

    private static String toUrl(ModelIdentifier modelIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        return "https://" + modelIdentifier.getSystem() + "/Library/" + modelIdentifier.getId();
    }
}
