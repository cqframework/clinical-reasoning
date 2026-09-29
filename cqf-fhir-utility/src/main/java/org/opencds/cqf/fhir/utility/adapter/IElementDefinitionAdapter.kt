package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement

interface IElementDefinitionAdapter : IAdapter<IBase> {

    val id: String?

    val path: String?

    val sliceName: String?

    fun hasSlicing(): Boolean

    val label: String?

    fun hasLabel(): Boolean

    val code: MutableList<ICodingAdapter?>?

    val short: String?

    fun hasShort(): Boolean

    val definition: String?

    val comment: String?

    val requirements: String?

    val alias: MutableList<String?>?

    val min: Int

    fun hasMin(): Boolean

    val isRequired: Boolean
        get() = this.min != 0

    val max: String?

    fun hasMax(): Boolean

    fun <T : IBase?> getType(): MutableList<T?>?

    /**
     * Returns the code of the first rep of the type property.
     *
     * @return
     */
    val typeCode: String?

    /**
     * Returns the first profile of the first rep of the type property.
     *
     * @return
     */
    val typeProfile: String?

    fun <T : IBaseDatatype?> getDefaultValue(): T?

    fun hasDefaultValue(): Boolean

    fun <T : IBaseDatatype?> getFixed(): T?

    fun hasFixed(): Boolean

    fun <T : IBaseDatatype?> getPattern(): T?

    fun hasPattern(): Boolean

    fun <T : IBaseDatatype?> getFixedOrPattern(): T?

    fun hasFixedOrPattern(): Boolean

    fun <T : IBaseDatatype?> getDefaultOrFixedOrPattern(): T?

    fun hasDefaultOrFixedOrPattern(): Boolean

    val mustSupport: Boolean

    fun <T : IBaseDatatypeElement?> getBinding(): T?

    fun hasBinding(): Boolean

    val bindingValueSet: String?

    val isModifier: Boolean

    fun hasCondition(): Boolean

    val baseMin: Int

    val baseMax: String?

    val basePath: String?

    val bindingStrength: String?

    fun hasMaxLength(): Boolean

    val extensionUrls: MutableList<String?>
        get() = mutableListOf()

    /**
     * Returns true if this element has R5-specific key constraints (mustHaveValue,
     * valueAlternatives, minValue, maxValue). Default returns false for non-R5 versions.
     */
    fun hasR5KeyConstraints(): Boolean {
        return false
    }
}
