package org.opencds.cqf.cql.evaluator.fhir.api;

import org.hl7.fhir.instance.model.api.ICompositeType;
/**
 * This interface exposes common functionality across all FHIR Attachment versions.
 */
public interface AttachmentAdapter {

    ICompositeType get();

    String getContentType();

    void setContentType(String contentType);

    byte[] getData();

    void setData(byte[] data);
}