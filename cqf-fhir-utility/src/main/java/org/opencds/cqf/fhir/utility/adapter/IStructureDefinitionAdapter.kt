package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IPrimitiveType

interface IStructureDefinitionAdapter : IKnowledgeArtifactAdapter {
    val type: String?
        get() = resolvePathString(get(), "type")

    val derivation: String?

    val baseDefinition: IPrimitiveType<String?>?

    fun hasSnapshot(): Boolean

    val snapshotElements: MutableList<IElementDefinitionAdapter?>?

    /** Returns all snapshot elements including the root element. */
    val allSnapshotElements: MutableList<IElementDefinitionAdapter?>?

    /** Returns all differential elements including the root element. */
    val allDifferentialElements: MutableList<IElementDefinitionAdapter?>?

    val differentialElements: MutableList<IElementDefinitionAdapter?>?

    fun getElement(elementId: String?): IElementDefinitionAdapter? {
        return this.differentialElements!!.firstOrNull { e -> e!!.id.equals(elementId) }
            ?: this.snapshotElements!!.firstOrNull { e -> e!!.id.equals(elementId) }
    }

    /**
     * Returns the first element found with a matching path. Differential elements will be returned
     * first. Elements with a slicing defined will be ignored.
     *
     * @param path The path of the element without the preceding resource type. e.g. value\[x]
     *   rather than Observation.value\[x]
     * @return IElementDefinitionAdapter
     */
    fun getElementByPath(path: String): IElementDefinitionAdapter? {
        return this.differentialElements!!
            .filter { e -> !e!!.hasSlicing() }
            .firstOrNull { e -> path == e!!.path!!.substring(e.path!!.indexOf(".") + 1) }
            ?: this.snapshotElements!!
                .filter { e -> !e!!.hasSlicing() }
                .firstOrNull { e -> path == e!!.path!!.substring(e.path!!.indexOf(".") + 1) }
    }

    fun getSliceElements(sliceName: String?): MutableList<IElementDefinitionAdapter?> {
        return this.differentialElements!!
            .filter { e -> matchesSliceElementId(e!!.id, sliceName) && e.sliceName.isNullOrBlank() }
            .toMutableList()
    }

    companion object {
        /**
         * Matches slice ids on name boundaries so `identifier:id` does not also match
         * `identifier:idType`.
         */
        @JvmStatic
        fun matchesSliceElementId(id: String?, sliceName: String?): Boolean {
            if (id.isNullOrBlank() || sliceName.isNullOrBlank()) {
                return false
            }
            return id.contains(".$sliceName.") ||
                id.endsWith(".$sliceName") ||
                id.contains(":$sliceName.") ||
                id.endsWith(":$sliceName")
        }
    }
}
