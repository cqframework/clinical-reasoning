package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.CodeType
import org.hl7.fhir.dstu3.model.Coding
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter

class CodingAdapter(coding: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, coding), ICodingAdapter {
    private val coding: Coding

    init {
        require(coding is Coding) {
            "object passed as codeableConcept argument is not a CodeableConcept data type"
        }
        this.coding = coding
    }

    override fun get(): Coding {
        return coding
    }

    override val codeType: CodeType?
        get() {
            return get().codeElement
        }

    override val code: String?
        get() {
            return get().code
        }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override fun setCode(code: String?): ICodingAdapter {
        get().code = code
        return this
    }

    override val display: String?
        get() {
            return get().display
        }

    override fun hasDisplay(): Boolean {
        return get().hasDisplay()
    }

    override fun setDisplay(display: String?): ICodingAdapter {
        get().display = display
        return this
    }

    override val system: String?
        get() {
            return get().system
        }

    override fun hasSystem(): Boolean {
        return get().hasSystem()
    }

    override fun setSystem(system: String?): ICodingAdapter {
        get().system = system
        return this
    }
}
