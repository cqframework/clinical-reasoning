package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

/**
 * This interface exposes common functionality across all FHIR GraphDefinition versions.
 */
public interface IGraphDefinitionAdapter extends IKnowledgeArtifactAdapter {

    // R4
    List<IBaseBackboneElement> getBackBoneElements();

    // R5
    List<IBaseBackboneElement> getNode();
}
