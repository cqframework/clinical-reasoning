package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.Identifier
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IIdentifierAdapter

class IdentifierAdapter(identifier: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, identifier), IIdentifierAdapter {
    private val identifier: Identifier

    init {
        require(identifier is Identifier) {
            "object passed as identifier argument is not an Identifier data type"
        }
        this.identifier = identifier
    }

    override fun get(): Identifier {
        return identifier
    }

    override val value: String?
        get() {
            return get().value
        }

    override fun hasValue(): Boolean {
        return get().hasValue()
    }

    override val system: String?
        get() {
            return get().system
        }

    override fun hasSystem(): Boolean {
        return get().hasSystem()
    }
}
