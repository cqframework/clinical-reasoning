package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.function.Function;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmResourceInfoForCql;

/**
 * {@link ModelInfoProvider} to provide a ELM ModelInfo from an NPM package.
 */
public class NpmModelInfoProvider implements ModelInfoProvider {

    private static final String APPLICATION_XML = "application/xml";

    private final NpmPackageLoader npmPackageLoader;

    public NpmModelInfoProvider(NpmPackageLoader npmPackageLoader) {
        this.npmPackageLoader = npmPackageLoader;
    }

    @Override
    @Nullable
    public ModelInfo load(ModelIdentifier modelIdentifier) {

        return npmPackageLoader
                .findMatchingLibrary(modelIdentifier)
                .map(this::findElmXmlAttachment)
                .flatMap(Function.identity())
                .map(IAttachmentAdapter::getData)
                .map(ByteArrayInputStream::new)
                .map(inputStream -> JAXB.unmarshal(inputStream, ModelInfo.class))
                .orElse(null);
    }

    @Nonnull
    private Optional<IAttachmentAdapter> findElmXmlAttachment(ILibraryAdapter library) {
        final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(
                library.fhirContext().getVersion().getVersion());

        return library.getContent().stream()
                .map(adapterFactory::createAttachment)
                .filter(attachment -> APPLICATION_XML.equals(attachment.getContentType()))
                .findFirst();
    }
}
