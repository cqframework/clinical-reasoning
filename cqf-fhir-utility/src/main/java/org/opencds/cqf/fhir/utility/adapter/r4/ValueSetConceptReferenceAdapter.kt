package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.ValueSet
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter

class ValueSetConceptReferenceAdapter(conceptReference: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, conceptReference), IValueSetConceptReferenceAdapter {
    private val conceptReference: ValueSet.ConceptReferenceComponent

    init {
        require(conceptReference is ValueSet.ConceptReferenceComponent) {
            "element passed as conceptReference argument is not a ConceptReferenceComponent element"
        }
        this.conceptReference = conceptReference
    }

    override fun get(): ValueSet.ConceptReferenceComponent {
        return conceptReference
    }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override val code: String?
        get() {
            return get().code
        }

    override val display: String?
        get() {
            return get().display
        }
}
