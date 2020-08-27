package org.opencds.cqf.cql.evaluator.fhir.api;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This interface exposes common functionality across all FHIR library versions.
 */
public interface LibraryAdapter {

    IIdType getId(IBaseResource library);

    void setId(IBaseResource library, IIdType id);

    String getName(IBaseResource library);

    void setName(IBaseResource library, String name);

    String getUrl(IBaseResource library);

    void setUrl(IBaseResource library, String url);

    String getVersion(IBaseResource library);

    void setVersion(IBaseResource library, String version);

    Boolean hasContent(IBaseResource library);

    List<ICompositeType> getContent(IBaseResource library);

    void setContent(IBaseResource library, List<ICompositeType> attachments);

    ICompositeType addContent(IBaseResource library);

    String getContentType(ICompositeType attachment);

    void setContentType(ICompositeType attachment, String contentType);

    byte[] getData(ICompositeType attachment);

    void setData(ICompositeType attachment, byte[] data);
}