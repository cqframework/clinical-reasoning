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
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;

/**
 * {@link ModelInfoProvider} to provide a ELM ModelInfo from an NPM package.
 */
public class NpmModelInfoProvider implements ModelInfoProvider {

    private static final String APPLICATION_XML = "application/xml";

    private final R4NpmResourceInfoForCql r4NpmResourceInfoForCql;

    public NpmModelInfoProvider(R4NpmResourceInfoForCql r4NpmResourceInfoForCql) {
        this.r4NpmResourceInfoForCql = r4NpmResourceInfoForCql;
    }

    @Override
    @Nullable
    public ModelInfo load(ModelIdentifier modelIdentifier) {

        final Optional<Library> optLibrary = r4NpmResourceInfoForCql.findMatchingLibrary(modelIdentifier);

        final Optional<Attachment> optCqlData = optLibrary.map(Library::getContent).stream()
                .flatMap(Collection::stream)
                .filter(attachment -> APPLICATION_XML.equals(attachment.getContentType()))
                .findFirst();

        if (optCqlData.isEmpty()) {
            return null;
        }

        final Attachment attachment = optCqlData.get();

        final InputStream inputStream = new ByteArrayInputStream(attachment.getData());

        return JAXB.unmarshal(inputStream, ModelInfo.class);
    }
}
