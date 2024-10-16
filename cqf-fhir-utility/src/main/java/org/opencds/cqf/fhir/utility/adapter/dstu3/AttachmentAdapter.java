package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

class AttachmentAdapter implements IAttachmentAdapter {

    private final Attachment attachment;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public AttachmentAdapter(ICompositeType attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("attachment can not be null");
        }

        if (!attachment.fhirType().equals("Attachment")) {
            throw new IllegalArgumentException("resource passed as attachment argument is not an Attachment resource");
        }

        if (!(attachment instanceof Attachment)) {
            throw new IllegalArgumentException("attachment is incorrect fhir version for this adapter");
        }

        this.attachment = (Attachment) attachment;
        fhirContext = FhirContext.forDstu3Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    protected Attachment getAttachment() {
        return this.attachment;
    }

    @Override
    public ICompositeType get() {
        return this.attachment;
    }

    @Override
    public String getContentType() {
        return this.getAttachment().getContentType();
    }

    @Override
    public void setContentType(String contentType) {
        this.getAttachment().setContentType(contentType);
    }

    @Override
    public byte[] getData() {
        return this.getAttachment().getData();
    }

    @Override
    public void setData(byte[] data) {
        this.getAttachment().setData(data);
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }
}
