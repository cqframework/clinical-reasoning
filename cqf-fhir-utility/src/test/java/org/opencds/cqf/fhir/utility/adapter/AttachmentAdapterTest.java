package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttachmentAdapterTest {

    @Test
    void r4AttachmentAdapter() {
        var attachment = new org.hl7.fhir.r4.model.Attachment();
        attachment.setContentType("application/json");
        attachment.setData(new byte[] {1, 2, 3});
        var adapter = IAdapterFactory.forFhirVersion(ca.uhn.fhir.context.FhirVersionEnum.R4)
                .createAttachment(attachment);
        assertEquals("application/json", adapter.getContentType());
        assertArrayEquals(new byte[] {1, 2, 3}, adapter.getData());
        adapter.setContentType("text/plain");
        assertEquals("text/plain", adapter.getContentType());
        adapter.setData(new byte[] {4, 5});
        assertArrayEquals(new byte[] {4, 5}, adapter.getData());
        assertNotNull(adapter.get());
    }

    @Test
    void r5AttachmentAdapter() {
        var attachment = new org.hl7.fhir.r5.model.Attachment();
        attachment.setContentType("text/xml");
        attachment.setData(new byte[] {10, 20});
        var adapter = IAdapterFactory.forFhirVersion(ca.uhn.fhir.context.FhirVersionEnum.R5)
                .createAttachment(attachment);
        assertEquals("text/xml", adapter.getContentType());
        assertArrayEquals(new byte[] {10, 20}, adapter.getData());
        adapter.setContentType("application/fhir+json");
        assertEquals("application/fhir+json", adapter.getContentType());
    }

    @Test
    void dstu3AttachmentAdapter() {
        var attachment = new org.hl7.fhir.dstu3.model.Attachment();
        attachment.setContentType("application/octet-stream");
        attachment.setData(new byte[] {99});
        var adapter = IAdapterFactory.forFhirVersion(ca.uhn.fhir.context.FhirVersionEnum.DSTU3)
                .createAttachment(attachment);
        assertEquals("application/octet-stream", adapter.getContentType());
        assertArrayEquals(new byte[] {99}, adapter.getData());
    }

    @Test
    void r4WrongTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> IAdapterFactory.forFhirVersion(
                        ca.uhn.fhir.context.FhirVersionEnum.R4)
                .createAttachment(new org.hl7.fhir.r4.model.Patient()));
    }
}
