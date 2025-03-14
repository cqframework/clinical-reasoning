package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR PlanDefinition versions.
 */
public interface IPlanDefinitionAdapter extends IKnowledgeArtifactAdapter {

    String getDescription();

    boolean hasLibrary();

    List<String> getLibrary();

    boolean hasAction();

    List<IPlanDefinitionActionAdapter> getAction();
}
