package org.opencds.cqf.fhir.utility.search;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.param.UriParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.utility.Canonicals;

/*
 * This is a utility class to help construct search maps, which are then passed into the Repository
 * to perform a search. If you find that you have a certain type of search that you are repeating
 * frequently consider adding it to this class.
 */
public class Searches {

    public static final Map<String, List<IQueryParameterType>> ALL = Collections.emptyMap();
    private static final String PATIENT = "patient";
    private static final String ACTOR = "actor";
    private static final String SUBJECT = "subject";
    private static final String MEDICATION_STATEMENT = "MedicationStatement";
    private static final String MEDICATION_REQUEST = "MedicationRequest";
    private static final String MEDICATION_DISPENSE = "MedicationDispense";
    private static final String MEDICATION_ADMINISTRATION = "MedicationAdministration";
    private static final String PROCEDURE_REQUEST = "ProcedureRequest";

    private Searches() {}

    public static SearchBuilder builder() {
        return new SearchBuilder();
    }

    public static Map<String, List<IQueryParameterType>> byId(String... ids) {
        return builder().withTokenParam("_id", ids).build();
    }

    public static Map<String, List<IQueryParameterType>> byId(String id) {
        return builder().withTokenParam("_id", id).build();
    }

    public static Map<String, List<IQueryParameterType>> byProfile(String profile) {
        return builder().withUriParam("_profile", profile).build();
    }

    public static Map<String, List<IQueryParameterType>> byCanonical(String canonical) {
        var parts = Canonicals.getParts(canonical);
        if (parts.version() != null) {
            return byUrlAndVersion(parts.url(), parts.version());
        } else {
            return byUrl(parts.url());
        }
    }

    public static Map<String, List<IQueryParameterType>> byCodeAndSystem(String code, String system) {
        return builder().withTokenParam("code", code, system).build();
    }

    public static Map<String, List<IQueryParameterType>> byUrl(String url) {
        return builder().withUriParam("url", url).build();
    }

    public static Map<String, List<IQueryParameterType>> byUrlAndVersion(String url, String version) {
        return builder()
                .withUriParam("url", url)
                .withTokenParam("version", version)
                .build();
    }

    public static Map<String, List<IQueryParameterType>> byName(String name) {
        return builder().withStringParam("name", name).build();
    }

    public static Map<String, List<IQueryParameterType>> byStatus(String status) {
        return builder().withTokenParam("status", status).build();
    }

    public static Map<String, List<IQueryParameterType>> exceptStatus(String status) {
        return builder()
                .withModifiedTokenParam("status", TokenParamModifier.NOT, status)
                .build();
    }

    public static Map<String, List<IQueryParameterType>> byNameAndVersion(String name, String version) {
        if (version == null || version.isEmpty()) {
            return builder().withStringParam("name", name).build();
        } else {

            return builder()
                    .withStringParam("name", name)
                    .withTokenParam("version", version)
                    .build();
        }
    }

    public static class SearchBuilder {
        private Map<String, List<IQueryParameterType>> values;

        public Map<String, List<IQueryParameterType>> build() {
            return this.values;
        }

        public SearchBuilder withStringParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new StringParam(value)));

            return this;
        }

        public SearchBuilder withTokenParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new TokenParam(value)));

            return this;
        }

        public SearchBuilder withModifiedTokenParam(String name, TokenParamModifier modifier, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            var token = new TokenParam(value);
            token.setModifier(modifier);
            values.put(name, Collections.singletonList(token));

            return this;
        }

        public SearchBuilder withTokenParam(String name, String value, String system) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new TokenParam(system, value)));

            return this;
        }

        public SearchBuilder withUriParam(String name, String value) {
            if (values == null) {
                values = new HashMap<>();
            }
            values.put(name, Collections.singletonList(new UriParam(value)));

            return this;
        }

        SearchBuilder withTokenParam(String name, String... values) {
            if (this.values == null) {
                this.values = new HashMap<>();
            }

            var params = new ArrayList<IQueryParameterType>(1 + values.length);
            for (var v : values) {
                params.add(new TokenParam(v));
            }

            this.values.put(name, params);

            return this;
        }
    }

    public static String getPatientSearchParam(FhirVersionEnum fhirVersion, String dataType) {
        if (fhirVersion.equals(FhirVersionEnum.DSTU3)) {
            return switch (dataType) {
                case "Group" -> "member";
                case "Patient" -> "_id";
                case "ResearchSubject" -> "individual";
                case "Appointment", "AppointmentResponse", "Schedule" -> ACTOR;
                case "Account",
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
                        "List" -> SUBJECT;
                case "AllergyIntolerance",
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
                        "MeasureReport" -> PATIENT;
                default -> null;
            };
        }
        return switch (dataType) {
            case "Coverage" -> "policy-holder";
            case "Group" -> "member";
            case "Patient" -> "_id";
            case "ResearchSubject" -> "individual";
            case "Appointment", "AppointmentResponse", "Schedule" -> ACTOR;
            case "Account",
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
                    "RequestOrchestration" -> SUBJECT;
            case "AllergyIntolerance",
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
                    "VisionPrescription" -> PATIENT;
            default -> null;
        };
    }
}
