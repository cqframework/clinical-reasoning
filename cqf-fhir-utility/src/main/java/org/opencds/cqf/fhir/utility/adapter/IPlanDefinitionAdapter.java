package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

/**
 * This interface exposes common functionality across all FHIR PlanDefinition versions.
 */
public interface IPlanDefinitionAdapter extends IKnowledgeArtifactAdapter {

    String getDescription();

    boolean hasLibrary();

    List<String> getLibrary();

    boolean hasGoal();

    List<IBaseBackboneElement> getGoal();

    boolean hasAction();

    List<IPlanDefinitionActionAdapter> getAction();
}
