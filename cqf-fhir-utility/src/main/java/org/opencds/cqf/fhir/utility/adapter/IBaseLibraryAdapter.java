package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface IBaseLibraryAdapter extends IBaseKnowledgeArtifactAdapter {

    boolean hasContent();
    List<? extends ICompositeType> getContent();
    <T extends ICompositeType> void setContent(List<T> attachments);
    <T extends ICompositeType> T addContent();
}
