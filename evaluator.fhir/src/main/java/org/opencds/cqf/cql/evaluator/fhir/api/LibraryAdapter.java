package org.opencds.cqf.cql.evaluator.fhir.api;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface LibraryAdapter extends Adapter<IBaseResource> {

    IBaseResource get();

    IIdType getId();

    void setId( IIdType id);

    String getName();

    void setName(String name);

    String getUrl();

    void setUrl(String url);

    String getVersion();

    void setVersion(String version);

    Boolean hasContent();

    List<ICompositeType> getContent();

    void setContent(List<ICompositeType> attachments);

    ICompositeType addContent();
}