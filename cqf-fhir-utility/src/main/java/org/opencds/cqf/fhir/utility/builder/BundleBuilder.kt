package org.opencds.cqf.fhir.utility.builder

import java.util.Date
import org.hl7.fhir.instance.model.api.IBaseBundle

class BundleBuilder<T : IBaseBundle> : BaseResourceBuilder<BundleBuilder<T>, T> {
    private var type: String? = null

    private var timestamp: Date? = Date()

    constructor(resourceClass: Class<T>) : super(resourceClass)

    constructor(resourceClass: Class<T>, id: String) : super(resourceClass, id)

    constructor(resourceClass: Class<T>, id: String, type: String) : this(resourceClass, id) {

        this.type = type
    }

    fun withType(type: String): BundleBuilder<T> {

        this.type = type

        return this
    }

    fun withTimestamp(timestamp: Date?): BundleBuilder<T> {
        this.timestamp = timestamp

        return this
    }

    override fun build(): T {
        requireNotNull(type)

        return super.build()
    }

    override fun initializeDstu3(resource: T?) {
        super.initializeDstu3(resource)
        val bundle = resource as org.hl7.fhir.dstu3.model.Bundle

        bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.valueOf(type!!))

        bundle.setIdentifier(
            org.hl7.fhir.dstu3.model
                .Identifier()
                .setSystem(this.identifier!!.first)
                .setValue(this.identifier!!.second)
        )
    }

    override fun initializeR4(resource: T?) {
        super.initializeR4(resource)
        val bundle = resource as org.hl7.fhir.r4.model.Bundle

        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.valueOf(type!!))

        bundle.setIdentifier(
            org.hl7.fhir.r4.model
                .Identifier()
                .setSystem(this.identifier!!.first)
                .setValue(this.identifier!!.second)
        )

        bundle.setTimestamp(timestamp)
    }

    override fun initializeR5(resource: T?) {
        super.initializeR5(resource)
        val bundle = resource as org.hl7.fhir.r5.model.Bundle

        bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.valueOf(type!!))

        bundle.setIdentifier(
            org.hl7.fhir.r5.model
                .Identifier()
                .setSystem(this.identifier!!.first)
                .setValue(this.identifier!!.second)
        )

        bundle.setTimestamp(timestamp)
    }
}
