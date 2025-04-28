package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.CrDiscoveryElement.getCdsServiceJson;
import static org.opencds.cqf.fhir.utility.Constants.CQF_FHIR_QUERY_PATTERN;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrUtils;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

@SuppressWarnings("squid:S1135")
public class CrDiscoveryService implements ICrDiscoveryService {

    protected static final String PATIENT_ID_CONTEXT = "{{context.patientId}}";
    protected static final int DEFAULT_MAX_URI_LENGTH = 8000;
    private static final String PATIENT = "patient";
    private static final String ACTOR = "actor";
    private static final String SUBJECT = "subject";
    private static final String MEDICATION_STATEMENT = "MedicationStatement";
    private static final String MEDICATION_REQUEST = "MedicationRequest";
    private static final String MEDICATION_DISPENSE = "MedicationDispense";
    private static final String MEDICATION_ADMINISTRATION = "MedicationAdministration";
    private static final String MEDICATION = "medication";
    private static final String PROCEDURE_REQUEST = "ProcedureRequest";
    protected int maxUriLength;

    protected final IRepository repository;
    protected final IIdType planDefinitionId;
    protected final IAdapterFactory adapterFactory;

    public CrDiscoveryService(IIdType planDefinitionId, IRepository repository) {
        this.planDefinitionId = planDefinitionId;
        this.repository = repository;
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        this.maxUriLength = DEFAULT_MAX_URI_LENGTH;
    }

    protected FhirVersionEnum fhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public CdsServiceJson resolveService() {
        return resolveService(CdsCrUtils.readPlanDefinitionFromRepository(repository, planDefinitionId));
    }

    protected CdsServiceJson resolveService(IBaseResource resource) {
        if (resource != null && resource.fhirType().equals("PlanDefinition")) {
            var planDef = (IPlanDefinitionAdapter) adapterFactory.createResource(resource);
            return getCdsServiceJson(planDef, getPrefetchUrlList(planDef));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ILibraryAdapter resolvePrimaryLibrary(IPlanDefinitionAdapter planDefinition) {
        IPrimitiveType<String> canonical = null;
        var dataReqExt = planDefinition.getExtensionByUrl(CRMI_EFFECTIVE_DATA_REQUIREMENTS);
        var dataReqExtValue = dataReqExt == null ? null : dataReqExt.getValue();
        // Use a Module Definition Library with Effective Data Requirements for the Plan Definition if it exists
        if (dataReqExtValue instanceof IPrimitiveType<?> moduleDefCanonical) {
            canonical = (IPrimitiveType<String>) moduleDefCanonical;
        }
        // Otherwise use the primary Library
        if (canonical == null && planDefinition.hasLibrary()) {
            // The CPGComputablePlanDefinition profile limits the cardinality of library to 1
            canonical = VersionUtilities.canonicalTypeForVersion(
                    fhirVersion(), planDefinition.getLibrary().get(0));
        }
        if (canonical != null) {
            return adapterFactory.createLibrary(SearchHelper.searchRepositoryByCanonical(repository, canonical));
        }
        return null;
    }

    public List<String> resolveValueCodingCodes(List<ICodingAdapter> valueCodings) {
        List<String> result = new ArrayList<>();

        var codes = new StringBuilder();
        for (var coding : valueCodings) {
            if (coding.hasCode()) {
                String system = coding.getSystem();
                String code = coding.getCode();

                codes = getCodesStringBuilder(result, codes, system, code);
            }
        }

        result.add(codes.toString());
        return result;
    }

    public List<String> resolveValueSetCodes(IPrimitiveType<String> valueSetId) {
        var valueSet = (IValueSetAdapter)
                adapterFactory.createResource(SearchHelper.searchRepositoryByCanonical(repository, valueSetId));
        List<String> result = new ArrayList<>();
        var codes = new StringBuilder();
        if (valueSet.hasExpansion() && valueSet.hasExpansionContains()) {
            for (var contains : valueSet.getExpansionContains()) {
                var system = contains.getSystem();
                var code = contains.getCode();
                codes = getCodesStringBuilder(result, codes, system, code);
            }
        } else if (valueSet.hasComposeInclude()) {
            for (var concepts : valueSet.getComposeInclude()) {
                var system = concepts.getSystem();
                if (concepts.hasConcept()) {
                    for (var concept : concepts.getConcept()) {
                        var code = concept.getCode();
                        codes = getCodesStringBuilder(result, codes, system, code);
                    }
                }
            }
        }
        result.add(codes.toString());
        return result;
    }

    protected StringBuilder getCodesStringBuilder(List<String> ret, StringBuilder codes, String system, String code) {
        var codeToken = system + "|" + code;
        var postAppendLength = codes.length() + codeToken.length();

        if (codes.length() > 0 && postAppendLength < maxUriLength) {
            codes.append(",");
        } else if (postAppendLength > maxUriLength) {
            ret.add(codes.toString());
            codes = new StringBuilder();
        }
        codes.append(codeToken);
        return codes;
    }

    public List<String> createRequestUrl(IDataRequirementAdapter dataRequirement) {
        if (dataRequirement == null) {
            return List.of();
        }

        // if we have a fhirQueryPattern extensions, use them
        var fhirQueryExtList = dataRequirement.getExtension().stream()
                .filter(e -> e.getUrl().equals(CQF_FHIR_QUERY_PATTERN) && e.getValue() != null)
                .map(e -> ((IPrimitiveType<?>) e.getValue()).getValueAsString())
                .toList();
        if (!fhirQueryExtList.isEmpty()) {
            return fhirQueryExtList;
        }

        // else build the query
        if (!dataRequirement.hasType() || !isPatientCompartment(dataRequirement.getType())) {
            return List.of();
        }
        var patientRelatedResource = dataRequirement.getType() + "?"
                + getPatientSearchParam(dataRequirement.getType())
                + "=Patient/" + PATIENT_ID_CONTEXT;

        // In the future we should consider adding support for the valueFilter extension
        // http://hl7.org/fhir/extensions/5.1.0/StructureDefinition-cqf-valueFilter.html

        if (dataRequirement.hasCodeFilter()) {
            return createRequestUrlHasFilters(dataRequirement, patientRelatedResource);
        } else {
            return List.of(patientRelatedResource);
        }
    }

    private List<String> createRequestUrlHasFilters(
            IDataRequirementAdapter dataRequirement, String patientRelatedResource) {
        List<String> ret = new ArrayList<>();
        for (var codeFilterComponent : dataRequirement.getCodeFilter()) {
            if (!codeFilterComponent.hasPath()) continue;
            String path = mapCodePathToSearchParam(dataRequirement.getType(), codeFilterComponent.getPath());
            if (codeFilterComponent.hasValueSet()) {
                for (String codes : resolveValueSetCodes(codeFilterComponent.getValueSet())) {
                    ret.add(patientRelatedResource + "&" + path + "=" + codes);
                }
            } else if (codeFilterComponent.hasCode()) {
                handleCodeFilters(patientRelatedResource, codeFilterComponent.getCode(), ret, path);
            }
        }
        return ret;
    }

    private void handleCodeFilters(
            String patientRelatedResource, List<ICodingAdapter> codeFilterValueCodings, List<String> ret, String path) {
        boolean isFirstCodingInFilter = true;
        for (String code : resolveValueCodingCodes(codeFilterValueCodings)) {
            if (isFirstCodingInFilter) {
                ret.add(patientRelatedResource + "&" + path + "=" + code);
            } else {
                ret.add("," + code);
            }

            isFirstCodingInFilter = false;
        }
    }

    public PrefetchUrlList getPrefetchUrlList(IPlanDefinitionAdapter planDefinition) {
        var prefetchList = new PrefetchUrlList();
        if (planDefinition == null) {
            return prefetchList;
        }
        var library = resolvePrimaryLibrary(planDefinition);
        if (library == null || !library.hasDataRequirement()) {
            return prefetchList;
        }
        for (var dataRequirement : library.getDataRequirement()) {
            List<String> requestUrls = createRequestUrl(dataRequirement);
            if (requestUrls != null) {
                prefetchList.addAll(requestUrls);
            }
        }
        return prefetchList;
    }

    protected String mapCodePathToSearchParam(String dataType, String path) {
        switch (dataType) {
            case MEDICATION_ADMINISTRATION, MEDICATION_DISPENSE, MEDICATION_REQUEST, MEDICATION_STATEMENT:
                if (path.equals(MEDICATION)) return "code";
                break;
            case PROCEDURE_REQUEST:
                if (path.equals("bodySite")) return "body-site";
                break;
            default:
                if (path.equals("vaccineCode")) return "vaccine-code";
                break;
        }
        return path.replace('.', '-').toLowerCase();
    }

    public boolean isPatientCompartment(String dataType) {
        return switch (dataType) {
            case "Account",
                    "AdverseEvent",
                    "AllergyIntolerance",
                    "Appointment",
                    "AppointmentResponse",
                    "AuditEvent",
                    "Basic",
                    "BodySite",
                    "CarePlan",
                    "CareTeam",
                    "ChargeItem",
                    "Claim",
                    "ClaimResponse",
                    "ClinicalImpression",
                    "Communication",
                    "CommunicationRequest",
                    "Composition",
                    "Condition",
                    "Consent",
                    "Coverage",
                    "DetectedIssue",
                    "DeviceRequest",
                    "DeviceUseStatement",
                    "DiagnosticReport",
                    "DocumentManifest",
                    "EligibilityRequest",
                    "Encounter",
                    "EnrollmentRequest",
                    "EpisodeOfCare",
                    "ExplanationOfBenefit",
                    "FamilyMemberHistory",
                    "Flag",
                    "Goal",
                    "Group",
                    "ImagingManifest",
                    "ImagingStudy",
                    "Immunization",
                    "ImmunizationRecommendation",
                    "List",
                    "MeasureReport",
                    "Media",
                    MEDICATION_ADMINISTRATION,
                    MEDICATION_DISPENSE,
                    MEDICATION_REQUEST,
                    MEDICATION_STATEMENT,
                    "NutritionOrder",
                    "Observation",
                    "Patient",
                    "Person",
                    "Procedure",
                    PROCEDURE_REQUEST,
                    "Provenance",
                    "QuestionnaireResponse",
                    "ReferralRequest",
                    "RelatedPerson",
                    "RequestGroup",
                    "RequestOrchestration",
                    "ResearchSubject",
                    "RiskAssessment",
                    "Schedule",
                    "Specimen",
                    "SupplyDelivery",
                    "SupplyRequest",
                    "VisionPrescription",
                    "BodyStructure",
                    "CoverageEligibilityRequest",
                    "CoverageEligibilityResponse",
                    "DocumentReference",
                    "ImmunizationEvaluation",
                    "Invoice",
                    "MolecularSequence",
                    "ServiceRequest" -> true;
            default -> false;
        };
    }

    public String getPatientSearchParam(String dataType) {
        if (fhirVersion().equals(FhirVersionEnum.DSTU3)) {
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
