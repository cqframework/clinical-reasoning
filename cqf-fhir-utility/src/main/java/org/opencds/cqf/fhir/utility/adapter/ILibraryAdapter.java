package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface ILibraryAdapter extends IKnowledgeArtifactAdapter {

    boolean hasContent();

    <T extends ICompositeType> List<T> getContent();

    void setContent(List<? extends ICompositeType> attachments);

    ICompositeType addContent();

    ICompositeType getType();

    ILibraryAdapter setType(String type);

    <T extends ICompositeType> List<T> getParameter();

    boolean hasDataRequirement();

    List<IDataRequirementAdapter> getDataRequirement();

    ILibraryAdapter addDataRequirement(ICompositeType dataRequirement);

    <T extends ICompositeType> ILibraryAdapter setDataRequirement(List<T> dataRequirement);

    void setExpansionParameters(IBaseParameters expansionParameters);

    void ensureExpansionParametersEntry(String resourceType, String canonical);
}
