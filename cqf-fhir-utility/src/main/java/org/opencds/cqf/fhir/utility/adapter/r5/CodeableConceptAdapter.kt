package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r5.model.CodeableConcept
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter

class CodeableConceptAdapter(codeableConcept: IBase) :
    BaseElementAdapter(FhirVersionEnum.R5, codeableConcept), ICodeableConceptAdapter {
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

    override fun fhirContext(): FhirContext? {
        return fhirContext
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
}
