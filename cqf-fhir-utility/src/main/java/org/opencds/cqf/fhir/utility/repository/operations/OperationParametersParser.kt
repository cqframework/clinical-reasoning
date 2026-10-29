package org.opencds.cqf.fhir.utility.repository.operations

import java.util.*
import java.util.stream.Collectors
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter

/** This class maps the standard input parameters of an Operation to key, value pairs. */
class OperationParametersParser(adapterFactory: IAdapterFactory) {
    protected var adapterFactory: IAdapterFactory

    // protected FhirTypeConverter fhirTypeConverter;
    init {
        Objects.requireNonNull<IAdapterFactory?>(adapterFactory, "adapterFactory must not be null")
        // requireNonNull(fhirTypeConverter, "fhirTypeConverter must not be null");
        this.adapterFactory = adapterFactory
        // this.fhirTypeConverter = fhirTypeConverter;
    }

    fun addResourceChild(parameters: IBaseParameters?, name: String?, resource: IBaseResource?) {
        Objects.requireNonNull<IBaseParameters?>(parameters, "parameters must not be null")
        Objects.requireNonNull<String?>(name, "name must not be null")
        val parametersAdapter = this.adapterFactory.createParameters(parameters!!)
        val parts = parametersAdapter.parameter

        var part =
            parts!!
                .stream()
                .filter { x: IParametersParameterComponentAdapter? -> x!!.name == name }
                .findFirst()
                .orElse(null)
        if (part == null) {
            part = parametersAdapter.addParameter()
        }

        part!!.setName(name)
        part.setResource(resource)
        part.setValue(null)
    }

    fun addValueChild(parameters: IBaseParameters?, name: String?, value: IBaseDatatype?) {
        Objects.requireNonNull<IBaseParameters?>(parameters, "parameters must not be null")
        Objects.requireNonNull<String?>(name, "name must not be null")
        val parametersAdapter = this.adapterFactory.createParameters(parameters!!)
        val parts = parametersAdapter.parameter

        var part =
            parts!!
                .stream()
                .filter { x: IParametersParameterComponentAdapter? -> x!!.name == name }
                .findFirst()
                .orElse(null)
        if (part == null) {
            part = parametersAdapter.addParameter()
        }

        part!!.setName(name)
        part.setResource(null)
        part.setValue(value)
    }

    fun getResourceChild(parameters: IBaseParameters?, name: String): IBaseResource? {
        if (parameters == null) {
            return null
        }
        val parametersAdapter = this.adapterFactory.createParameters(parameters)
        return parametersAdapter.parameter!!
            .filter { x -> x!!.name == name }
            .map { x -> x!!.resource }
            .firstOrNull()
    }

    fun getResourceChildren(parameters: IBaseParameters?): MutableMap<String?, IBaseResource?>? {
        if (parameters == null) {
            return null
        }
        Objects.requireNonNull<IBaseParameters?>(parameters, "parameters must not be null")
        val parametersAdapter = this.adapterFactory.createParameters(parameters)
        return parametersAdapter.parameter!!
            .stream()
            .collect(
                Collectors.toMap(
                    IParametersParameterComponentAdapter::name,
                    IParametersParameterComponentAdapter::resource,
                )
            )
    }

    fun getValueChild(parameters: IBaseParameters?, name: String): IBaseDatatype? {
        if (parameters == null) {
            return null
        }
        val parametersAdapter = this.adapterFactory.createParameters(parameters)
        return parametersAdapter.parameter!!
            .filter { x -> x!!.name == name }
            .map { x -> x!!.value }
            .firstOrNull()
    }

    fun getValueChildren(parameters: IBaseParameters?): MutableMap<String?, IBaseDatatype?>? {
        if (parameters == null) {
            return null
        }
        Objects.requireNonNull<IBaseParameters?>(parameters, "parameters must not be null")
        val parametersAdapter = this.adapterFactory.createParameters(parameters)
        return parametersAdapter.parameter!!
            .stream()
            .collect(
                Collectors.toMap(
                    IParametersParameterComponentAdapter::name,
                    IParametersParameterComponentAdapter::value,
                )
            )
    }

    fun getParameterParts(parameters: IBaseParameters?): MutableMap<String?, Any?> {
        Objects.requireNonNull<IBaseParameters?>(parameters, "parameters must not be null")
        val parametersAdapter = adapterFactory.createParameters(parameters!!)
        val parts = parametersAdapter.parameter

        val parameterParts = mutableMapOf<String?, Any?>()
        for (part in parts!!) {
            val value: IBase? =
                if (part!!.hasValue()) part.value
                else if (part.hasResource()) part.resource else null
            if (value != null) {
                if (!parameterParts.containsKey(part.name)) {
                    parameterParts[part.name] = value
                } else {
                    val existingValue: Any = parameterParts.get(part.name)!!
                    if (existingValue is MutableList<*>) {
                        (existingValue as MutableList<IBase?>).add(value)
                        parameterParts[part.name] = existingValue
                    } else {
                        val newListValue = mutableListOf(existingValue, value)
                        parameterParts[part.name] = newListValue
                    }
                }
            }
        }
        return parameterParts
    }
}
