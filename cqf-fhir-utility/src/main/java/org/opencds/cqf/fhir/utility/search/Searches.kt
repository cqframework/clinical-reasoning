package org.opencds.cqf.fhir.utility.search

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.model.api.IQueryParameterAnd
import ca.uhn.fhir.model.api.IQueryParameterOr
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.*
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import java.util.*
import org.opencds.cqf.fhir.utility.Canonicals.getParts

/*
 * This is a utility class to help construct search maps, which are then passed into the Repository
 * to perform a search. If you find that you have a certain type of search that you are repeating
 * frequently consider adding it to this class.
 */
object Searches {
    @JvmField val ALL = mutableMapOf<String?, MutableList<IQueryParameterType?>?>()
    private const val PATIENT = "patient"
    private const val ACTOR = "actor"
    private const val SUBJECT = "subject"
    private const val MEDICATION_STATEMENT = "MedicationStatement"
    private const val MEDICATION_REQUEST = "MedicationRequest"
    private const val MEDICATION_DISPENSE = "MedicationDispense"
    private const val MEDICATION_ADMINISTRATION = "MedicationAdministration"
    private const val PROCEDURE_REQUEST = "ProcedureRequest"

    @JvmStatic
    fun builder(): SearchBuilder {
        return SearchBuilder()
    }

    @JvmStatic
    fun byId(vararg ids: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withTokenParam("_id", *ids).build()
    }

    @JvmStatic
    fun byId(id: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withTokenParam("_id", id).build()
    }

    @JvmStatic
    fun byProfile(profile: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withProfile(profile).build()
    }

    @JvmStatic
    fun byCanonical(canonical: String): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        val parts = getParts(canonical)
        if (parts.version() != null) {
            return byUrlAndVersion(parts.url(), parts.version())
        } else {
            return byUrl(parts.url())
        }
    }

    @JvmStatic
    fun byCodeAndSystem(
        code: String?,
        system: String?,
    ): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withTokenParam("code", code, system).build()
    }

    @JvmStatic
    fun byUrl(url: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withUriParam("url", url).build()
    }

    @JvmStatic
    fun byUrlAndVersion(
        url: String?,
        version: String?,
    ): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withUriParam("url", url).withTokenParam("version", version).build()
    }

    @JvmStatic
    fun byName(name: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withStringParam("name", name).build()
    }

    @JvmStatic
    fun byStatus(status: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withTokenParam("status", status).build()
    }

    @JvmStatic
    fun exceptStatus(status: String?): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        return builder().withModifiedTokenParam("status", TokenParamModifier.NOT, status).build()
    }

    @JvmStatic
    fun byNameAndVersion(
        name: String?,
        version: String?,
    ): Multimap<String?, MutableList<IQueryParameterType?>?>? {
        val multimap: Multimap<String?, MutableList<IQueryParameterType?>?>?

        if (version.isNullOrEmpty()) {
            multimap = builder().withStringParam("name", name).build()
        } else {
            multimap =
                builder().withStringParam("name", name).withTokenParam("version", version).build()
        }

        return multimap
    }

    @JvmStatic
    fun toFlattenedMap(
        mm: Multimap<String?, MutableList<IQueryParameterType?>?>
    ): MutableMap<String?, MutableList<IQueryParameterType?>?> {
        val result = mutableMapOf<String?, MutableList<IQueryParameterType?>?>()

        mm.asMap().forEach { (key, listOfLists) ->
            val merged = listOfLists!!.filterNotNull().flatten().toMutableList()
            result[key] = merged
        }

        return result
    }

    @JvmStatic
    fun getPatientSearchParam(fhirVersion: FhirVersionEnum, dataType: String?): String? {
        if (fhirVersion == FhirVersionEnum.DSTU3) {
            return when (dataType) {
                "Group" -> "member"
                "Patient" -> "_id"
                "ResearchSubject" -> "individual"
                "Appointment",
                "AppointmentResponse",
                "Schedule" -> ACTOR
                "Account",
                "AdverseEvent",
                "ChargeItem",
                "ClinicalImpression",
                "Communication",
                "CommunicationRequest",
                "Composition",
                "DeviceUseStatement",
                "DiagnosticReport",
                "DocumentManifest",
                "DocumentReference",
                "DeviceRequest",
                "EnrollmentRequest",
                "SupplyRequest",
                "Specimen",
                "RiskAssessment",
                "RequestGroup",
                "QuestionnaireResponse",
                "Observation",
                MEDICATION_STATEMENT,
                MEDICATION_REQUEST,
                "Media",
                "List" -> SUBJECT
                "AllergyIntolerance",
                "AuditEvent",
                "Basic",
                "BodySite",
                "CarePlan",
                "CareTeam",
                "Claim",
                "ClaimResponse",
                "Condition",
                "Consent",
                "Coverage",
                "DetectedIssue",
                "EligibilityRequest",
                "Encounter",
                "EpisodeOfCare",
                "ExplanationOfBenefit",
                "FamilyMemberHistory",
                "Flag",
                "Goal",
                "ImagingManifest",
                "ImagingStudy",
                "Immunization",
                "ImmunizationRecommendation",
                "VisionPrescription",
                "SupplyDelivery",
                "RelatedPerson",
                "ReferralRequest",
                "Provenance",
                PROCEDURE_REQUEST,
                "Procedure",
                "Person",
                "NutritionOrder",
                MEDICATION_DISPENSE,
                MEDICATION_ADMINISTRATION,
                "MeasureReport" -> PATIENT

                else -> null
            }
        }
        return when (dataType) {
            "Coverage" -> "policy-holder"
            "Group" -> "member"
            "Patient" -> "_id"
            "ResearchSubject" -> "individual"
            "Appointment",
            "AppointmentResponse",
            "Schedule" -> ACTOR
            "Account",
            "AdverseEvent",
            "ChargeItem",
            "ClinicalImpression",
            "Communication",
            "CommunicationRequest",
            "Composition",
            "DeviceRequest",
            "DeviceUseStatement",
            "DiagnosticReport",
            "DocumentManifest",
            "DocumentReference",
            "EnrollmentRequest",
            "Invoice",
            "List",
            "Media",
            MEDICATION_REQUEST,
            MEDICATION_STATEMENT,
            "QuestionnaireResponse",
            "Observation",
            "RequestGroup",
            "RiskAssessment",
            "SupplyRequest",
            "Specimen",
            "RequestOrchestration" -> SUBJECT
            "AllergyIntolerance",
            "AuditEvent",
            "Basic",
            "BodyStructure",
            "CarePlan",
            "CareTeam",
            "Claim",
            "ClaimResponse",
            "Condition",
            "Consent",
            "DetectedIssue",
            "Encounter",
            "EpisodeOfCare",
            "ExplanationOfBenefit",
            "FamilyMemberHistory",
            "Flag",
            "Goal",
            "ImagingStudy",
            "Immunization",
            "ImmunizationRecommendation",
            "MeasureReport",
            MEDICATION_ADMINISTRATION,
            MEDICATION_DISPENSE,
            "MolecularSequence",
            "NutritionOrder",
            "Person",
            "Procedure",
            "Provenance",
            "RelatedPerson",
            "ServiceRequest",
            "SupplyDelivery",
            "VisionPrescription" -> PATIENT
            else -> null
        }
    }

    class SearchBuilder {
        private var values: Multimap<String?, MutableList<IQueryParameterType?>?>? = null

        fun build(): Multimap<String?, MutableList<IQueryParameterType?>?>? {
            return this.values
        }

        fun getValues(): Multimap<String?, MutableList<IQueryParameterType?>?> {
            if (values == null) {
                values = ArrayListMultimap.create<String?, MutableList<IQueryParameterType?>?>()
            }

            return values!!
        }

        fun withStringParam(name: String?, value: String?): SearchBuilder {
            getValues().put(name, mutableListOf(StringParam(value)))

            return this
        }

        fun withTokenParam(name: String?, value: String?): SearchBuilder {
            getValues().put(name, mutableListOf(TokenParam(value)))

            return this
        }

        fun withModifiedTokenParam(
            name: String?,
            modifier: TokenParamModifier?,
            value: String?,
        ): SearchBuilder {
            val token = TokenParam(value)
            token.setModifier(modifier)
            getValues().put(name, mutableListOf(token))

            return this
        }

        fun withTokenParam(name: String?, value: String?, system: String?): SearchBuilder {
            getValues().put(name, mutableListOf(TokenParam(system, value)))

            return this
        }

        fun withUriParam(name: String?, value: String?): SearchBuilder {
            getValues().put(name, mutableListOf(UriParam(value)))

            return this
        }

        fun withTokenParam(name: String?, vararg values: String?): SearchBuilder {
            val params = ArrayList<IQueryParameterType?>(1 + values.size)
            for (v in values) {
                params.add(TokenParam(v))
            }

            getValues().put(name, params)

            return this
        }

        fun withReferenceParam(spName: String?, referenceValue: String?): SearchBuilder {
            getValues().put(spName, mutableListOf(ReferenceParam(referenceValue)))
            return this
        }

        fun withProfile(profile: String?): SearchBuilder {
            return withUriParam("_profile", profile)
        }

        fun withAndListParam(spName: String?, andListParam: IQueryParameterAnd<*>): SearchBuilder {
            val lists = getValues().get(spName)
            andListParam.getValuesAsQueryTokens().forEach { dateOrListParam ->
                val list =
                    dateOrListParam.getValuesAsQueryTokens() as MutableList<IQueryParameterType?>?
                lists.add((list))
            }

            return this
        }

        fun withOrListParam(spName: String?, orListParam: IQueryParameterOr<*>): SearchBuilder {
            val qpList = orListParam.getValuesAsQueryTokens() as MutableList<IQueryParameterType?>?

            getValues().get(spName).add(qpList)
            return this
        }
    }
}
