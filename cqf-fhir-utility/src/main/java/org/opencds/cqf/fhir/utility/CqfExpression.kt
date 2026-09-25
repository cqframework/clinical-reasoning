package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseExtension
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.adapter.IAdapter

/**
 * This class is used to contain the various properties of a CqfExpression with an alternate so that
 * it can be used in version agnostic logic.
 */
class CqfExpression {
    var language: String? = null
        private set

    var expression: String? = null
        private set

    var referencedLibraries: MutableMap<String?, String?>? = null
        private set

    var libraryUrl: String? = null
        get() = resolveLibrary(language!!, field, expression!!)
        private set

    var altLanguage: String? = null
        private set

    var altExpression: String? = null
        private set

    var altLibraryUrl: String? = null
        get() =
            if (altExpression.isNullOrBlank()) null
            else resolveLibrary(altLanguage, field, altExpression!!)
        private set

    var name: String? = null
        private set

    constructor()

    @JvmOverloads
    constructor(
        language: String,
        expression: String,
        referencedLibraries: MutableMap<String?, String?>?,
        libraryUrl: String? = null,
        altLanguage: String? = null,
        altExpression: String? = null,
        altLibraryUrl: String? = null,
        name: String? = null,
    ) {
        this.language = language
        this.expression = expression
        this.referencedLibraries = referencedLibraries
        this.libraryUrl = libraryUrl
        this.altLanguage = altLanguage
        this.altExpression = altExpression
        this.altLibraryUrl = altLibraryUrl
        this.name = name
    }

    private fun resolveLibrary(lang: String?, url: String?, expr: String): String? {
        // If the expression is FHIRPath or a raw CQL expression a wrapper Library will be created
        if (listOf("text/cql.expression", "text/cql-expression", "text/fhirpath").contains(lang)) {
            return null
        }
        if (expr.contains(".") && lang == "text/cql" && url.isNullOrBlank()) {
            return null
        }
        // If the expression has a reference use it
        if (!url.isNullOrBlank()) {
            return url
        }
        // If the expression is an identifier and has no reference there should be a single
        // referenced Library
        if (!referencedLibraries.isNullOrEmpty()) {
            return referencedLibraries!!.values.firstOrNull()
        }
        throw IllegalArgumentException("No Library reference found for expression: $expr")
    }

    fun setName(name: String?): CqfExpression {
        this.name = name
        return this
    }

    fun setLanguage(language: String): CqfExpression {
        this.language = language
        return this
    }

    fun setExpression(expression: String): CqfExpression {
        this.expression = expression
        return this
    }

    fun setReferencedLibraries(referencedLibraries: MutableMap<String?, String?>?): CqfExpression {
        this.referencedLibraries = referencedLibraries
        return this
    }

    fun setLibraryUrl(libraryUrl: String?): CqfExpression {
        this.libraryUrl = libraryUrl
        return this
    }

    fun setAltLanguage(altLanguage: String): CqfExpression {
        this.altLanguage = altLanguage
        return this
    }

    fun setAltExpression(altExpression: String): CqfExpression {
        this.altExpression = altExpression
        return this
    }

    fun setAltLibraryUrl(altLibraryUrl: String?): CqfExpression {
        this.altLibraryUrl = altLibraryUrl
        return this
    }

    fun toExpressionType(fhirVersion: FhirVersionEnum): ICompositeType? {
        return when (fhirVersion) {
            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model
                    .Expression()
                    .setLanguage(language)
                    .setExpression(expression)
                    .setReference(libraryUrl)
                    .setName(name)

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model
                    .Expression()
                    .setLanguage(language)
                    .setExpression(expression)
                    .setReference(libraryUrl)
                    .setName(name)

            else -> null
        }
    }

    companion object {
        @JvmStatic
        fun of(
            expression: IAdapter<*>?,
            referencedLibraries: MutableMap<String?, String?>?,
        ): CqfExpression? {
            if (expression == null) {
                return null
            }
            val altExpressionExt =
                expression.getExtensionByUrl<IBaseExtension<*, *>?>(Constants.ALT_EXPRESSION_EXT)
            val altExpression =
                if (altExpressionExt == null) null
                else expression.getAdapterFactory().createBase(altExpressionExt.getValue())
            return CqfExpression(
                expression.resolvePathString("language"),
                expression.resolvePathString("expression"),
                referencedLibraries,
                expression.resolvePathString("reference"),
                if (altExpression != null) altExpression.resolvePathString("language") else null,
                if (altExpression != null) altExpression.resolvePathString("expression") else null,
                if (altExpression != null) altExpression.resolvePathString("reference") else null,
                expression.resolvePathString("name"),
            )
        }

        @JvmStatic
        fun of(
            extension: IBaseExtension<*, *>?,
            referencedLibraries: MutableMap<String?, String?>?,
        ): CqfExpression? {
            if (extension == null) {
                return null
            }
            val fhirPackagePath = "org.hl7.fhir."
            val className = extension.javaClass.canonicalName
            val modelSplit = className.split(fhirPackagePath.toRegex())
            require(modelSplit.size >= 2)
            var model = modelSplit[1]
            model = model.substring(0, model.indexOf(".")).uppercase()
            val version = FhirVersionEnum.forVersionString(model)
            return when (version) {
                FhirVersionEnum.DSTU3 ->
                    CqfExpression(
                        "text/cql-expression",
                        extension.value.toString(),
                        referencedLibraries,
                    )

                FhirVersionEnum.R4 ->
                    of(extension.value as org.hl7.fhir.r4.model.Expression?, referencedLibraries)
                FhirVersionEnum.R5 ->
                    of(extension.value as org.hl7.fhir.r5.model.Expression?, referencedLibraries)
                else -> null
            }
        }

        @JvmStatic
        fun of(
            expression: org.hl7.fhir.r4.model.Expression?,
            referencedLibraries: MutableMap<String?, String?>?,
        ): CqfExpression? {
            if (expression == null) {
                return null
            }
            val altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT)
            val altExpression =
                if (altExpressionExt == null) null
                else altExpressionExt.value as org.hl7.fhir.r4.model.Expression?
            return CqfExpression(
                expression.language,
                expression.expression,
                referencedLibraries,
                expression.reference,
                altExpression?.language,
                altExpression?.expression,
                if (altExpression != null && altExpression.hasReference()) altExpression.reference
                else null,
                expression.name,
            )
        }

        @JvmStatic
        fun of(
            expression: org.hl7.fhir.r5.model.Expression?,
            referencedLibraries: MutableMap<String?, String?>?,
        ): CqfExpression? {
            if (expression == null) {
                return null
            }
            val altExpressionExt = expression.getExtensionByUrl(Constants.ALT_EXPRESSION_EXT)
            val altExpression =
                if (altExpressionExt == null) null
                else altExpressionExt.value as org.hl7.fhir.r5.model.Expression?
            return CqfExpression(
                expression.language,
                expression.expression,
                referencedLibraries,
                expression.reference,
                altExpression?.language,
                altExpression?.expression,
                if (altExpression != null && altExpression.hasReference()) altExpression.reference
                else null,
                expression.name,
            )
        }
    }
}
