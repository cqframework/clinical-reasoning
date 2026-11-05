package org.opencds.cqf.fhir.utility.repository.ig

internal enum class ResourceCategory {
    DATA,
    TERMINOLOGY,
    CONTENT;

    companion object {
        private val TERMINOLOGY_RESOURCES = setOf("ValueSet", "CodeSystem")
        private val CONTENT_RESOURCES =
            setOf(
                "Library",
                "Questionnaire",
                "Measure",
                "PlanDefinition",
                "StructureDefinition",
                "ActivityDefinition",
            )

        fun forType(resourceType: String?): ResourceCategory {
            if (TERMINOLOGY_RESOURCES.contains(resourceType)) {
                return TERMINOLOGY
            } else if (CONTENT_RESOURCES.contains(resourceType)) {
                return CONTENT
            }

            return DATA
        }
    }
}
