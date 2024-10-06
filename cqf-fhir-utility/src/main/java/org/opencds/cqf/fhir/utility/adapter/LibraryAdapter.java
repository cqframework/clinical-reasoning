package org.opencds.cqf.fhir.utility.adapter;

import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface LibraryAdapter extends KnowledgeArtifactAdapter {

    List<String> LIBRARY_TYPES =
            Arrays.asList("logic-library", "model-definition", "asset-collection", "module-definition");

    boolean hasContent();

    <T extends ICompositeType> List<T> getContent();

    void setContent(List<? extends ICompositeType> attachments);

    ICompositeType addContent();

    ICompositeType getType();

    LibraryAdapter setType(String type);

    <T extends ICompositeType> List<T> getParameter();

    <T extends ICompositeType> List<T> getDataRequirement();

    LibraryAdapter addDataRequirement(ICompositeType dataRequirement);

    <T extends ICompositeType> LibraryAdapter setDataRequirement(List<T> dataRequirement);

    <T extends ICompositeType> List<T> getUseContext();

    void setExpansionParameters(
            List<String> systemVersionExpansionParameters, List<String> canonicalVersionExpansionParameters);
}
