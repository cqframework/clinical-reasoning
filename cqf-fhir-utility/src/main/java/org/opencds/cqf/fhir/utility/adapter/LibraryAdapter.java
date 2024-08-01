package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface LibraryAdapter extends KnowledgeArtifactAdapter {

    boolean hasContent();

    List<? extends ICompositeType> getContent();

    void setContent(List<? extends ICompositeType> attachments);

    ICompositeType addContent();

    Optional<IBaseParameters> getExpansionParameters();

    void setExpansionParameters(
            List<String> systemVersionExpansionParameters, List<String> canonicalVersionExpansionParameters);
}
