package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseResource

interface IParametersParameterComponentAdapter : IAdapter<IBase> {
    override fun get(): IBaseBackboneElement?

    val name: String?

    fun setName(name: String?): IParametersParameterComponentAdapter?

    fun hasName(): Boolean

    val part: MutableList<IParametersParameterComponentAdapter?>?

    fun getPartValues(name: String?): MutableList<IBase?>?

    fun setPart(
        parametersParameterComponents: MutableList<IBaseBackboneElement?>?
    ): IParametersParameterComponentAdapter?

    fun addPart(): IParametersParameterComponentAdapter?

    fun hasPart(): Boolean

    fun hasPart(name: String?): Boolean

    fun hasResource(): Boolean

    val resource: IBaseResource?

    fun setResource(resource: IBaseResource?): IParametersParameterComponentAdapter?

    fun hasValue(): Boolean

    fun hasPrimitiveValue(): Boolean

    val primitiveValue: String?

    fun setValue(value: IBaseDatatype?): IParametersParameterComponentAdapter?

    val value: IBaseDatatype?

    fun newTupleWithParts(): IBase?

    fun getPartValue(part: IParametersParameterComponentAdapter): IBase? {
        if (part.hasValue()) {
            return part.value
        } else if (part.hasResource()) {
            return part.resource
        } else if (part.hasPart()) {
            return part.newTupleWithParts()
        }
        return null
    }
}
