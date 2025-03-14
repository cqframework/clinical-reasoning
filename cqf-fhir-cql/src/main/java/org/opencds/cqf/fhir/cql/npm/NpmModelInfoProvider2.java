package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
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

    private final NpmResourceHolder npmResourceHolder;
    private final NpmResourceHolderGetter npmResourceHolderGetter;

    public NpmModelInfoProvider2(NpmResourceHolderGetter npmResourceHolderGetter, NpmResourceHolder npmResourceHolder) {
        this.npmResourceHolder = npmResourceHolder;
        this.npmResourceHolderGetter = npmResourceHolderGetter;
    }

    @Override
    public ModelInfo load(ModelIdentifier modelIdentifier) {

        final Optional<Library> optLibrary = findLibrary(modelIdentifier);

        if (optLibrary.isEmpty()) {
            return null;
        }

        final Optional<Attachment> optCqlData = optLibrary.map(Library::getContent).stream()
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

    private Optional<Library> findLibrary(ModelIdentifier modelIdentifier) {

        final String url = toUrl(modelIdentifier);

        final Optional<Library> optMainLibrary = npmResourceHolder.getOptMainLibrary();

        if (doesLibraryMatch(modelIdentifier, optMainLibrary.orElse(null))) {
            return optMainLibrary;
        }

        return npmResourceHolderGetter.loadLibrary(url);
    }

    private static boolean doesLibraryMatch(ModelIdentifier modelIdentifier, @Nullable Library libraryCandidate) {
        return LibraryMatcher.doesLibraryMatch(modelIdentifier.getId(), libraryCandidate);
    }

    // LUKETODO:  this is not correct:
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
