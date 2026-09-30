package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface ITupleAdapter : IAdapter<IBase> {
    fun getProperty(name: String?): Any?

    val properties: MutableMap<String, Any?>
}
