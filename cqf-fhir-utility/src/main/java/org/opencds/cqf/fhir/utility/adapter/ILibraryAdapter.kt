package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.ParametersUtil
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants

/** This interface exposes common functionality across all FHIR Library versions. */
interface ILibraryAdapter : IKnowledgeArtifactAdapter {
    fun hasContent(): Boolean

    fun <T : ICompositeType> getContent(): MutableList<T?>?

    fun setContent(attachments: MutableList<out ICompositeType?>?)

    fun addContent(): ICompositeType?

    val type: ICompositeType?

    fun setType(type: String?): ILibraryAdapter

    fun <T : ICompositeType> getParameter(): MutableList<T?>?

    fun hasDataRequirement(): Boolean

    val dataRequirement: MutableList<IDataRequirementAdapter?>?

    fun addDataRequirement(dataRequirement: ICompositeType?): ILibraryAdapter

    fun <T : ICompositeType> setDataRequirement(dataRequirement: MutableList<T?>?): ILibraryAdapter

    fun setExpansionParameters(expansionParameters: IBaseParameters?)

    fun ensureExpansionParametersEntry(
        artifactAdapter: IKnowledgeArtifactAdapter,
        crmiVersion: String?,
    ) {
        val maybeExpansionParameters = this.expansionParameters
        if (maybeExpansionParameters.isPresent) {
            val expansionParameters = maybeExpansionParameters.get()
            val resourceType = artifactAdapter.get()!!.fhirType()
            val url = artifactAdapter.url
            val canonical = url + "|" + artifactAdapter.version
            val parameterName = this.getExpansionParameterName(resourceType, crmiVersion)
            if (!this.parameterExists(expansionParameters, parameterName, canonical)) {
                val canonicalToAdd =
                    buildCanonicalToAdd(
                        artifactAdapter,
                        crmiVersion,
                        resourceType,
                        canonical,
                        this.fhirContext()!!,
                    )
                ParametersUtil.addParameterToParameters(
                    this.fhirContext(),
                    expansionParameters,
                    parameterName,
                    canonicalToAdd,
                )
            }
        }
    }

    fun parameterExists(
        parameters: IBaseParameters?,
        parameterName: String?,
        canonical: String?,
    ): Boolean {
        if (parameters == null) {
            return false
        } else {
            val nameMatchedParameters =
                ParametersUtil.getNamedParameterValuesAsString(
                    this.fhirContext(),
                    parameters,
                    parameterName,
                )
            return nameMatchedParameters.any { p -> p == canonical }
        }
    }

    /**
     * Build a canonical-like primitive with extensions in a FHIR-version-agnostic way.
     *
     * @param artifactAdapter Adapter providing descriptor
     * @param crmiVersion CRMI version
     * @param resourceType Resource type string
     * @param canonical Canonical value
     * @param ctx FhirContext for the version in use
     * @return IPrimitiveType<String> with extensions added
     */
    private fun buildCanonicalToAdd(
        artifactAdapter: IKnowledgeArtifactAdapter,
        crmiVersion: String?,
        resourceType: String,
        canonical: String?,
        ctx: FhirContext,
    ): IPrimitiveType<String?> {
        @Suppress("UNCHECKED_CAST")
        val canonicalToAdd =
            ctx.getElementDefinition("canonical")!!.newInstance() as IPrimitiveType<String?>
        canonicalToAdd.setValueAsString(canonical)

        if (canonicalToAdd is IBaseHasExtensions) {
            // Helper to add to extension list

            val extensions = canonicalToAdd.extension as MutableList<IBaseExtension<*, *>?>

            // ResourceType extension
            if (shouldAddResourceTypeExtension(crmiVersion, resourceType)) {
                val resourceTypeExt =
                    ctx.getElementDefinition("Extension")!!.newInstance() as IBaseExtension<*, *>
                resourceTypeExt.url = Constants.CQF_RESOURCETYPE

                @Suppress("UNCHECKED_CAST")
                val codeValue =
                    ctx.getElementDefinition("code")!!.newInstance() as IPrimitiveType<String?>
                codeValue.valueAsString = resourceType

                resourceTypeExt.value = codeValue
                extensions.add(resourceTypeExt)
            }

            // Display extension
            val displayExt =
                ctx.getElementDefinition("Extension")!!.newInstance() as IBaseExtension<*, *>
            displayExt.url = Constants.DISPLAY_EXTENSION

            @Suppress("UNCHECKED_CAST")
            val displayValue =
                ctx.getElementDefinition("string")!!.newInstance() as IPrimitiveType<String?>
            displayValue.valueAsString = artifactAdapter.descriptor

            displayExt.value = displayValue
            extensions.add(displayExt)
        }

        return canonicalToAdd
    }

    fun getExpansionParameterName(resourceType: String?, crmiVersion: String?): String {
        require(!StringUtils.isBlank(resourceType)) { "Missing required parameter: 'resourceType'" }

        val isCrmiV1 = crmiVersion != null && crmiVersion == Constants.CRMI_VERSION_1

        return if (resourceType == "CodeSystem") {
            if (isCrmiV1) Constants.SYSTEM_VERSION else Constants.DEFAULT_SYSTEM_VERSION
        } else if (resourceType == "ValueSet") {
            if (isCrmiV1) Constants.CANONICAL_VERSION else Constants.DEFAULT_VALUESET_VERSION
        } else {
            if (isCrmiV1) Constants.CANONICAL_VERSION else Constants.DEFAULT_CANONICAL_VERSION
        }
    }

    // For CRMI Version 1, entries for all resource types other than CodeSystem should have the
    // resourceType extension. For CRMI Version 2, only entries for ValueSets should have the
    // resourceType extension.
    fun shouldAddResourceTypeExtension(crmiVersion: String?, resourceType: String): Boolean {
        val isV1AndQualifies =
            (crmiVersion == null || crmiVersion == Constants.CRMI_VERSION_1) &&
                resourceType != Constants.RESOURCETYPE_CODESYSTEM

        val isV2AndQualifies =
            (crmiVersion != null && crmiVersion != Constants.CRMI_VERSION_1) &&
                resourceType == Constants.RESOURCETYPE_VALUESET

        return isV1AndQualifies || isV2AndQualifies
    }

    fun addCqfMessagesExtension(messages: IBaseOperationOutcome?) {
        addContained(messages)
        val ext = addExtension<IBaseExtension<*, *>>()
        ext!!.url = CQF_MESSAGES_EXT_URL
        val ref =
            fhirContext()!!.getElementDefinition("Reference")!!.newInstance() as IBaseReference
        ext.value = ref.setReference("#messages")
    }

    companion object {
        const val CQF_MESSAGES_EXT_URL: String =
            "http://hl7.org/fhir/StructureDefinition/cqf-messages"
    }
}
