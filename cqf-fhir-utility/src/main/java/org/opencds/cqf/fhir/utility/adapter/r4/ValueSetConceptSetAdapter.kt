package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.ValueSet
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptSetAdapter

class ValueSetConceptSetAdapter(conceptSet: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, conceptSet), IValueSetConceptSetAdapter {
    private val conceptSet: ValueSet.ConceptSetComponent

    init {
        require(conceptSet is ValueSet.ConceptSetComponent) {
            "element passed as conceptSet argument is not a ConceptSetComponent element"
        }
        this.conceptSet = conceptSet
    }

    override fun get(): ValueSet.ConceptSetComponent {
        return conceptSet
    }

    override fun hasConcept(): Boolean {
        return get().hasConcept()
    }

    override val concept: MutableList<IValueSetConceptReferenceAdapter?>
        get() {
            return get()
                .concept
                .map { conceptReference -> ValueSetConceptReferenceAdapter(conceptReference) }
                .toMutableList()
        }

    override fun hasSystem(): Boolean {
        return get().hasSystem()
    }

    override val system: String?
        get() {
            return get().system
        }
}
