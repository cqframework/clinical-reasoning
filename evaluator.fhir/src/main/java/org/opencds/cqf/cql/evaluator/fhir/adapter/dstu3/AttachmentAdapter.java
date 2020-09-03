package org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.instance.model.api.ICompositeType;

class AttachmentAdapter implements org.opencds.cqf.cql.evaluator.fhir.adapter.AttachmentAdapter {

    private Attachment attachment;

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

}