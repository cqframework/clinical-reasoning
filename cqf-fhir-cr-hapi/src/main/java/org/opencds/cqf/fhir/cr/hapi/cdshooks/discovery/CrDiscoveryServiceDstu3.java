package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrUtils;
import org.opencds.cqf.fhir.utility.dstu3.SearchHelper;

public class CrDiscoveryServiceDstu3 implements ICrDiscoveryService {

    protected static final String PATIENT_ID_CONTEXT = "{{context.patientId}}";
    protected static final int DEFAULT_MAX_URI_LENGTH = 8000;
    protected int maxUriLength;

    protected Repository repository;
    protected final IIdType planDefinitionId;

    public CrDiscoveryServiceDstu3(IIdType planDefinitionId, Repository repository) {
        this.planDefinitionId = planDefinitionId;
        this.repository = repository;
        this.maxUriLength = DEFAULT_MAX_URI_LENGTH;
    }

    public CdsServiceJson resolveService() {
        return resolveService(
                CdsCrUtils.readPlanDefinitionFromRepository(FhirVersionEnum.DSTU3, repository, planDefinitionId));
    }

    protected CdsServiceJson resolveService(IBaseResource planDefinition) {
        if (planDefinition instanceof PlanDefinition) {
            PlanDefinition planDef = (PlanDefinition) planDefinition;
            return new CrDiscoveryElementDstu3(planDef, getPrefetchUrlList(planDef)).getCdsServiceJson();
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
        // Assuming 1 library
        // TODO: enhance to handle multiple libraries - need a way to identify primary
        // library
        Library library = null;
        if (planDefinition.hasLibrary() && planDefinition.getLibraryFirstRep().hasReference()) {
            library = repository.read(
                    Library.class, planDefinition.getLibraryFirstRep().getReferenceElement());
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

    public List<String> resolveValueSetCodes(StringType valueSetId) {
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
        if (!isPatientCompartment(dataRequirement.getType())) return null;
        String patientRelatedResource = dataRequirement.getType() + "?"
                + getPatientSearchParam(dataRequirement.getType())
                + "=Patient/" + PATIENT_ID_CONTEXT;
        List<String> ret = new ArrayList<>();
        if (dataRequirement.hasCodeFilter()) {
            for (DataRequirement.DataRequirementCodeFilterComponent codeFilterComponent :
                    dataRequirement.getCodeFilter()) {
                if (!codeFilterComponent.hasPath()) continue;
                String path = mapCodePathToSearchParam(dataRequirement.getType(), codeFilterComponent.getPath());

                StringType codeFilterComponentString = null;
                if (codeFilterComponent.hasValueSetStringType()) {
                    codeFilterComponentString = codeFilterComponent.getValueSetStringType();
                } else if (codeFilterComponent.hasValueSetReference()) {
                    codeFilterComponentString = new StringType(
                            codeFilterComponent.getValueSetReference().getReference());
                } else if (codeFilterComponent.hasValueCoding()) {
                    List<Coding> codeFilterValueCodings = codeFilterComponent.getValueCoding();
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

                if (codeFilterComponentString != null) {
                    for (String codes : resolveValueSetCodes(codeFilterComponentString)) {
                        ret.add(patientRelatedResource + "&" + path + "=" + codes);
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
        if (planDefinition == null) return null;
        if (!isEca(planDefinition)) return null;
        Library library = resolvePrimaryLibrary(planDefinition);
        // TODO: resolve data requirements
        if (!library.hasDataRequirement()) return null;
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
            case "MedicationAdministration":
                if (path.equals("medication")) return "code";
                break;
            case "MedicationDispense":
                if (path.equals("medication")) return "code";
                break;
            case "MedicationRequest":
                if (path.equals("medication")) return "code";
                break;
            case "MedicationStatement":
                if (path.equals("medication")) return "code";
                break;
            case "ProcedureRequest":
                if (path.equals("bodySite")) return "body-site";
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
            case "BodySite":
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
            case "DetectedIssue":
            case "DeviceRequest":
            case "DeviceUseStatement":
            case "DiagnosticReport":
            case "DocumentManifest":
            case "EligibilityRequest":
            case "Encounter":
            case "EnrollmentRequest":
            case "EpisodeOfCare":
            case "ExplanationOfBenefit":
            case "FamilyMemberHistory":
            case "Flag":
            case "Goal":
            case "Group":
            case "ImagingManifest":
            case "ImagingStudy":
            case "Immunization":
            case "ImmunizationRecommendation":
            case "List":
            case "MeasureReport":
            case "Media":
            case "MedicationAdministration":
            case "MedicationDispense":
            case "MedicationRequest":
            case "MedicationStatement":
            case "NutritionOrder":
            case "Observation":
            case "Patient":
            case "Person":
            case "Procedure":
            case "ProcedureRequest":
            case "Provenance":
            case "QuestionnaireResponse":
            case "ReferralRequest":
            case "RelatedPerson":
            case "RequestGroup":
            case "ResearchSubject":
            case "RiskAssessment":
            case "Schedule":
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
            case "Account":
                return "subject";
            case "AdverseEvent":
                return "subject";
            case "AllergyIntolerance":
                return "patient";
            case "Appointment":
                return "actor";
            case "AppointmentResponse":
                return "actor";
            case "AuditEvent":
                return "patient";
            case "Basic":
                return "patient";
            case "BodySite":
                return "patient";
            case "CarePlan":
                return "patient";
            case "CareTeam":
                return "patient";
            case "ChargeItem":
                return "subject";
            case "Claim":
                return "patient";
            case "ClaimResponse":
                return "patient";
            case "ClinicalImpression":
                return "subject";
            case "Communication":
                return "subject";
            case "CommunicationRequest":
                return "subject";
            case "Composition":
                return "subject";
            case "Condition":
                return "patient";
            case "Consent":
                return "patient";
            case "Coverage":
                return "patient";
            case "DetectedIssue":
                return "patient";
            case "DeviceRequest":
                return "subject";
            case "DeviceUseStatement":
                return "subject";
            case "DiagnosticReport":
                return "subject";
            case "DocumentManifest":
                return "subject";
            case "DocumentReference":
                return "subject";
            case "EligibilityRequest":
                return "patient";
            case "Encounter":
                return "patient";
            case "EnrollmentRequest":
                return "subject";
            case "EpisodeOfCare":
                return "patient";
            case "ExplanationOfBenefit":
                return "patient";
            case "FamilyMemberHistory":
                return "patient";
            case "Flag":
                return "patient";
            case "Goal":
                return "patient";
            case "Group":
                return "member";
            case "ImagingManifest":
                return "patient";
            case "ImagingStudy":
                return "patient";
            case "Immunization":
                return "patient";
            case "ImmunizationRecommendation":
                return "patient";
            case "List":
                return "subject";
            case "MeasureReport":
                return "patient";
            case "Media":
                return "subject";
            case "MedicationAdministration":
                return "patient";
            case "MedicationDispense":
                return "patient";
            case "MedicationRequest":
                return "subject";
            case "MedicationStatement":
                return "subject";
            case "NutritionOrder":
                return "patient";
            case "Observation":
                return "subject";
            case "Patient":
                return "_id";
            case "Person":
                return "patient";
            case "Procedure":
                return "patient";
            case "ProcedureRequest":
                return "patient";
            case "Provenance":
                return "patient";
            case "QuestionnaireResponse":
                return "subject";
            case "ReferralRequest":
                return "patient";
            case "RelatedPerson":
                return "patient";
            case "RequestGroup":
                return "subject";
            case "ResearchSubject":
                return "individual";
            case "RiskAssessment":
                return "subject";
            case "Schedule":
                return "actor";
            case "Specimen":
                return "subject";
            case "SupplyDelivery":
                return "patient";
            case "SupplyRequest":
                return "subject";
            case "VisionPrescription":
                return "patient";
        }

        return null;
    }
}
