package org.opencds.cqf.fhir.utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KnowledgeArtifactUtil {

    /**
     * The set of all "knowledge" resource types.
     */
    public static final Set<String> RESOURCE_TYPES = new HashSet<>(Arrays.asList(
        "Library", "Measure", "PlanDefinition", "StructureDefinition", "ActivityDefinition", "Questionnaire"));

}
