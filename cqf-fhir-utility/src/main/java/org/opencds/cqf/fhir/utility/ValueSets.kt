package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.BaseRuntimeChildDefinition
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition
import ca.uhn.fhir.context.RuntimeResourceBlockDefinition
import ca.uhn.fhir.fhirpath.IFhirPath
import java.lang.reflect.InvocationTargetException
import java.util.Date
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.cql.engine.runtime.Code

object ValueSets {
    @JvmStatic
    fun getCompose(fhirContext: FhirContext, valueSet: IBaseResource?): IBase? {
        val composeChild = getComposeDefinition(fhirContext)
        val compose = composeChild.accessor.getValues(valueSet)

        if (compose.isEmpty()) {
            return null
        }

        require(compose.size <= 1) { "ValueSet has multiple compose definitions." }

        return compose[0]
    }

    @JvmStatic
    fun getIncludes(fhirContext: FhirContext, valueSet: IBaseResource?): MutableList<IBase?>? {
        val compose = getCompose(fhirContext, valueSet) ?: return null

        val includeChild = getIncludeDefinition(fhirContext)
        val includeComponents = includeChild.accessor.getValues(compose)

        if (includeComponents.isEmpty()) {
            return null
        }

        return includeComponents
    }

    @JvmStatic
    fun getExcludes(fhirContext: FhirContext, valueSet: IBaseResource?): MutableList<IBase?>? {
        val compose = getCompose(fhirContext, valueSet) ?: return null

        val excludeChild = getExcludeDefinition(fhirContext)
        val excludeComponents = excludeChild.accessor.getValues(compose)

        if (excludeComponents == null || excludeComponents.isEmpty()) {
            return null
        }

        return excludeComponents
    }

    @JvmStatic
    fun getIncludeConcepts(
        fhirContext: FhirContext,
        valueSet: IBaseResource?,
    ): MutableList<IBase?>? {
        val includes = getIncludes(fhirContext, valueSet) ?: return null

        val conceptChild = getIncludeConceptDefinition(fhirContext)

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        val concepts = mutableListOf<IBase?>()
        for (include in includes) {
            val currentConcepts = conceptChild.accessor.getValues(include)
            if (currentConcepts != null) {
                concepts.addAll(currentConcepts)
            }
        }

        return concepts
    }

    @JvmStatic
    fun getIncludeFilters(
        fhirContext: FhirContext,
        valueSet: IBaseResource?,
    ): MutableList<IBase?>? {
        val includes = getIncludes(fhirContext, valueSet) ?: return null

        val filterChild = getIncludeFilterDefinition(fhirContext)

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        val filters = mutableListOf<IBase?>()
        for (include in includes) {
            val currentFilters = filterChild.accessor.getValues(include)
            if (currentFilters != null) {
                filters.addAll(currentFilters)
            }
        }

        return filters
    }

    @JvmStatic
    fun getExcludeConcepts(
        fhirContext: FhirContext,
        valueSet: IBaseResource?,
    ): MutableList<IBase?>? {
        val excludes = getExcludes(fhirContext, valueSet) ?: return null

        val conceptChild = getExcludeConceptDefinition(fhirContext)

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        val concepts = mutableListOf<IBase?>()
        for (exclude in excludes) {
            val currentConcepts = conceptChild.accessor.getValues(exclude)
            if (currentConcepts != null) {
                concepts.addAll(currentConcepts)
            }
        }

        return concepts
    }

    @JvmStatic
    fun getExcludeFilters(
        fhirContext: FhirContext,
        valueSet: IBaseResource?,
    ): MutableList<IBase?>? {
        val excludes = getExcludes(fhirContext, valueSet) ?: return null

        val filterChild = getExcludeFilterDefinition(fhirContext)

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        val filters = mutableListOf<IBase?>()
        for (exclude in excludes) {
            val currentFilters = filterChild.accessor.getValues(exclude)
            if (currentFilters != null) {
                filters.addAll(currentFilters)
            }
        }

        return filters
    }

    @JvmStatic
    fun getExpansion(fhirContext: FhirContext, valueSet: IBaseResource?): IBase? {
        val expansionChild = getExpansionDefinition(fhirContext)
        val expansion = expansionChild.accessor.getValues(valueSet)

        if (expansion == null || expansion.isEmpty()) {
            return null
        }

        require(expansion.size <= 1) { "ValueSet has multiple expansion definitions." }

        return expansion[0]
    }

    @JvmStatic
    fun getContainsInExpansion(fhirContext: FhirContext, expansion: IBase?): MutableList<IBase?>? {
        if (expansion == null) {
            return null
        }

        val containsDefinition = getContainsDefinition(fhirContext)

        val contains = containsDefinition.accessor.getValues(expansion)

        if (contains == null || contains.isEmpty()) {
            return null
        }

        return contains
    }

    @JvmStatic
    fun getContains(fhirContext: FhirContext, valueSet: IBaseResource?): MutableList<IBase?>? {
        return getContainsInExpansion(fhirContext, getExpansion(fhirContext, valueSet))
    }

    @JvmStatic
    fun getCodesInCompose(fhirContext: FhirContext, valueSet: IBaseResource?): MutableList<Code>? {
        val includes = getIncludes(fhirContext, valueSet) ?: return null

        val conceptChild = getIncludeConceptDefinition(fhirContext)

        val versionAccessor = getIncludeVersionDefinition(fhirContext).accessor
        val systemAccessor = getIncludeSystemDefinition(fhirContext).accessor
        val codeAccessor = getIncludeConceptCodeDefinition(fhirContext).accessor
        val displayAccessor = getIncludeConceptDisplayDefinition(fhirContext).accessor

        val codes = mutableListOf<Code>()
        for (include in includes) {
            val version = getStringValueFromPrimitiveAccessor(include, versionAccessor)
            val system = getStringValueFromPrimitiveAccessor(include, systemAccessor)

            val concepts = conceptChild.accessor.getValues(include)

            for (c in concepts) {
                val code = getStringValueFromPrimitiveAccessor(c, codeAccessor)
                val display = getStringValueFromPrimitiveAccessor(c, displayAccessor)
                codes.add(
                    Code()
                        .withSystem(system)
                        .withCode(code)
                        .withDisplay(display)
                        .withVersion(version)
                )
            }
        }

        return codes
    }

    @JvmStatic
    fun getCodesInContains(
        fhirContext: FhirContext,
        contains: MutableList<IBase?>?,
    ): MutableList<Code>? {
        if (contains == null) {
            return null
        }

        val systemAccessor = getSystemDefinition(fhirContext).accessor
        val codeAccessor = getCodeDefinition(fhirContext).accessor
        val displayAccessor = getDisplayDefinition(fhirContext).accessor
        val versionAccessor = getVersionDefinition(fhirContext).accessor

        val codes = mutableListOf<Code>()
        for (c in contains) {
            val system = getStringValueFromPrimitiveAccessor(c, systemAccessor)
            val code = getStringValueFromPrimitiveAccessor(c, codeAccessor)
            val display = getStringValueFromPrimitiveAccessor(c, displayAccessor)
            val version = getStringValueFromPrimitiveAccessor(c, versionAccessor)

            codes.add(
                Code().withSystem(system).withCode(code).withDisplay(display).withVersion(version)
            )
        }

        return codes
    }

    @JvmStatic
    fun getCodesInExpansion(fhirContext: FhirContext, expansion: IBase?): MutableList<Code>? {
        return getCodesInContains(fhirContext, getContainsInExpansion(fhirContext, expansion))
    }

    @JvmStatic
    fun getCodesInExpansion(
        fhirContext: FhirContext,
        valueSet: IBaseResource?,
    ): MutableList<Code>? {
        return getCodesInContains(fhirContext, getContains(fhirContext, valueSet))
    }

    @JvmStatic
    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        IllegalArgumentException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class,
        SecurityException::class,
    )
    fun addCodeToExpansion(fhirContext: FhirContext, expansion: IBase?, code: Code) {
        val containsDef = getContainsDefinition(fhirContext)
        val systemDef = getSystemDefinition(fhirContext)
        val codeDef = getCodeDefinition(fhirContext)
        val displayDef = getDisplayDefinition(fhirContext)
        val versionDef = getVersionDefinition(fhirContext)
        @Suppress("UNCHECKED_CAST")
        val newCode =
            Resources.newBackboneElement(
                containsDef.getChildByName("contains").implementingClass
                    as Class<out IBaseBackboneElement>
            )
        systemDef.mutator.addValue(
            newCode,
            systemDef
                .getChildByName("system")
                .implementingClass
                .getConstructor(String::class.java)
                .newInstance(code.system),
        )
        codeDef.mutator.addValue(
            newCode,
            codeDef
                .getChildByName("code")
                .implementingClass
                .getConstructor(String::class.java)
                .newInstance(code.code),
        )
        displayDef.mutator.addValue(
            newCode,
            displayDef
                .getChildByName("display")
                .implementingClass
                .getConstructor(String::class.java)
                .newInstance(code.display),
        )
        versionDef.mutator.addValue(
            newCode,
            versionDef
                .getChildByName("version")
                .implementingClass
                .getConstructor(String::class.java)
                .newInstance(code.version),
        )
        containsDef.mutator.addValue(expansion, newCode)
    }

    @JvmStatic
    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        IllegalArgumentException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class,
        SecurityException::class,
    )
    fun setExpansionTimestamp(fhirContext: FhirContext, expansion: IBase?, timeStamp: Date?) {
        val expansionChild = getExpansionDefinition(fhirContext)
        val expansionBlockChild =
            expansionChild.getChildByName("expansion") as RuntimeResourceBlockDefinition
        val timeStampChild = expansionBlockChild.getChildByName("timestamp")
        timeStampChild.mutator.setValue(
            expansion,
            timeStampChild
                .getChildByName("timestamp")
                .implementingClass
                .getConstructor(Date::class.java)
                .newInstance(timeStamp),
        )
    }

    @JvmStatic
    fun addParameterToExpansion(
        fhirContext: FhirContext,
        expansion: IBase?,
        parameter: IBaseBackboneElement?,
    ) {
        getParameterDefinition(fhirContext).mutator.addValue(expansion, parameter)
    }

    fun getUrl(fhirContext: FhirContext, valueSet: IBaseResource?): String? {
        val urlDef = getUrlDefinition(fhirContext)
        return getStringValueFromPrimitiveAccessor(valueSet, urlDef.accessor)
    }

    fun getId(fhirContext: FhirContext, valueSet: IBaseResource?): String? {
        val idDef = getIdDefinition(fhirContext)
        return getStringValueFromPrimitiveAccessor(valueSet, idDef.accessor)
    }

    private fun getStringValueFromPrimitiveAccessor(value: IBase?, accessor: IAccessor?): String? {
        if (value == null || accessor == null) {
            return null
        }

        val values = accessor.getValues(value)
        if (values == null || values.isEmpty()) {
            return null
        }

        require(values.size <= 1) {
            "More than one value returned while attempting to access primitive value."
        }

        val baseValue = values[0]

        require(baseValue is IPrimitiveType<*>) {
            "Non-primitive value encountered while trying to access primitive value."
        }
        return baseValue.valueAsString
    }

    private fun getComposeDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val def = fhirContext.getResourceDefinition("ValueSet")
        return def.getChildByName("compose")
    }

    private fun getIncludeDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val composeChild = getComposeDefinition(fhirContext)
        return getIncludeDefinition(composeChild)
    }

    private fun getIncludeDefinition(
        composeChild: BaseRuntimeChildDefinition
    ): BaseRuntimeChildDefinition {
        val composeBlockChild =
            composeChild.getChildByName("compose") as RuntimeResourceBlockDefinition
        val includeChild = composeBlockChild.getChildByName("include")
        return includeChild
    }

    private fun getIncludeConceptDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val includeChild = getIncludeDefinition(fhirContext)
        val includeBlockChild =
            includeChild.getChildByName("include") as RuntimeResourceBlockDefinition
        return getConceptDefinition(includeBlockChild)
    }

    private fun getIncludeFilterDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val includeChild = getIncludeDefinition(fhirContext)
        val includeBlockChild =
            includeChild.getChildByName("include") as RuntimeResourceBlockDefinition
        return getFilterDefinition(includeBlockChild)
    }

    private fun getConceptDefinition(
        includeOrExcludeChild: RuntimeResourceBlockDefinition
    ): RuntimeChildResourceBlockDefinition {
        return includeOrExcludeChild.getChildByName("concept")
            as RuntimeChildResourceBlockDefinition
    }

    private fun getFilterDefinition(
        includeOrExcludeChild: RuntimeResourceBlockDefinition
    ): RuntimeChildResourceBlockDefinition {
        return includeOrExcludeChild.getChildByName("filter") as RuntimeChildResourceBlockDefinition
    }

    private fun getExcludeDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val composeChild = getComposeDefinition(fhirContext)
        return getExcludeDefinition(composeChild)
    }

    private fun getExcludeDefinition(
        composeChild: BaseRuntimeChildDefinition
    ): BaseRuntimeChildDefinition {
        val composeBlockChild =
            composeChild.getChildByName("compose") as RuntimeResourceBlockDefinition
        val excludeChild = composeBlockChild.getChildByName("exclude")
        return excludeChild
    }

    private fun getExcludeConceptDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val excludeChild = getExcludeDefinition(fhirContext)
        val excludeBlockChild =
            excludeChild.getChildByName("exclude") as RuntimeResourceBlockDefinition
        return getConceptDefinition(excludeBlockChild)
    }

    private fun getExcludeFilterDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val excludeChild = getExcludeDefinition(fhirContext)
        val excludeBlockChild =
            excludeChild.getChildByName("exclude") as RuntimeResourceBlockDefinition
        return getFilterDefinition(excludeBlockChild)
    }

    private fun getExpansionDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val def = fhirContext.getResourceDefinition("ValueSet")
        return def.getChildByName("expansion")
    }

    private fun getParameterDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val expansionChild = getExpansionDefinition(fhirContext)
        val expansionBlockChild =
            expansionChild.getChildByName("expansion") as RuntimeResourceBlockDefinition
        return getParameterDefinition(expansionBlockChild)
    }

    private fun getParameterDefinition(
        expansionChild: RuntimeResourceBlockDefinition
    ): BaseRuntimeChildDefinition {
        return expansionChild.getChildByName("parameter")
    }

    private fun getContainsDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val expansionChild = getExpansionDefinition(fhirContext)
        val expansionBlockChild =
            expansionChild.getChildByName("expansion") as RuntimeResourceBlockDefinition
        return getContainsDefinition(expansionBlockChild)
    }

    private fun getContainsDefinition(
        expansionChild: RuntimeResourceBlockDefinition
    ): BaseRuntimeChildDefinition {
        return expansionChild.getChildByName("contains")
    }

    private fun getSystemDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val containsDefinition = getContainsDefinition(fhirContext)
        val containsBlockDefinition =
            containsDefinition.getChildByName("contains") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("system")
    }

    private fun getVersionDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val containsDefinition = getContainsDefinition(fhirContext)
        val containsBlockDefinition =
            containsDefinition.getChildByName("contains") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("version")
    }

    private fun getCodeDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val containsDefinition = getContainsDefinition(fhirContext)
        val containsBlockDefinition =
            containsDefinition.getChildByName("contains") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("code")
    }

    private fun getDisplayDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val containsDefinition = getContainsDefinition(fhirContext)
        val containsBlockDefinition =
            containsDefinition.getChildByName("contains") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("display")
    }

    private fun getIncludeConceptCodeDefinition(
        fhirContext: FhirContext
    ): BaseRuntimeChildDefinition {
        val includeConceptDefinition = getIncludeConceptDefinition(fhirContext)
        val containsBlockDefinition =
            includeConceptDefinition.getChildByName("concept") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("code")
    }

    private fun getIncludeConceptDisplayDefinition(
        fhirContext: FhirContext
    ): BaseRuntimeChildDefinition {
        val includeConceptDefinition = getIncludeConceptDefinition(fhirContext)
        val containsBlockDefinition =
            includeConceptDefinition.getChildByName("concept") as RuntimeResourceBlockDefinition
        return containsBlockDefinition.getChildByName("display")
    }

    private fun getIncludeSystemDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val includeDefinition = getIncludeDefinition(fhirContext)
        val includeBlockDefinition =
            includeDefinition.getChildByName("include") as RuntimeResourceBlockDefinition
        return includeBlockDefinition.getChildByName("system")
    }

    private fun getIncludeVersionDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val includeDefinition = getIncludeDefinition(fhirContext)
        val includeBlockDefinition =
            includeDefinition.getChildByName("include") as RuntimeResourceBlockDefinition
        return includeBlockDefinition.getChildByName("version")
    }

    private fun getUrlDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val def = fhirContext.getResourceDefinition("ValueSet")
        return def.getChildByName("url")
    }

    private fun getIdDefinition(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        val def = fhirContext.getResourceDefinition("ValueSet")
        return def.getChildByName("id")
    }

    fun <T : IBase?> getExpansionParameters(
        expansion: IBase?,
        fhirPath: IFhirPath,
        filterExpression: String?,
    ): MutableList<IBase?>? {
        // String expression = "expansion.parameter";
        // if (filterExpression != null) { expression = expression + filterExpression; }
        val expression = "parameter$filterExpression"
        // String expression = (filterExpression == null) ? "expansion.parameter" :
        // "expansion.parameter" + filterExpression;
        return fhirPath.evaluate(expansion, expression, IBase::class.java)
    }
}
