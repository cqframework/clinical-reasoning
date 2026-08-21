package org.opencds.cqf.fhir.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.ParametersUtil
import java.time.ZonedDateTime
import kotlin.IllegalArgumentException
import org.apache.commons.lang3.StringUtils
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.opencds.cqf.cql.engine.execution.CqlEngine
import org.opencds.cqf.cql.engine.execution.EvaluationResult
import org.opencds.cqf.cql.engine.execution.EvaluationResults
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition
import org.opencds.cqf.fhir.utility.Constants.DATA_ABSENT_REASON
import org.opencds.cqf.fhir.utility.CqfExpression
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LibraryEngine(val repository: IRepository, val settings: EvaluationSettings) {

    protected val fhirContext: FhirContext = repository.fhirContext()
    protected val adapterFactory: IAdapterFactory = IAdapterFactory.forFhirContext(fhirContext)
    protected val modelResolver: FhirModelResolver<*, *, *, *, *, *, *, *> =
        FhirModelResolverCache.resolverForVersion(fhirContext.version.version)

    private fun buildContextParameter(patientId: String?): Pair<String, String?>? {
        if (patientId != null) {
            return "Patient" to patientId.removePrefix("Patient/")
        }

        return null
    }

    fun evaluate(
        url: String,
        patientId: String?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        additionalData: IBaseBundle?,
        zonedDateTime: ZonedDateTime?,
        expressions: MutableSet<String>?,
    ): IBaseParameters {
        return this.evaluate(
            VersionedIdentifiers.forUrl(url),
            patientId,
            parameters,
            rawParameters,
            additionalData,
            zonedDateTime,
            expressions,
        )
    }

    fun evaluate(
        id: VersionedIdentifier,
        patientId: String?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        additionalData: IBaseBundle?,
        zonedDateTime: ZonedDateTime?,
        expressions: MutableSet<String>?,
    ): IBaseParameters {
        val cqlFhirParametersConverter =
            Engines.getCqlFhirParametersConverter(repository.fhirContext())
        val result =
            getEvaluationResult(
                id,
                patientId,
                parameters,
                rawParameters,
                additionalData,
                expressions,
                cqlFhirParametersConverter,
                zonedDateTime,
                null,
            )

        return cqlFhirParametersConverter.toFhirParameters(result)
    }

    protected fun getModelName(base: Any): String? {
        if (base is MutableList<*>) {
            // A Tuple requires each property to have a type.  If there is no value default to a
            // FHIR string.
            return if (base.isEmpty()) "FHIR.string" else getModelName(base[0]!!)
        }
        var fhirType = (base as IBase).fhirType()
        if (fhirType == "Tuple") {
            val properties = ArrayList<String?>()
            val tuple = adapterFactory.createTuple(base)
            tuple.getProperties().forEach { (propertyName: String?, value: Any?) ->
                properties.add("$propertyName ${getModelName(value!!)}")
            }
            return "Tuple { ${properties.joinToString(", ")} }"
        }
        if (fhirType.contains(".")) {
            val split = fhirType.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            fhirType = split.joinToString(".") { str -> StringUtils.capitalize(str) }
        }
        return "FHIR.$fhirType"
    }

    fun evaluateExpression(
        expression: String?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        patientId: String?,
        referencedLibraries: MutableMap<String?, String?>?,
        bundle: IBaseBundle?,
        contextParameter: IBase?,
        resourceParameter: IBase?,
    ): IBaseParameters {
        val libraryConstructor = LibraryConstructor(fhirContext)
        val cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext)
        val cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(parameters)
        val evaluationParameters = cqlFhirParametersConverter.toCqlParameters(parameters)
        if (contextParameter != null) {
            val contextType = getModelName(contextParameter)
            cqlParameters.add(CqlParameterDefinition("%context", contextType, false))
            evaluationParameters["%context"] = modelResolver.toCqlValue(contextParameter, false)

            val resourceType =
                if (resourceParameter == null) contextType else getModelName(resourceParameter)
            cqlParameters.add(CqlParameterDefinition("%resource", resourceType, false))
            evaluationParameters["%resource"] =
                modelResolver.toCqlValue(resourceParameter ?: contextParameter, false)
        }
        if (rawParameters != null) {
            rawParameters.forEach { (k: String?, v: Any?) ->
                cqlParameters.add(CqlParameterDefinition(k, getModelName(v!!), v is MutableList<*>))
            }
            evaluationParameters.putAll(cqlFhirParametersConverter.toCqlParameters(rawParameters))
        }
        val libraryName = "expression"
        val libraryVersion = "1.0.0"
        val cql =
            libraryConstructor.constructCqlLibrary(
                libraryName,
                libraryVersion,
                expression,
                referencedLibraries,
                cqlParameters,
            )

        val requestSettings = EvaluationSettings(settings)
        requestSettings.librarySourceProviders.add(StringLibrarySourceProvider(listOf(cql)))
        val engine = Engines.forRepository(repository, requestSettings, bundle)

        val id = VersionedIdentifier().withId(libraryName).withVersion(libraryVersion)

        val result =
            engine
                .evaluate {
                    this.contextParameter = buildContextParameter(patientId)
                    this.parameters = evaluationParameters
                    library(id) { expressions("return") }
                }
                .onlyResultOrThrow

        return cqlFhirParametersConverter.toFhirParameters(result)
    }

    fun getExpressionResult(
        subjectId: String?,
        expression: String,
        language: String?,
        libraryToBeEvaluated: String?,
        referencedLibraries: MutableMap<String?, String?>?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        bundle: IBaseBundle?,
        contextParameter: IBase?,
        resourceParameter: IBase?,
    ): MutableList<IBase?>? {
        validateExpression(language, expression)
        var results: MutableList<IBase?>?
        val parametersResult: IBaseParameters
        if (libraryToBeEvaluated == null) {
            parametersResult =
                this.evaluateExpression(
                    expression,
                    parameters,
                    rawParameters,
                    subjectId,
                    referencedLibraries,
                    bundle,
                    contextParameter,
                    resourceParameter,
                )
            // The expression is assumed to be the parameter component name
            // The expression evaluator creates a library with a single expression defined as
            // "return"
            results =
                resolveParameterValues(
                    ParametersUtil.getNamedParameters(fhirContext, parametersResult, "return")
                )
        } else {
            validateLibrary(libraryToBeEvaluated)
            parametersResult =
                this.evaluate(
                    libraryToBeEvaluated,
                    subjectId,
                    parameters,
                    rawParameters,
                    bundle,
                    null,
                    mutableSetOf(expression),
                )
            results =
                resolveParameterValues(
                    ParametersUtil.getNamedParameters(fhirContext, parametersResult, expression)
                )
        }

        return results
    }

    fun validateExpression(language: String?, expression: String?) {
        if (language == null) {
            logger.error("Missing language type for the Expression")
            throw IllegalArgumentException("Missing language type for the Expression")
        } else if (expression == null) {
            logger.error("Missing expression for the Expression")
            throw IllegalArgumentException("Missing expression for the Expression")
        }
    }

    fun validateLibrary(libraryUrl: String?) {
        if (libraryUrl == null) {
            logger.error("Missing library for the Expression")
            throw IllegalArgumentException("Missing library for the Expression")
        }
    }

    fun resolveParameterValues(values: List<IBase?>?): MutableList<IBase?>? {
        if (values.isNullOrEmpty()) {
            return null
        }

        return values
            .map { parametersParameterComponent ->
                adapterFactory.createParametersParameter(parametersParameterComponent)
            }
            .mapNotNull { param ->
                when {
                    param!!.hasValue() ->
                        if (
                            (param.getValue() as IBaseHasExtensions).extension.any {
                                DATA_ABSENT_REASON == it.url
                            }
                        ) {
                            null
                        } else {
                            param.getValue()
                        }
                    param.hasResource() -> param.getResource()
                    param.hasPart() -> param.newTupleWithParts()
                    else -> null
                }
            }
            .toMutableList()
    }

    fun resolveExpression(
        patientId: String?,
        expression: CqfExpression,
        params: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        bundle: IBaseBundle?,
        contextParameter: IBase?,
        resourceParameter: IBase?,
    ): List<IBase?>? {
        var result =
            getExpressionResult(
                patientId,
                expression.expression,
                expression.language,
                expression.libraryUrl,
                expression.referencedLibraries,
                params,
                rawParameters,
                bundle,
                contextParameter,
                resourceParameter,
            )
        if (result == null && expression.altExpression != null) {
            result =
                getExpressionResult(
                    patientId,
                    expression.altExpression,
                    expression.altLanguage,
                    expression.altLibraryUrl,
                    expression.referencedLibraries,
                    params,
                    rawParameters,
                    bundle,
                    contextParameter,
                    resourceParameter,
                )
        }

        return result
    }

    fun getEvaluationResult(
        ids: List<VersionedIdentifier>,
        patientId: String?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        additionalData: IBaseBundle?,
        expressions: MutableSet<String>?,
        cqlFhirParametersConverter: CqlFhirParametersConverter?,
        zonedDateTime: ZonedDateTime?,
        engine: CqlEngine?,
    ): EvaluationResults {
        val cqlFhirParametersConverterToUse =
            cqlFhirParametersConverter
                ?: Engines.getCqlFhirParametersConverter(repository.fhirContext())

        // engine context built externally of LibraryEngine?
        val engineToUse = engine ?: Engines.forRepository(repository, settings, additionalData)

        val evaluationParameters = cqlFhirParametersConverterToUse.toCqlParameters(parameters)
        if (rawParameters != null) {
            evaluationParameters.putAll(
                cqlFhirParametersConverterToUse.toCqlParameters(rawParameters)
            )
        }

        return engineToUse.evaluate {
            this.parameters = evaluationParameters
            contextParameter = buildContextParameter(patientId)
            evaluationDateTime = zonedDateTime
            ids.forEach { i ->
                if (!expressions.isNullOrEmpty()) {
                    library(i) { expressions(expressions) }
                } else {
                    library(i)
                }
            }
        }
    }

    fun getEvaluationResult(
        id: VersionedIdentifier,
        patientId: String?,
        parameters: IBaseParameters?,
        rawParameters: MutableMap<String?, Any?>?,
        additionalData: IBaseBundle?,
        expressions: MutableSet<String>?,
        cqlFhirParametersConverter: CqlFhirParametersConverter?,
        zonedDateTime: ZonedDateTime?,
        engine: CqlEngine?,
    ): EvaluationResult {
        val evaluationResultsForMultiLib =
            getEvaluationResult(
                mutableListOf(id),
                patientId,
                parameters,
                rawParameters,
                additionalData,
                expressions,
                cqlFhirParametersConverter,
                zonedDateTime,
                engine,
            )

        return evaluationResultsForMultiLib.onlyResultOrThrow
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LibraryEngine::class.java)
    }
}
