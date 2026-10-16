package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.ValueSet
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter

class ValueSetExpansionContainsAdapter(contains: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, contains), IValueSetExpansionContainsAdapter {
    private val contains: ValueSet.ValueSetExpansionContainsComponent

    init {
        require(contains is ValueSet.ValueSetExpansionContainsComponent) {
            "element passed as contains argument is not a ValueSetExpansionContainsComponent element"
        }
        this.contains = contains
    }

    override fun get(): ValueSet.ValueSetExpansionContainsComponent {
        return contains
    }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override val code: String?
        get() {
            return get().code
        }

    override fun hasSystem(): Boolean {
        return get().hasSystem()
    }

    override val system: String?
        get() {
            return get().system
        }

    override fun hasDisplay(): Boolean {
        return get().hasDisplay()
    }

    override val display: String?
        get() {
            return get().display
        }
}
