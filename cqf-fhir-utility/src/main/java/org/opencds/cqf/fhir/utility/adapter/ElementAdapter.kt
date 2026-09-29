package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase

class ElementAdapter(fhirVersion: FhirVersionEnum?, private val base: IBase) :
    BaseElementAdapter(fhirVersion, base) {
    override fun get(): IBase {
        return base
    }
}
