package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.*
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.*

abstract class BaseAdapter(protected val fhirContext: FhirContext) {
    protected val fhirVersion = this.fhirContext.version.version
    val adapterFactory: IAdapterFactory = IAdapterFactory.forFhirContext(this.fhirContext)

    @JvmRecord data class ExtensionInfo(val url: String?, val index: Int)

    open fun fhirContext(): FhirContext? {
        return fhirContext
    }

    open fun resolvePath(target: Any?, path: String): Any? {
        var target = target
        val identifiers = path.split(".")
        for (identifier in identifiers) {
            // handling indexes: i.e. item[0].code
            if (identifier.contains("[")) {
                val index =
                    identifier
                        .substring(identifier.indexOf("[") + 1, identifier.indexOf("]"))
                        .toInt()
                target = resolveProperty(target, identifier.replace("\\[\\d+]".toRegex(), ""))
                if (target !is MutableList<*> || index >= target.size) {
                    return null
                }
                target = target[index]
            } else {
                target = resolveProperty(target, identifier)
            }
        }

        return target
    }

    protected fun resolveProperty(target: Any?, path: String): Any? {
        var target: Any? = target ?: return null

        if (target is IBaseEnumeration<*> && path == "value") {
            return target.valueAsString
        }

        if (target is IAnyResource && target.fhirType() == path) {
            return target
        }

        if (target is MutableList<*>) {
            var index = 0
            if (path.contains("[\\d]")) {
                try {
                    index = path.substring(path.indexOf("[")).replace("]", "").toInt()
                } catch (e: NumberFormatException) {
                    // Do nothing
                }
            }
            target = target[index]
        }

        val base = target as IBase
        val definition: BaseRuntimeElementCompositeDefinition<*>
        if (base is IPrimitiveType<*>) {
            return if (path == "value") (target as IPrimitiveType<*>).value else target
        } else {
            definition = resolveRuntimeDefinition<IBase>(base)
        }

        var child = definition.getChildByName(path)
        if (child == null) {
            child = resolveChoiceProperty(definition, path)
        }

        if (child == null) {
            return null
        }

        val values = child.accessor.getValues(base)

        if (values == null || values.isEmpty()) {
            return null
        }

        // If the instance is a primitive (including (or even especially an enumeration), and it has
        // no value, return
        // null
        if (child is RuntimeChildPrimitiveDatatypeDefinition) {
            val value = values[0]
            if (value is IPrimitiveType<*>) {
                if (!value.hasValue()) {
                    return null
                }
            }
        }

        if (
            child is RuntimeChildChoiceDefinition &&
                !child.elementName.equals(path, ignoreCase = true)
        ) {
            if (
                !values[0]!!
                    .javaClass
                    .simpleName
                    .equals(
                        child.getChildByName(path).implementingClass.simpleName,
                        ignoreCase = true,
                    )
            ) {
                return null
            }
        }

        return if (child.max < 1) values else values[0]
    }

    fun setValue(target: IBase?, path: String, value: Any?) {
        var value = value
        if (target == null) {
            return
        }

        val definition = resolveRuntimeDefinition<IBase>(target)
        if (path.contains(("."))) {
            setNestedValue(target, path, value, definition)
        } else {
            if (!path.contains("[x]")) {
                val childDef = definition.getChildByName(path)
                if (childDef != null) {
                    val elementDef = childDef.getChildByName(path)
                    if (
                        elementDef != null &&
                            elementDef.implementingClass.simpleName == ENUMERATION &&
                            value != null &&
                            (value.javaClass.simpleName != ENUMERATION)
                    ) {
                        value =
                            getEnumValue<Enum<*>, IBaseEnumeration<Enum<*>>>(
                                childDef as RuntimeChildPrimitiveEnumerationDatatypeDefinition,
                                value,
                            )
                    }
                }
            }

            if (target is IBaseEnumeration<*> && path == "value") {
                target.valueAsString = value as String?
                return
            }

            if (target is IPrimitiveType<*>) {
                setPrimitiveValue(value!!, target)
                return
            }

            var child = definition.getChildByName(path)
            if (child == null) {
                child = resolveChoiceProperty(definition, path)
            }

            requireNotNull(child) { "Unable to resolve path $path." }

            try {
                if (value is Iterable<*>) {
                    for (`val` in value) {
                        child.mutator.addValue(
                            target,
                            setBaseValue(`val`, target, getChildType(child)),
                        )
                    }
                } else {
                    child.mutator.setValue(target, setBaseValue(value, target, getChildType(child)))
                }
            } catch (le: IllegalArgumentException) {
                throw UnprocessableEntityException("Configuration error encountered: ${le.message}")
            }
        }
    }

    protected fun setPrimitiveValue(value: Any?, target: IPrimitiveType<*>) {
        val simpleName = target.javaClass.simpleName
        @Suppress("UNCHECKED_CAST")
        when (simpleName) {
            "DateTimeType",
            "InstantType" -> target.valueAsString = value.toString()
            "TimeType" -> (target as IPrimitiveType<String?>).value = value.toString()
            "Base64BinaryType" -> target.valueAsString = value as String?
            else -> (target as IPrimitiveType<Any?>).value = value
        }
    }

    protected fun setBaseValue(value: Any?, target: IBase?, type: Class<*>?): IBase? {
        if (target is IPrimitiveType<*>) {
            setPrimitiveValue(value, target)
        }
        return (if (type == null) value else AdapterHelper.`as`(fhirVersion, value, type)) as IBase?
    }

    protected fun getChildType(child: BaseRuntimeChildDefinition?): Class<*>? {
        if (child is BaseRuntimeChildDatatypeDefinition) {
            return child.datatype
        }
        return null
    }

    protected fun <T : IBase> resolveRuntimeDefinition(
        base: IBase
    ): BaseRuntimeElementCompositeDefinition<T?> {
        @Suppress("UNCHECKED_CAST")
        return when (base) {
            is IAnyResource -> fhirContext.getResourceDefinition(base)

            is IBaseBackboneElement,
            is IBaseElement -> fhirContext.getElementDefinition(base.javaClass)

            is ICompositeType -> fhirContext.getElementDefinition(base.javaClass)

            else ->
                throw UnprocessableEntityException(
                    "Unable to resolve the runtime definition for ${base.javaClass.name}"
                )
        }
            as BaseRuntimeElementCompositeDefinition<T?>
    }

    protected fun resolveChoiceProperty(
        definition: BaseRuntimeElementCompositeDefinition<*>,
        path: String,
    ): BaseRuntimeChildDefinition? {
        for (child in definition.children) {
            if (child is RuntimeChildChoiceDefinition) {
                if (child.elementName.startsWith(path)) {
                    return child
                }
            }
        }

        return null
    }

    protected fun <T : Enum<*>, E : IBaseEnumeration<T>> getEnumValue(
        targetDef: RuntimeChildPrimitiveEnumerationDatatypeDefinition,
        value: Any?,
    ): E? {
        val enumValue: String?
        if (value is IPrimitiveType<*>) {
            enumValue = value.valueAsString
        } else if (value is IBaseCoding) {
            enumValue = value.code
        } else {
            enumValue = value.toString()
        }
        @Suppress("UNCHECKED_CAST")
        return when (fhirContext.version.version) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Enumeration<T>(
                    toEnumFactory(targetDef.boundEnumType),
                    enumValue,
                )
            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Enumeration<T>(
                    toEnumFactory(targetDef.boundEnumType),
                    enumValue,
                )

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Enumeration<T>(
                    toEnumFactory(targetDef.boundEnumType),
                    enumValue,
                )

            else -> null
        }
            as E?
    }

    fun setNestedValue(
        target: IBase,
        path: String,
        value: Any?,
        def: BaseRuntimeElementDefinition<*>,
    ) {
        var target = target
        var def = def
        val segments = splitPathSegments(path)
        for (i in segments.indices) {
            val segment = segments[i]
            val isLast = i == segments.size - 1

            if (isExtensionSegment(segment)) {
                val extInfo: ExtensionInfo = parseExtensionSegment(segment)
                target = resolveOrCreateExtension(target, extInfo)!!
                if (!isLast) {
                    def = fhirContext.getElementDefinition(target.javaClass)
                }
            } else {
                val isList = segment.contains("[")
                val isSlice = segment.contains(":")
                val sliceName = if (isSlice) segment.split(":")[1] else null
                val index =
                    if (isList) Character.getNumericValue(segment[segment.indexOf("[") + 1]) else 0
                val targetPath = getTargetPath(segment, isList, isSlice)
                val targetDef = def.getChildByName(targetPath)
                if (targetDef != null) {
                    val targetValues = targetDef.accessor.getValues(target)
                    val targetValue =
                        if (targetValues.size >= index + 1 && !isLast)
                            getTargetValueFromList(sliceName, index, targetValues)
                        else getTargetValue(target, value, isLast, targetPath, targetDef)
                    target = targetValue ?: target
                    if (!isLast) {
                        val nextDef = fhirContext.getElementDefinition(target.javaClass)
                        if (nextDef is BaseRuntimeElementCompositeDefinition<*>) def = nextDef
                        else if (nextDef is RuntimePrimitiveDatatypeDefinition) def = nextDef
                        else
                            throw UnprocessableEntityException(
                                "Unable to resolve the runtime definition for ${target.javaClass.name}"
                            )
                    }
                }
            }
        }
    }

    protected fun resolveOrCreateExtension(target: IBase, info: ExtensionInfo): IBase? {
        require(target is IBaseHasExtensions) {
            "Target does not support extensions: ${target.javaClass.name}"
        }
        val matching = target.extension.filter { ext -> info.url == ext!!.url }.toList()
        if (matching.size > info.index) {
            return matching[info.index]
        }
        val extensionList = target.extension as MutableList<IBaseExtension<*, *>>
        var created: IBaseExtension<*, *>? = null
        for (i in matching.size..info.index) {
            created = newExtension(info.url)
            extensionList.add(created)
        }
        return created
    }

    protected fun newExtension(url: String?): IBaseExtension<*, *> {
        return when (fhirContext.version.version) {
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Extension(url)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Extension(url)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Extension(url)
            else ->
                throw IllegalStateException(
                    "Unsupported FHIR version: ${fhirContext.version.version}"
                )
        }
    }

    protected fun getTargetPath(identifier: String, isList: Boolean, isSlice: Boolean): String {
        if (isList) {
            return identifier.replace("\\[\\d]".toRegex(), "")
        }
        if (isSlice) {
            return identifier.substring(0, identifier.indexOf(":"))
        }
        return identifier
    }

    protected fun getTargetValue(
        target: IBase?,
        value: Any?,
        isLast: Boolean,
        targetPath: String?,
        targetDef: BaseRuntimeChildDefinition,
    ): IBase? {
        var targetValue: IBase? = null
        val elementDef = targetDef.getChildByName(targetPath)
        if (isLast) {
            val elementClass: Class<out IBase?> = elementDef.implementingClass
            if (elementClass.simpleName == ENUMERATION) {
                if (
                    AdapterHelper.`as`(fhirVersion, value, IPrimitiveType::class.java)
                        is IPrimitiveType<*>
                ) {
                    targetValue =
                        getEnumValue<Enum<*>, IBaseEnumeration<Enum<*>>>(
                            targetDef as RuntimeChildPrimitiveEnumerationDatatypeDefinition,
                            (AdapterHelper.`as`(fhirVersion, value, IPrimitiveType::class.java)
                                    as IPrimitiveType<*>)
                                .valueAsString,
                        )
                }
            } else {
                targetValue = AdapterHelper.`as`(fhirVersion, value, elementClass) as IBase?
            }
        } else {
            targetValue = elementDef.newInstance(targetDef.instanceConstructorArguments)
        }
        if (targetValue != null) {
            targetDef.mutator.addValue(target, targetValue)
        }
        return targetValue
    }

    protected fun getTargetValueFromList(
        sliceName: String?,
        index: Int,
        targetValues: MutableList<IBase?>,
    ): IBase? {
        val targetValue: IBase?
        if (targetValues.size > 1 && !sliceName.isNullOrBlank()) {
            // TODO: handle slice names
            // targetValue = targetValues.stream()
            targetValue = targetValues[0]
        } else {
            targetValue = targetValues[index]
        }
        return targetValue
    }

    companion object {
        protected const val ENUMERATION = "Enumeration"
        protected val EXTENSION_PATTERN = "extension\\('([^']+)'\\)(\\[(\\d+)])?".toRegex()

        fun <E : IBaseEnumFactory<*>> toEnumFactory(enumerationType: Class<*>): E {
            val clazz: Class<*>?
            val className = enumerationType.name + "EnumFactory"
            try {
                clazz = Class.forName(className)
                @Suppress("UNCHECKED_CAST")
                return clazz.getDeclaredConstructor().newInstance() as E
            } catch (e: Exception) {
                throw UnprocessableEntityException("Failed to instantiate $className")
            }
        }

        protected fun splitPathSegments(path: String): MutableList<String> {
            val segments = mutableListOf<String>()
            var current = StringBuilder()
            var inQuotes = false
            for (i in path.indices) {
                val c = path[i]
                if (c == '\'') {
                    inQuotes = !inQuotes
                    current.append(c)
                } else if (c == '.' && !inQuotes) {
                    segments.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
            }
            if (current.isNotEmpty()) {
                segments.add(current.toString())
            }
            return segments
        }

        protected fun isExtensionSegment(segment: String): Boolean {
            return EXTENSION_PATTERN.matches(segment)
        }

        protected fun parseExtensionSegment(segment: String): ExtensionInfo {
            val matchResult = EXTENSION_PATTERN.matchEntire(segment)
            requireNotNull(matchResult) { "Not an extension segment: $segment" }
            val url = matchResult.groupValues[1]
            val index =
                if (matchResult.groupValues[3].isNotEmpty()) matchResult.groupValues[3].toInt()
                else 0
            return ExtensionInfo(url, index)
        }
    }
}
