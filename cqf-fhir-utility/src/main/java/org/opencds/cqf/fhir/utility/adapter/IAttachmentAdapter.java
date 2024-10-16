package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This interface exposes common functionality across all FHIR Attachment versions.
 */
public interface IAttachmentAdapter extends IAdapter<ICompositeType> {

    ICompositeType get();

    String getContentType();

    void setContentType(String contentType);

    byte[] getData();

    void setData(byte[] data);
}
