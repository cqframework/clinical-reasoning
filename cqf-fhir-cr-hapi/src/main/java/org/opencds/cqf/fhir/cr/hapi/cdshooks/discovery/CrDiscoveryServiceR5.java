package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrUtils;
import org.opencds.cqf.fhir.utility.r5.SearchHelper;

public class CrDiscoveryServiceR5 implements ICrDiscoveryService {

    protected static final String PATIENT_ID_CONTEXT = "{{context.patientId}}";
    protected static final int DEFAULT_MAX_URI_LENGTH = 8000;
    private static final String MEDICATION_ADMINISTRATION = "MedicationAdministration";
    private static final String MEDICATION_DISPENSE = "MedicationDispense";
    private static final String MEDICATION_REQUEST = "MedicationRequest";
    private static final String MEDICATION_STATEMENT = "MedicationStatement";
    private static final String MEDICATION = "medication";
    private static final String ACTOR = "actor";
    private static final String PATIENT = "patient";
    private static final String SUBJECT = "subject";
    protected int maxUriLength;

    protected final Repository repository;
    protected final IIdType planDefinitionId;

    public CrDiscoveryServiceR5(IIdType planDefinitionId, Repository repository) {
        this.planDefinitionId = planDefinitionId;
        this.repository = repository;
        this.maxUriLength = DEFAULT_MAX_URI_LENGTH;
    }

    public CdsServiceJson resolveService() {
        return resolveService(
                CdsCrUtils.readPlanDefinitionFromRepository(FhirVersionEnum.R5, repository, planDefinitionId));
    }

    protected CdsServiceJson resolveService(IBaseResource resource) {
        if (resource instanceof PlanDefinition planDef) {
            return new CrDiscoveryElementR5(planDef, getPrefetchUrlList(planDef)).getCdsServiceJson();
        }
        return null;
    }

    public boolean isEca(PlanDefinition planDefinition) {
        if (planDefinition.hasType() && planDefinition.getType().hasCoding()) {
            for (Coding coding : planDefinition.getType().getCoding()) {
                if (coding.getCode().equals("eca-rule")) {
                    return true;
                }
            }
        }
        return false;
    }

    public Library resolvePrimaryLibrary(PlanDefinition planDefinition) {
        //  CPGComputablePlanDefinition profile limits  cardinality of library to 1
        Library library = null;
        if (planDefinition.hasLibrary() && !planDefinition.getLibrary().isEmpty()) {
            library = (Library) SearchHelper.searchRepositoryByCanonical(
                    repository, planDefinition.getLibrary().get(0));
        }
        return library;
    }

    public List<String> resolveValueCodingCodes(List<Coding> valueCodings) {
        List<String> result = new ArrayList<>();

        StringBuilder codes = new StringBuilder();
        for (Coding coding : valueCodings) {
            if (coding.hasCode()) {
                String system = coding.getSystem();
                String code = coding.getCode();

                codes = getCodesStringBuilder(result, codes, system, code);
            }
        }

        result.add(codes.toString());
        return result;
    }

    public List<String> resolveValueSetCodes(CanonicalType valueSetId) {
        ValueSet valueSet = (ValueSet) SearchHelper.searchRepositoryByCanonical(repository, valueSetId);
        List<String> result = new ArrayList<>();
        StringBuilder codes = new StringBuilder();
        if (valueSet.hasExpansion() && valueSet.getExpansion().hasContains()) {
            for (ValueSet.ValueSetExpansionContainsComponent contains :
                    valueSet.getExpansion().getContains()) {
                String system = contains.getSystem();
                String code = contains.getCode();

                codes = getCodesStringBuilder(result, codes, system, code);
            }
        } else if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
            for (ValueSet.ConceptSetComponent concepts : valueSet.getCompose().getInclude()) {
                String system = concepts.getSystem();
                if (concepts.hasConcept()) {
                    for (ValueSet.ConceptReferenceComponent concept : concepts.getConcept()) {
                        String code = concept.getCode();

                        codes = getCodesStringBuilder(result, codes, system, code);
                    }
                }
            }
        }
        result.add(codes.toString());
        return result;
    }

    protected StringBuilder getCodesStringBuilder(List<String> list, StringBuilder codes, String system, String code) {
        String codeToken = system + "|" + code;
        int postAppendLength = codes.length() + codeToken.length();

        if (codes.length() > 0 && postAppendLength < maxUriLength) {
            codes.append(",");
        } else if (postAppendLength > maxUriLength) {
            list.add(codes.toString());
            codes = new StringBuilder();
        }
        codes.append(codeToken);
        return codes;
    }

    public List<String> createRequestUrl(DataRequirement dataRequirement) {
        if (!isPatientCompartment(dataRequirement.getType().toCode())) return List.of();
        String patientRelatedResource = dataRequirement.getType() + "?"
                + getPatientSearchParam(dataRequirement.getType().toCode())
                + "=Patient/" + PATIENT_ID_CONTEXT;
        List<String> ret = new ArrayList<>();
        if (dataRequirement.hasCodeFilter()) {
            for (DataRequirement.DataRequirementCodeFilterComponent codeFilterComponent :
                    dataRequirement.getCodeFilter()) {
                if (!codeFilterComponent.hasPath()) continue;
                String path =
                        mapCodePathToSearchParam(dataRequirement.getType().toCode(), codeFilterComponent.getPath());
                if (codeFilterComponent.hasValueSetElement()) {
                    for (String codes : resolveValueSetCodes(codeFilterComponent.getValueSetElement())) {
                        ret.add(patientRelatedResource + "&" + path + "=" + codes);
                    }
                } else if (codeFilterComponent.hasCode()) {
                    List<Coding> codeFilterValueCodings = codeFilterComponent.getCode();
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
            }
            return ret;
        } else {
            ret.add(patientRelatedResource);
            return ret;
        }
    }

    public PrefetchUrlList getPrefetchUrlList(PlanDefinition planDefinition) {
        PrefetchUrlList prefetchList = new PrefetchUrlList();
        if (planDefinition == null) return new PrefetchUrlList();
        if (!isEca(planDefinition)) return new PrefetchUrlList();
        Library library = resolvePrimaryLibrary(planDefinition);
        // TODO: resolve data requirements
        if (library == null || !library.hasDataRequirement()) {
            return new PrefetchUrlList();
        }
        for (DataRequirement dataRequirement : library.getDataRequirement()) {
            List<String> requestUrls = createRequestUrl(dataRequirement);
            if (requestUrls != null) {
                prefetchList.addAll(requestUrls);
            }
        }
        return prefetchList;
    }

    protected String mapCodePathToSearchParam(String dataType, String path) {
        switch (dataType) {
            case MEDICATION_ADMINISTRATION:
                if (path.equals(MEDICATION)) return "code";
                break;
            case MEDICATION_DISPENSE:
                if (path.equals(MEDICATION)) return "code";
                break;
            case MEDICATION_REQUEST:
                if (path.equals(MEDICATION)) return "code";
                break;
            case MEDICATION_STATEMENT:
                if (path.equals(MEDICATION)) return "code";
                break;
            default:
                if (path.equals("vaccineCode")) return "vaccine-code";
                break;
        }
        return path.replace('.', '-').toLowerCase();
    }

    public static boolean isPatientCompartment(String dataType) {
        if (dataType == null) {
            return false;
        }
        switch (dataType) {
            case "Account":
            case "AdverseEvent":
            case "AllergyIntolerance":
            case "Appointment":
            case "AppointmentResponse":
            case "AuditEvent":
            case "Basic":
            case "BodyStructure":
            case "CarePlan":
            case "CareTeam":
            case "ChargeItem":
            case "Claim":
            case "ClaimResponse":
            case "ClinicalImpression":
            case "Communication":
            case "CommunicationRequest":
            case "Composition":
            case "Condition":
            case "Consent":
            case "Coverage":
            case "CoverageEligibilityRequest":
            case "CoverageEligibilityResponse":
            case "DetectedIssue":
            case "DeviceRequest":
            case "DeviceUseStatement":
            case "DiagnosticReport":
            case "DocumentManifest":
            case "DocumentReference":
            case "Encounter":
            case "EnrollmentRequest":
            case "EpisodeOfCare":
            case "ExplanationOfBenefit":
            case "FamilyMemberHistory":
            case "Flag":
            case "Goal":
            case "Group":
            case "ImagingStudy":
            case "Immunization":
            case "ImmunizationEvaluation":
            case "ImmunizationRecommendation":
            case "Invoice":
            case "List":
            case "MeasureReport":
            case "Media":
            case MEDICATION_ADMINISTRATION:
            case MEDICATION_DISPENSE:
            case MEDICATION_REQUEST:
            case MEDICATION_STATEMENT:
            case "MolecularSequence":
            case "NutritionOrder":
            case "Observation":
            case "Patient":
            case "Person":
            case "Procedure":
            case "Provenance":
            case "QuestionnaireResponse":
            case "RelatedPerson":
            case "RequestGroup":
            case "ResearchSubject":
            case "RiskAssessment":
            case "Schedule":
            case "ServiceRequest":
            case "Specimen":
            case "SupplyDelivery":
            case "SupplyRequest":
            case "VisionPrescription":
                return true;
            default:
                return false;
        }
    }

    public String getPatientSearchParam(String dataType) {
        switch (dataType) {
            case "Account", "AdverseEvent":
                return SUBJECT;
            case "AllergyIntolerance":
                return PATIENT;
            case "Appointment","AppointmentResponse":
                return ACTOR;
            case "AuditEvent", "Basic", "BodyStructure", "CarePlan","CareTeam":
                return PATIENT;
            case "ChargeItem":
                return SUBJECT;
            case "Claim":
                return PATIENT;
            case "ClaimResponse":
                return PATIENT;
            case "ClinicalImpression":
                return SUBJECT;
            case "Communication":
                return SUBJECT;
            case "CommunicationRequest":
                return SUBJECT;
            case "Composition":
                return SUBJECT;
            case "Condition":
                return PATIENT;
            case "Consent":
                return PATIENT;
            case "Coverage":
                return "policy-holder";
            case "DetectedIssue":
                return PATIENT;
            case "DeviceRequest":
                return SUBJECT;
            case "DeviceUseStatement":
                return SUBJECT;
            case "DiagnosticReport":
                return SUBJECT;
            case "DocumentManifest":
                return SUBJECT;
            case "DocumentReference":
                return SUBJECT;
            case "Encounter":
                return PATIENT;
            case "EnrollmentRequest":
                return SUBJECT;
            case "EpisodeOfCare":
                return PATIENT;
            case "ExplanationOfBenefit":
                return PATIENT;
            case "FamilyMemberHistory":
                return PATIENT;
            case "Flag":
                return PATIENT;
            case "Goal":
                return PATIENT;
            case "Group":
                return "member";
            case "ImagingStudy":
                return PATIENT;
            case "Immunization":
                return PATIENT;
            case "ImmunizationRecommendation":
                return PATIENT;
            case "Invoice":
                return SUBJECT;
            case "List":
                return SUBJECT;
            case "MeasureReport":
                return PATIENT;
            case "Media":
                return SUBJECT;
            case MEDICATION_ADMINISTRATION:
                return PATIENT;
            case MEDICATION_DISPENSE:
                return PATIENT;
            case MEDICATION_REQUEST:
                return SUBJECT;
            case MEDICATION_STATEMENT:
                return SUBJECT;
            case "MolecularSequence":
                return PATIENT;
            case "NutritionOrder":
                return PATIENT;
            case "Observation":
                return SUBJECT;
            case "Patient":
                return "_id";
            case "Person":
                return PATIENT;
            case "Procedure":
                return PATIENT;
            case "Provenance":
                return PATIENT;
            case "QuestionnaireResponse":
                return SUBJECT;
            case "RelatedPerson":
                return PATIENT;
            case "RequestGroup":
                return SUBJECT;
            case "ResearchSubject":
                return "individual";
            case "RiskAssessment":
                return SUBJECT;
            case "Schedule":
                return ACTOR;
            case "ServiceRequest":
                return PATIENT;
            case "Specimen":
                return SUBJECT;
            case "SupplyDelivery":
                return PATIENT;
            case "SupplyRequest":
                return SUBJECT;
            case "VisionPrescription":
                return PATIENT;
        }

        return null;
    }
}
