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

    @JvmStatic
    fun getId(classInstance: ClassInstance): kotlin.String? {
        if (classInstance.type.namespaceURI == fhirModelNamespaceUri && classInstance.has("id")) {
            val resourceIdInstance = classInstance["id"] as ClassInstance?
            val resourceIdValue = resourceIdInstance?.get("value")
            if (resourceIdValue != null) {
                val type = classInstance.type.localPart
                return "$type/${plainStringValue(resourceIdValue)}"
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
    fun convertToFhirR4(value: Value?): IBase {
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
}
