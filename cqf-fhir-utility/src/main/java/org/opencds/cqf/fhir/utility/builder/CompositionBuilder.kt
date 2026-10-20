package org.opencds.cqf.fhir.utility.builder

import java.util.Date
import org.hl7.fhir.instance.model.api.IDomainResource

class CompositionBuilder<T : IDomainResource> :
    BaseDomainResourceBuilder<CompositionBuilder<T>, T> {
    private var status: String? = null
    private var title: String? = null
    private var type: CodeableConceptSettings? = null
    private var subject: String? = null
    private var author: String? = null
    private var custodian: String? = null

    private var date = Date()

    constructor(resourceClass: Class<T>) : super(resourceClass)

    constructor(resourceClass: Class<T>, id: String) : super(resourceClass, id)

    constructor(
        resourceClass: Class<T>,
        id: String,
        type: CodeableConceptSettings,
        status: String,
        author: String,
        title: String,
    ) : this(resourceClass, id) {
        this.type = type
        this.status = status
        this.author = author
        this.title = title
    }

    fun withStatus(status: String): CompositionBuilder<T> {

        this.status = status

        return this
    }

    fun withTitle(title: String): CompositionBuilder<T> {

        this.title = title

        return this
    }

    fun withType(type: CodeableConceptSettings): CompositionBuilder<T> {

        this.type = type

        return this
    }

    fun withDate(date: Date): CompositionBuilder<T> {

        this.date = date

        return this
    }

    fun withSubject(subject: String?): CompositionBuilder<T> {
        this.subject = BaseResourceBuilder.ensurePatientReference(subject)

        return this
    }

    fun withAuthor(author: String): CompositionBuilder<T> {
        require(
            author.startsWith("Practitioner") ||
                author.startsWith("PractitionerRole") ||
                author.startsWith("Device") ||
                author.startsWith("Patient") ||
                author.startsWith("RelatedPerson") ||
                author.startsWith("Organization")
        )
        this.author = author

        return this
    }

    fun withCustodian(custodian: String?): CompositionBuilder<T> {
        this.custodian = BaseResourceBuilder.ensureOrganizationReference(custodian)

        return this
    }

    override fun build(): T {
        requireNotNull(type)
        requireNotNull(status)
        requireNotNull(author)
        requireNotNull(title)

        require(type!!.codingSettings.isNotEmpty() && type!!.codingSettings.size == 1)

        return super.build()
    }

    private val typeSetting: CodingSettings?
        get() = type!!.codingSettingsArray[0]

    override fun initializeDstu3(resource: T?) {
        super.initializeDstu3(resource)
        val composition = resource as org.hl7.fhir.dstu3.model.Composition

        composition
            .setDate(date)
            .setIdentifier(
                org.hl7.fhir.dstu3.model
                    .Identifier()
                    .setSystem(this.identifier!!.first)
                    .setValue(this.identifier!!.second)
            )
            .setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.valueOf(status!!))
            .setSubject(org.hl7.fhir.dstu3.model.Reference(subject))
            .setTitle(title)
            .setType(
                org.hl7.fhir.dstu3.model
                    .CodeableConcept()
                    .addCoding(
                        org.hl7.fhir.dstu3.model
                            .Coding()
                            .setSystem(this.typeSetting!!.system)
                            .setCode(this.typeSetting!!.code)
                            .setDisplay(this.typeSetting!!.display)
                    )
            )
            .addAuthor(org.hl7.fhir.dstu3.model.Reference(author))
            .setCustodian(org.hl7.fhir.dstu3.model.Reference(custodian))
    }

    override fun initializeR4(resource: T?) {
        super.initializeR4(resource)
        val composition = resource as org.hl7.fhir.r4.model.Composition

        composition
            .setDate(date)
            .setIdentifier(
                org.hl7.fhir.r4.model
                    .Identifier()
                    .setSystem(this.identifier!!.first)
                    .setValue(this.identifier!!.second)
            )
            .setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.valueOf(status!!))
            .setSubject(org.hl7.fhir.r4.model.Reference(subject))
            .setTitle(title)
            .setType(
                org.hl7.fhir.r4.model
                    .CodeableConcept()
                    .addCoding(
                        org.hl7.fhir.r4.model
                            .Coding()
                            .setSystem(this.typeSetting!!.system)
                            .setCode(this.typeSetting!!.code)
                            .setDisplay(this.typeSetting!!.display)
                    )
            )
            .addAuthor(org.hl7.fhir.r4.model.Reference(author))
            .setCustodian(org.hl7.fhir.r4.model.Reference(custodian))
    }

    override fun initializeR5(resource: T?) {
        super.initializeR5(resource)
        val composition = resource as org.hl7.fhir.r5.model.Composition
        val r5Identifiers = mutableListOf<org.hl7.fhir.r5.model.Identifier?>()
        val r5Identifier = org.hl7.fhir.r5.model.Identifier()
        r5Identifier.setSystem(this.identifier!!.first).setValue(this.identifier!!.second)
        r5Identifiers.add(r5Identifier)
        val r5References = mutableListOf<org.hl7.fhir.r5.model.Reference?>()
        val r5Reference = org.hl7.fhir.r5.model.Reference(subject)
        r5References.add(r5Reference)
        composition
            .setDate(date)
            .setIdentifier(r5Identifiers)
            .setStatus(org.hl7.fhir.r5.model.Enumerations.CompositionStatus.valueOf(status!!))
            .setSubject(r5References)
            .setTitle(title)
            .setType(
                org.hl7.fhir.r5.model
                    .CodeableConcept()
                    .addCoding(
                        org.hl7.fhir.r5.model
                            .Coding()
                            .setSystem(this.typeSetting!!.system)
                            .setCode(this.typeSetting!!.code)
                            .setDisplay(this.typeSetting!!.display)
                    )
            )
            .addAuthor(org.hl7.fhir.r5.model.Reference(author))
            .setCustodian(org.hl7.fhir.r5.model.Reference(custodian))
    }
}
