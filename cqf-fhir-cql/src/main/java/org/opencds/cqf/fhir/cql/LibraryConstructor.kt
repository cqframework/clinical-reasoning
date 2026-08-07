package org.opencds.cqf.fhir.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.fhirpath.IFhirPath
import org.apache.commons.lang3.StringUtils
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition
import org.opencds.cqf.fhir.utility.FhirPathCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LibraryConstructor(protected var fhirContext: FhirContext) {
    protected var fhirPath: IFhirPath = FhirPathCache.cachedForContext(fhirContext)

    fun constructCqlLibrary(
        name: String?,
        version: String?,
        expression: String?,
        libraries: MutableMap<String?, String?>?,
        parameters: MutableList<CqlParameterDefinition>?,
    ): String {
        logger.debug("Constructing expression for local evaluation")
        return constructCqlLibrary(
            name,
            version,
            mutableSetOf("define \"return\":\n  $expression"),
            libraries,
            parameters,
        )
    }

    fun constructCqlLibrary(
        name: String?,
        version: String?,
        expressions: MutableSet<String?>,
        libraries: MutableMap<String?, String?>?,
        parameters: MutableList<CqlParameterDefinition>?,
    ): String {
        val sb = StringBuilder()

        constructHeader(sb, name, version)
        constructUsings(sb)
        constructIncludes(sb, libraries)
        constructParameters(sb, parameters)
        constructContext(sb, null)
        for (expression in expressions) {
            sb.append("$expression\n\n")
        }

        val cql = sb.toString()

        logger.debug(cql)
        return cql
    }

    private fun getFhirVersionString(fhirVersion: FhirVersionEnum): String? {
        // The version of the DSTU3 enum is 3.0.2 which the CQL Engine does not support.
        return if (fhirVersion == FhirVersionEnum.DSTU3) "3.0.1" else fhirVersion.fhirVersionString
    }

    private fun constructIncludes(sb: StringBuilder, libraries: MutableMap<String?, String?>?) {
        sb.append(
            "include FHIRHelpers version '${getFhirVersionString(fhirContext.version.version)}' called FHIRHelpers\n"
        )

        if (libraries != null) {
            for (library in libraries.entries) {
                val vi = VersionedIdentifiers.forUrl(library.value!!)
                sb.append("include \"${vi.id}\"")
                if (vi.version != null) {
                    sb.append(" version '${vi.version}'")
                }
                sb.append(" called \"${library.key}\"")
                sb.append("\n")
            }
        }
        sb.append("\n")
    }

    private fun constructParameters(
        sb: StringBuilder,
        parameters: MutableList<CqlParameterDefinition>?,
    ) {
        if (parameters.isNullOrEmpty()) {
            return
        }

        for (cpd in parameters) {
            sb.append("parameter \"")
                .append(cpd.name)
                .append("\" ")
                .append(this.getTypeDeclaration(cpd.type, cpd.isList))
                .append("\n")
        }
        sb.append("\n")
    }

    private fun getTypeDeclaration(type: String?, isList: Boolean?): String? {
        // TODO: Handle "FHIR" and "System" prefixes
        // Should probably mark system types in the CqlParameterDefinition?
        if (true == isList) {
            return "List<$type>"
        } else {
            return type
        }
    }

    private fun constructUsings(sb: StringBuilder) {
        sb.append("using FHIR version '${getFhirVersionString(fhirContext.version.version)}'\n\n")
    }

    private fun constructHeader(sb: StringBuilder, name: String?, version: String?) {
        sb.append("library $name version '$version'\n\n")
    }

    private fun constructContext(sb: StringBuilder, contextType: String?) {
        sb.append("context ${if (StringUtils.isBlank(contextType)) "Patient" else contextType}\n\n")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LibraryConstructor::class.java)
    }
}
