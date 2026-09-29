package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.Parameters
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.dstu3.model.Type
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter

internal class ParametersParameterComponentAdapter(parametersParameterComponent: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, parametersParameterComponent),
    IParametersParameterComponentAdapter {
    protected val parametersParameterComponent: Parameters.ParametersParameterComponent

    init {
        require(parametersParameterComponent.fhirType() == "Parameters.parameter") {
            "element passed as parametersParameterComponent argument is not a ParametersParameterComponent Element"
        }

        this.parametersParameterComponent =
            parametersParameterComponent as Parameters.ParametersParameterComponent
    }

    override fun get(): IBaseBackboneElement {
        return this.parametersParameterComponent
    }

    override val name: String?
        get() {
            return this.parametersParameterComponent.name
        }

    override fun setName(name: String?): IParametersParameterComponentAdapter {
        this.parametersParameterComponent.setName(name)
        return this
    }

    override fun hasName(): Boolean {
        return this.parametersParameterComponent.hasName()
    }

    override val part: MutableList<IParametersParameterComponentAdapter?>
        get() {
            return this.parametersParameterComponent.part
                .map { parametersParameterComponent ->
                    adapterFactory.createParametersParameter(parametersParameterComponent)
                }
                .toMutableList()
        }

    override fun getPartValues(name: String?): MutableList<IBase?> {
        return this.parametersParameterComponent.part
            .filter { p -> p!!.name == name }
            .map { p -> if (p!!.hasResource()) p.resource else p.value }
            .filterNotNull()
            .map { obj -> IBase::class.java.cast(obj) }
            .toMutableList()
    }

    override fun setPart(
        parametersParameterComponents: MutableList<IBaseBackboneElement?>?
    ): IParametersParameterComponentAdapter {
        this.parametersParameterComponent.setPart(
            if (parametersParameterComponents == null) null
            else
                parametersParameterComponents
                    .map { x -> x as Parameters.ParametersParameterComponent? }
                    .toMutableList()
        )
        return this
    }

    override fun addPart(): IParametersParameterComponentAdapter? {
        return adapterFactory.createParametersParameter(this.parametersParameterComponent.addPart())
    }

    override fun hasPart(): Boolean {
        return this.parametersParameterComponent.hasPart()
    }

    override fun hasPart(name: String?): Boolean {
        for (part in this.part) {
            if (name == part!!.name) {
                return true
            }
        }
        return false
    }

    override fun hasResource(): Boolean {
        return this.parametersParameterComponent.hasResource()
    }

    override val resource: IBaseResource?
        get() {
            return this.parametersParameterComponent.resource
        }

    override fun setResource(resource: IBaseResource?): IParametersParameterComponentAdapter {
        this.parametersParameterComponent.setResource(resource as Resource?)
        return this
    }

    override fun hasValue(): Boolean {
        return this.parametersParameterComponent.hasValue()
    }

    override fun hasPrimitiveValue(): Boolean {
        return hasValue() && this.value is IPrimitiveType<*>
    }

    override fun setValue(value: IBaseDatatype?): IParametersParameterComponentAdapter {
        this.parametersParameterComponent.setValue(value as Type?)
        return this
    }

    override val value: IBaseDatatype?
        get() {
            return this.parametersParameterComponent.value
        }

    override val primitiveValue: String?
        get() {
            return if (hasPrimitiveValue()) this.parametersParameterComponent.value.primitiveValue()
            else null
        }

    override fun newTupleWithParts(): IBase? {
        return null
    }
}
