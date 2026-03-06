package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r5.model.Attachment;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;

class AttachmentAdapter extends BaseAdapter implements IAttachmentAdapter {

    private final Attachment attachment;

    public AttachmentAdapter(IBase attachment) {
        super(FhirVersionEnum.R5, attachment);
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
