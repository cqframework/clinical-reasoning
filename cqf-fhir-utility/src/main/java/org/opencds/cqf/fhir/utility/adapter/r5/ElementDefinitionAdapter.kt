package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement
import org.hl7.fhir.r5.model.*
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter

class ElementDefinitionAdapter(elementDefinition: IBase) :
    BaseElementAdapter(FhirVersionEnum.R5, elementDefinition), IElementDefinitionAdapter {
    private val elementDefinition: ElementDefinition

    init {
        require(elementDefinition is ElementDefinition) {
            "object passed as elementDefinition argument is not a ElementDefinition data type"
        }
        this.elementDefinition = elementDefinition
    }

    override fun get(): ElementDefinition {
        return elementDefinition
    }

    override val id: String?
        get() {
            return get().id
        }

    override val path: String?
        get() {
            return get().path
        }

    override val sliceName: String?
        get() {
            return get().sliceName
        }

    override fun hasSlicing(): Boolean {
        return get().hasSlicing()
    }

    override val label: String?
        get() {
            return get().label
        }

    override fun hasLabel(): Boolean {
        return get().hasLabel()
    }

    override val code: MutableList<ICodingAdapter?>
        get() {
            return get().code.map { coding -> adapterFactory.createCoding(coding) }.toMutableList()
        }

    override val short: String?
        get() {
            return get().short
        }

    override fun hasShort(): Boolean {
        return get().hasShort()
    }

    override val definition: String?
        get() {
            return get().definition
        }

    override val comment: String?
        get() {
            return get().comment
        }

    override val requirements: String?
        get() {
            return get().requirements
        }

    override val alias: MutableList<String?>
        get() {
            return get().alias.map { obj -> obj!!.asStringValue() }.toMutableList()
        }

    override val min: Int
        get() {
            return get().min
        }

    override fun hasMin(): Boolean {
        return get().hasMin()
    }

    override val max: String?
        get() {
            return get().max
        }

    override fun hasMax(): Boolean {
        return get().hasMax()
    }

    override fun <T : IBase?> getType(): MutableList<T?> {
        return get().type.map { t -> t as T? }.toMutableList()
    }

    override val typeCode: String?
        get() {
            return get().typeFirstRep.code
        }

    override val typeProfile: String?
        get() {
            return (get().typeFirstRep.profile.firstOrNull() ?: CanonicalType()).asStringValue()
        }

    override fun <T : IBaseDatatype?> getDefaultValue(): T? {
        return get().defaultValue as T?
    }

    override fun hasDefaultValue(): Boolean {
        return get().hasDefaultValue()
    }

    override fun <T : IBaseDatatype?> getFixed(): T? {
        return get().fixed as T?
    }

    override fun hasFixed(): Boolean {
        return get().hasFixed()
    }

    override fun <T : IBaseDatatype?> getPattern(): T? {
        return get().pattern as T?
    }

    override fun hasPattern(): Boolean {
        return get().hasPattern()
    }

    override fun <T : IBaseDatatype?> getFixedOrPattern(): T? {
        return get().fixedOrPattern as T?
    }

    override fun hasFixedOrPattern(): Boolean {
        return get().hasFixedOrPattern()
    }

    override fun <T : IBaseDatatype?> getDefaultOrFixedOrPattern(): T? {
        return if (hasFixedOrPattern()) getFixedOrPattern<T?>() else getDefaultValue<T?>()
    }

    override fun hasDefaultOrFixedOrPattern(): Boolean {
        return hasDefaultValue() || hasFixedOrPattern()
    }

    override val mustSupport: Boolean
        get() {
            return get().mustSupport
        }

    override fun <T : IBaseDatatypeElement?> getBinding(): T? {
        return get().binding as T?
    }

    override fun hasBinding(): Boolean {
        return get().hasBinding()
    }

    override val bindingValueSet: String?
        get() {
            return if (hasBinding()) get().binding.valueSet else null
        }

    override val isModifier: Boolean
        get() {
            return get().isModifier
        }

    override fun hasCondition(): Boolean {
        return get().hasCondition()
    }

    override val baseMin: Int
        get() {
            return if (get().hasBase()) get().base.min else 0
        }

    override val baseMax: String?
        get() {
            return if (get().hasBase()) get().base.max else "*"
        }

    override val basePath: String?
        get() {
            return if (get().hasBase()) get().base.path else null
        }

    override val bindingStrength: String?
        get() {
            return if (hasBinding() && get().binding.hasStrength()) get().binding.strength.toCode()
            else null
        }

    override fun hasMaxLength(): Boolean {
        return get().hasMaxLength()
    }

    override val extensionUrls: MutableList<String?>
        get() {
            return get().extension.map { e -> e!!.url }.toMutableList()
        }

    override fun hasR5KeyConstraints(): Boolean {
        return get().hasMustHaveValue() ||
            get().hasValueAlternatives() ||
            get().hasMinValue() ||
            get().hasMaxValue()
    }
}
