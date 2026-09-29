package org.opencds.cqf.fhir.utility.adapter.dstu3

import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter

internal class ParametersAdapter(parameters: IBaseResource) :
    ResourceAdapter(parameters as Resource), IParametersAdapter {
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

    override fun setParameter(parametersParameterComponents: MutableList<IBaseBackboneElement?>?) {
        this.parameters.parameter =
            if (parametersParameterComponents == null) null
            else
                parametersParameterComponents
                    .map { x -> x as Parameters.ParametersParameterComponent? }
                    .toMutableList()
    }

    override fun addParameter(name: String?, value: String?) {
        this.parameters.addParameter().setName(name).setValue(StringType(value))
    }

    override fun setParameter(name: String?, value: Int) {
        if (hasParameter(name)) {
            getParameter(name)!!.setValue(IntegerType(value))
        } else {
            this.parameters.addParameter().setName(name).setValue(IntegerType(value))
        }
    }

    override fun addParameter(name: String?, value: IBase?) {
        if (value is Type) {
            this.parameters.addParameter().setName(name).setValue(value)
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

    override fun addParameter(): IParametersParameterComponentAdapter? {
        return adapterFactory.createParametersParameter(this.parameters.addParameter())
    }

    override fun <T : IBaseDatatype> getParameterValues(name: String?): MutableList<T?> {
        @Suppress("UNCHECKED_CAST")
        return this.parameters.parameter
            .filter { p -> p!!.name == name }
            .map { obj -> obj!!.value }
            .toMutableList() as MutableList<T?>
    }

    override fun hasParameter(name: String?): Boolean {
        return this.parameters.parameter.any { p -> p!!.name == name }
    }

    override fun getParameter(name: String?): IParametersParameterComponentAdapter? {
        return this.parameters.parameter
            .filter { p -> p!!.name == name }
            .map { parametersParameterComponent ->
                adapterFactory.createParametersParameter(parametersParameterComponent)
            }
            .firstOrNull()
    }
}
