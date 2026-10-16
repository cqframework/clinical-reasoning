package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter

class CodeableConceptAdapter(codeableConcept: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, codeableConcept), ICodeableConceptAdapter {
    private val codeableConcept: CodeableConcept

    init {
        require(codeableConcept is CodeableConcept) {
            "object passed as codeableConcept argument is not a CodeableConcept data type"
        }
        this.codeableConcept = codeableConcept
    }

    override fun get(): CodeableConcept {
        return codeableConcept
    }

    override fun hasCoding(): Boolean {
        return get().hasCoding()
    }

    override val coding: MutableList<ICodingAdapter?>
        get() {
            return get()
                .coding
                .map { coding -> adapterFactory.createCoding(coding) }
                .toMutableList()
        }

    override fun hasCoding(code: String?): Boolean {
        return get().coding.any { coding -> coding!!.code == code }
    }

    override fun addCoding(
        system: String?,
        code: String?,
        display: String?,
    ): ICodeableConceptAdapter {
        get().addCoding(Coding().setSystem(system).setCode(code).setDisplay(display))
        return this
    }
}
