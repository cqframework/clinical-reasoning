package org.opencds.cqf.cql.evaluator.fhir.r4;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Attachment;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirVersionEnum;


public class LibraryAdapter
        implements org.opencds.cqf.cql.evaluator.fhir.api.LibraryAdapter {

    protected Library castLibrary(IBaseResource library) {
        if (library == null) {
            throw new IllegalArgumentException("library can not be null");
        }

        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }

        if (!library.getStructureFhirVersionEnum().equals(FhirVersionEnum.R4)) {
            throw new IllegalArgumentException("library is incorrect fhir version for this adapter");
        }

        return (Library)library;
    }

    protected Attachment castAttachment(ICompositeType attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("attachment can not be null");
        }

        if (!attachment.fhirType().equals("Attachment")) {
            throw new IllegalArgumentException("resource passed as attachment argument is not an Attachment resource");
        }

        if (!(attachment instanceof Attachment)) {
            throw new IllegalArgumentException("attachment is incorrect fhir version for this adapter");
        }

        return (Attachment)attachment;
    }

    @Override
    public IIdType getId(IBaseResource library) {
        return castLibrary(library).getIdElement();
    }

    @Override
    public void setId(IBaseResource library, IIdType id) {
        castLibrary(library).setId(id);
    }

    @Override
    public String getName(IBaseResource library) {
        return castLibrary(library).getName();
    }

    @Override
    public void setName(IBaseResource library, String name) {
        castLibrary(library).setName(name);
    }

    @Override
    public String getUrl(IBaseResource library) {
        return castLibrary(library).getUrl();
    }

    @Override
    public void setUrl(IBaseResource library, String url) {
        castLibrary(library).setUrl(url);
    }

    @Override
    public String getVersion(IBaseResource library) {
        return castLibrary(library).getVersion();
    }

    @Override
    public void setVersion(IBaseResource library, String version) {
        castLibrary(library).setVersion(version);
    }

    @Override
    public Boolean hasContent(IBaseResource library) {
        return castLibrary(library).hasContent();
    }

    @Override
    public List<ICompositeType> getContent(IBaseResource library) {
        return castLibrary(library)
            .getContent().stream().map(x -> (ICompositeType)x).collect(Collectors.toList());
    }

    @Override
    public void setContent(IBaseResource library, List<ICompositeType> attachments) {
        List<Attachment> castAttachments = attachments.stream().map(x -> castAttachment(x)).collect(Collectors.toList());
        castLibrary(library).setContent(castAttachments);
    }

    @Override
    public ICompositeType addContent(IBaseResource library) {
        return castLibrary(library).addContent();
    }

    @Override
    public String getContentType(ICompositeType attachment) {
        return castAttachment(attachment).getContentType();
    }

    @Override
    public void setContentType(ICompositeType attachment, String contentType) {
        castAttachment(attachment).setContentType(contentType);
    }

    @Override
    public byte[] getData(ICompositeType attachment) {
        return castAttachment(attachment).getData();
    }

    @Override
    public void setData(ICompositeType attachment, byte[] data) {
       castAttachment(attachment).setData(data);
    }
}