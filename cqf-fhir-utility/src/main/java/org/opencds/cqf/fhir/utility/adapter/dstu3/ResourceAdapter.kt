package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.Base
import org.hl7.fhir.dstu3.model.DomainResource
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.BaseResourceAdapter

open class ResourceAdapter(resource: IBaseResource) : BaseResourceAdapter(resource) {
    init {
        require(resource.structureFhirVersionEnum == FhirVersionEnum.DSTU3) {
            "resource is incorrect fhir version for this adapter"
        }
    }

    override val resource: Resource
        get() = super.resource as Resource

    val isDomainResource: Boolean
        get() = domainResource != null

    val domainResource: DomainResource?
        get() = if (resource is DomainResource) resource as DomainResource else null

    @Throws(FHIRException::class)
    override fun setProperty(name: String, value: IBase?): IBase? {
        return this.resource.setProperty(name, value as Base?)
    }

    @Throws(FHIRException::class)
    override fun addChild(name: String): IBase? {
        return this.resource.addChild(name)
    }

    @Throws(FHIRException::class)
    override fun getSingleProperty(name: String): IBase? {
        val values = getProperty(name, true)

        if (values.isNullOrEmpty()) {
            return null
        }

        require(values.size <= 1) { "more than one value found for property: $name" }

        return values[0]
    }

    @Throws(FHIRException::class)
    override fun getProperty(name: String): Array<out IBase?>? {
        return getProperty(name, true)
    }

    @Throws(FHIRException::class)
    override fun getProperty(name: String, checkValid: Boolean): Array<out IBase?>? {
        return this.resource.getProperty(name.hashCode(), name, checkValid)
    }

    @Throws(FHIRException::class)
    override fun makeProperty(name: String): IBase? {
        return this.resource.makeProperty(name.hashCode(), name)
    }

    @Throws(FHIRException::class)
    override fun getTypesForProperty(name: String): Array<String?>? {
        return this.resource.getTypesForProperty(name.hashCode(), name)
    }

    override fun copy(): IBaseResource? {
        return this.resource.copy()
    }

    override fun copyValues(dst: IBaseResource?) {
        this.resource.copyValues(dst as Resource?)
    }

    override fun equalsDeep(other: IBase?): Boolean {
        return this.resource.equalsDeep(other as Base?)
    }

    override fun equalsShallow(other: IBase?): Boolean {
        return this.resource.equalsShallow(other as Base?)
    }
}
