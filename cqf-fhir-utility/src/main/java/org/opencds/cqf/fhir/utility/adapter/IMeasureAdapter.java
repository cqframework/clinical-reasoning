package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
// LUKETODO:  add stuff here
public interface IMeasureAdapter extends IKnowledgeArtifactAdapter {
    List<String> getLibraryValues();
}
