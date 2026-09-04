package org.opencds.cqf.fhir.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.cql.engine.fhir.fhirModelNamespaceUri
import org.opencds.cqf.cql.engine.runtime.ClassInstance
import org.opencds.cqf.cql.engine.runtime.String
import org.opencds.cqf.cql.engine.runtime.Value
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter

/** This class provides utilities for handling ClassInstance objects from the CQL engine. */
object ClassInstanceHelper {
    var r4Converter: CqlFhirParametersConverter =
        Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached())
    val DSTU3_RESOURCE_TYPE_NAMES =
        org.hl7.fhir.dstu3.model.ResourceType.entries.map { obj -> obj.name }
    val R4_RESOURCE_TYPE_NAMES = org.hl7.fhir.r4.model.ResourceType.entries.map { obj -> obj.name }
    val R4B_RESOURCE_TYPE_NAMES =
        org.hl7.fhir.r4b.model.ResourceType.entries.map { obj -> obj.name }
    val R5_RESOURCE_TYPE_NAMES = org.hl7.fhir.r5.model.ResourceType.entries.map { obj -> obj.name }

    /** Every name that is a resource type in some modelled FHIR version. See [isFhirResource]. */
    private val ALL_RESOURCE_TYPE_NAMES: Set<kotlin.String> =
        (DSTU3_RESOURCE_TYPE_NAMES +
                R4_RESOURCE_TYPE_NAMES +
                R4B_RESOURCE_TYPE_NAMES +
                R5_RESOURCE_TYPE_NAMES)
            .toSet()

    @JvmStatic
    fun getId(classInstance: ClassInstance): kotlin.String? {
        val idPart = getIdPart(classInstance) ?: return null
        return "${classInstance.type.localPart}/$idPart"
    }

    /**
     * The bare `id.value` of a FHIR [ClassInstance], unqualified by resource type, or null when the
     * instance carries no id.
     *
     * This is the id that a HAPI resource converted from the same instance reports from
     * `getIdElement()`: the conversion copies `id.value` and nothing else, so the resource type
     * that [getId] prepends is not part of it. Callers that need to agree with a converted resource
     * want this; callers building a reference want [getId].
     */
    @JvmStatic
    fun getIdPart(classInstance: ClassInstance): kotlin.String? {
        if (classInstance.type.namespaceURI == fhirModelNamespaceUri && classInstance.has("id")) {
            val resourceIdInstance = classInstance["id"] as ClassInstance?
            val resourceIdValue = resourceIdInstance?.get("value")
            if (resourceIdValue != null) {
                return plainStringValue(resourceIdValue)
            }
        }
        return null
    }

    /**
     * Renders the plain string form of a CQL id value. The `id.value` element is a CQL [String],
     * whose `toString()` adds CQL quoting (e.g. `'enc-1'`); using that quoted form yields broken
     * references like `Encounter/'enc-1'`. Unwrap the underlying Java value instead.
     */
    private fun plainStringValue(value: Any?): kotlin.String {
        if (value is String) {
            return value.value
        }
        return value.toString()
    }

    @JvmStatic
    fun convertToFhirR4IfNeeded(value: Any?): Any? {
        return r4Converter.convertToFhirIfNeeded(value)
    }

    @JvmStatic
    fun convertToFhirR4(value: Value): IBase {
        return r4Converter.toFhirValue(value)
    }

    @JvmStatic
    fun getClassName(classInstance: ClassInstance): kotlin.String {
        // TODO: Need to fix version determination
        val version = "r4"
        val qName = classInstance.type
        val system =
            if (qName.namespaceURI == fhirModelNamespaceUri) "org.hl7.fhir" else qName.namespaceURI
        return "$system.$version.model.${qName.localPart}"
    }

    @JvmStatic
    fun isFhirResource(fhirVersion: FhirVersionEnum?, classInstance: ClassInstance): Boolean {
        if (classInstance.type.namespaceURI == fhirModelNamespaceUri) {
            val resourceTypes =
                when (fhirVersion) {
                    FhirVersionEnum.DSTU3 -> DSTU3_RESOURCE_TYPE_NAMES
                    FhirVersionEnum.R4 -> R4_RESOURCE_TYPE_NAMES
                    else -> mutableListOf()
                }
            return resourceTypes.contains(classInstance.type.localPart)
        }
        return false
    }

    /**
     * Whether the instance is a FHIR resource in any FHIR version modelled here, for callers that
     * hold no FHIR version of their own. A [ClassInstance] names its type but not the version that
     * type came from, and the versions disagree about which names are resources ("Sequence" is a
     * DSTU3 resource; R4 renamed it "MolecularSequence"), so this asks every version rather than
     * assuming one.
     *
     * Sound only for questions whose answer does not depend on the version — telling a resource
     * from a complex datatype so it can be keyed by [getIdPart], for instance. Converting to HAPI
     * FHIR is not such a question: use the version-qualified overload there.
     */
    @JvmStatic
    fun isFhirResource(classInstance: ClassInstance): Boolean {
        return classInstance.type.namespaceURI == fhirModelNamespaceUri &&
            ALL_RESOURCE_TYPE_NAMES.contains(classInstance.type.localPart)
    }
}
