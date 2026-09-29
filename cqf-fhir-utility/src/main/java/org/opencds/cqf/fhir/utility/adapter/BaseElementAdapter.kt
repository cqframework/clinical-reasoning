package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase

abstract class BaseElementAdapter
protected constructor(fhirVersion: FhirVersionEnum?, protected val element: IBase) :
    BaseAdapter(FhirContext.forCached(fhirVersion)), IAdapter<IBase> {

    open fun setId(id: String?): IAdapter<*> {
        setValue(get(), "id", id)
        return this
    }
}
