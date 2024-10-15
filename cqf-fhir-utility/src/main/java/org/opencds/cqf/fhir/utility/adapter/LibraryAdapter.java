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

    List<? extends ICompositeType> getContent();

    void setContent(List<? extends ICompositeType> attachments);

    ICompositeType addContent();

    ICompositeType getType();

    LibraryAdapter setType(String type);

    List<? extends ICompositeType> getParameter();

    List<? extends ICompositeType> getDataRequirement();

    LibraryAdapter addDataRequirement(ICompositeType dataRequirement);

    LibraryAdapter setDataRequirement(List<ICompositeType> dataRequirement);

    List<? extends ICompositeType> getUseContext();

    void setExpansionParameters(
            List<String> systemVersionExpansionParameters, List<String> canonicalVersionExpansionParameters);
}
