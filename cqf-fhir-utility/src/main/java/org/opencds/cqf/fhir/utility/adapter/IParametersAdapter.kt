package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseResource

interface IParametersAdapter : IResourceAdapter {
    fun hasParameter(): Boolean

    val parameter: MutableList<IParametersParameterComponentAdapter?>?

    fun hasParameter(name: String?): Boolean

    fun getParameter(name: String?): IParametersParameterComponentAdapter?

    fun <T : IBaseDatatype> getParameterValues(name: String?): MutableList<T?>?

    fun setParameter(parametersParameterComponents: MutableList<IBaseBackboneElement?>?)

    fun addParameter(parameter: IBase?)

    fun addParameter(name: String?, value: String?)

    fun setParameter(name: String?, value: Int)

    fun addParameter(name: String?, value: IBase?)

    fun addParameter(name: String?, resource: IBaseResource?)

    fun addParameter(): IParametersParameterComponentAdapter?
}
