package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface IBaseLibraryAdapter extends IBaseKnowledgeArtifactAdapter {

    boolean hasContent();
    List<? extends ICompositeType> getContent();
    <T extends ICompositeType> void setContent(List<T> attachments);
    <T extends ICompositeType> T addContent();
}
