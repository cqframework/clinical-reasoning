package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IItemComponentAdapter : IAdapter<IBase> {

    val linkId: String?

    fun hasDefinition(): Boolean

    val definition: String?

    fun hasItem(): Boolean

    var item: MutableList<out IItemComponentAdapter?>?

    fun addItem(item: IItemComponentAdapter?)
}
