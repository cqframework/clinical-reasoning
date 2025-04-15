package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR ActivityDefinition versions.
 */
public interface IActivityDefinitionAdapter extends IKnowledgeArtifactAdapter {

    String getDescription();

    boolean hasLibrary();

    List<String> getLibrary();
}
