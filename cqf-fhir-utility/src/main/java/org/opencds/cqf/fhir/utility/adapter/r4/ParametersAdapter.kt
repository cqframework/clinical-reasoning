package org.opencds.cqf.fhir.utility.adapter.r4

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Type
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter

internal class ParametersAdapter(parameters: IBaseResource) :
    ResourceAdapter(parameters), IParametersAdapter {
    protected val parameters: Parameters

    init {
        require(parameters.fhirType() == "Parameters") {
            "resource passed as parameters argument is not a Parameters resource"
        }

        this.parameters = parameters as Parameters
    }

    override fun hasParameter(): Boolean {
        return parameters.hasParameter()
    }

    override val parameter: MutableList<IParametersParameterComponentAdapter?>
        get() {
            return this.parameters.parameter
                .map { parametersParameterComponent ->
                    adapterFactory.createParametersParameter(parametersParameterComponent)
                }
                .toMutableList()
        }

    override fun <T : IBaseDatatype> getParameterValues(name: String?): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.parameters.getParameterValues(name) as MutableList<T?>?
    }

    override fun hasParameter(name: String?): Boolean {
        return parameters.hasParameter(name)
    }

    override fun getParameter(name: String?): IParametersParameterComponentAdapter? {
        val param = this.parameters.getParameter(name)
        return if (param == null) null else adapterFactory.createParametersParameter(param)
    }

    override fun setParameter(parametersParameterComponents: MutableList<IBaseBackboneElement?>?) {
        this.parameters.setParameter(
            if (parametersParameterComponents == null) null
            else
                parametersParameterComponents
                    .map { x -> x as Parameters.ParametersParameterComponent? }
                    .toMutableList()
        )
    }

    override fun addParameter(name: String?, value: String?) {
        this.parameters.addParameter(name, value)
    }

    override fun setParameter(name: String?, value: Int) {
        if (hasParameter(name)) {
            getParameter(name)!!.setValue(IntegerType(value))
        } else {
            this.parameters.addParameter(name, value)
        }
    }

    override fun addParameter(name: String?, value: IBase?) {
        if (value is Type) {
            this.parameters.addParameter(name, value)
        } else {
            throw IllegalArgumentException("element passed as value argument is not a valid type")
        }
    }

    override fun addParameter(name: String?, resource: IBaseResource?) {
        if (resource is Resource) {
            this.parameters.addParameter().setName(name).setResource(resource)
        } else {
            throw IllegalArgumentException(
                "element passed as value argument is not a valid data type"
            )
        }
    }

    override fun addParameter(parameter: IBase?) {
        if (parameter is Parameters.ParametersParameterComponent) {
            this.parameters.addParameter(parameter)
        } else {
            throw IllegalArgumentException(
                "element passed as parameter argument is not a valid parameter component"
            )
        }
    }

    override fun addParameter(): IParametersParameterComponentAdapter {
        return adapterFactory.createParametersParameter(this.parameters.addParameter())
    }
}
