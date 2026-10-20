package org.opencds.cqf.fhir.utility.builder

import org.hl7.fhir.instance.model.api.IDomainResource

class DetectedIssueBuilder<T : IDomainResource> :
    BaseDomainResourceBuilder<DetectedIssueBuilder<T>, T> {
    private var status: String? = null
    private var c: CodeableConceptSettings? = null
    private var patient: String? = null
    private var evidenceDetails: MutableList<String?>? = null

    constructor(resourceClass: Class<T>) : super(resourceClass)

    constructor(resourceClass: Class<T>, id: String) : super(resourceClass, id)

    constructor(
        resourceClass: Class<T>,
        id: String,
        status: String,
        evidenceDetail: String,
    ) : super(resourceClass, id) {
        this.status = status
        addEvidenceDetail(evidenceDetail)
    }

    private fun addEvidenceDetail(evidenceDetail: String?) {
        if (evidenceDetails == null) {
            evidenceDetails = mutableListOf()
        }

        evidenceDetails!!.add(evidenceDetail)
    }

    private fun getEvidenceDetails(): MutableList<String?> {
        if (evidenceDetails == null) {
            return mutableListOf()
        }

        return evidenceDetails!!
    }

    fun withStatus(status: String?): DetectedIssueBuilder<T> {
        this.status = status

        return this
    }

    fun withCode(code: CodeableConceptSettings): DetectedIssueBuilder<T> {

        c = code

        return this
    }

    fun withPatient(patient: String?): DetectedIssueBuilder<T> {
        this.patient = BaseResourceBuilder.ensurePatientReference(patient)

        return this
    }

    fun withEvidenceDetail(evidenceDetail: String): DetectedIssueBuilder<T> {

        addEvidenceDetail(evidenceDetail)

        return this
    }

    override fun build(): T {
        requireNotNull(status)
        requireNotNull(evidenceDetails)
        require(evidenceDetails!!.isNotEmpty())

        return super.build()
    }

    private val codeSetting: CodingSettings?
        get() = c!!.codingSettingsArray[0]

    override fun initializeDstu3(resource: T?) {
        super.initializeDstu3(resource)
        val detectedIssue = resource as org.hl7.fhir.dstu3.model.DetectedIssue

        detectedIssue
            .setIdentifier(
                org.hl7.fhir.dstu3.model
                    .Identifier()
                    .setSystem(this.identifier!!.first)
                    .setValue(this.identifier!!.second)
            )
            .setPatient(org.hl7.fhir.dstu3.model.Reference(patient))
            .setStatus(org.hl7.fhir.dstu3.model.DetectedIssue.DetectedIssueStatus.valueOf(status!!))
        getEvidenceDetails().forEach { value -> detectedIssue.setReference(value) }
    }

    override fun initializeR4(resource: T?) {
        super.initializeR4(resource)
        val detectedIssue = resource as org.hl7.fhir.r4.model.DetectedIssue

        val identifier = mutableListOf<org.hl7.fhir.r4.model.Identifier?>()
        identifier.add(
            org.hl7.fhir.r4.model
                .Identifier()
                .setSystem(this.identifier!!.first)
                .setValue(this.identifier!!.second)
        )

        detectedIssue
            .setIdentifier(identifier)
            .setPatient(org.hl7.fhir.r4.model.Reference(patient))
            .setStatus(org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueStatus.valueOf(status!!))
            .setCode(
                org.hl7.fhir.r4.model
                    .CodeableConcept()
                    .addCoding(
                        org.hl7.fhir.r4.model
                            .Coding()
                            .setSystem(this.codeSetting!!.system)
                            .setCode(this.codeSetting!!.code)
                            .setDisplay(this.codeSetting!!.display)
                    )
            )
        getEvidenceDetails().forEach { evidence ->
            detectedIssue.addEvidence(
                org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueEvidenceComponent()
                    .addDetail(org.hl7.fhir.r4.model.Reference(evidence))
            )
        }
    }

    override fun initializeR5(resource: T?) {
        super.initializeR5(resource)
        val detectedIssue = resource as org.hl7.fhir.r5.model.DetectedIssue

        val identifier = mutableListOf<org.hl7.fhir.r5.model.Identifier?>()
        identifier.add(
            org.hl7.fhir.r5.model
                .Identifier()
                .setSystem(this.identifier!!.first)
                .setValue(this.identifier!!.second)
        )

        detectedIssue
            .setIdentifier(identifier)
            .setStatus(org.hl7.fhir.r5.model.DetectedIssue.DetectedIssueStatus.valueOf(status!!))
            .setCode(
                org.hl7.fhir.r5.model
                    .CodeableConcept()
                    .addCoding(
                        org.hl7.fhir.r5.model
                            .Coding()
                            .setSystem(this.codeSetting!!.system)
                            .setCode(this.codeSetting!!.code)
                            .setDisplay(this.codeSetting!!.display)
                    )
            )
        getEvidenceDetails().forEach { evidence ->
            detectedIssue.addEvidence(
                org.hl7.fhir.r5.model.DetectedIssue.DetectedIssueEvidenceComponent()
                    .addDetail(org.hl7.fhir.r5.model.Reference(evidence))
            )
        }
    }
}
