package org.opencds.cqf.fhir.benchmark.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractRequest;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;

public class RequestHelpers {
    public static final String PATIENT_ID = "patientId";
    public static final String ENCOUNTER_ID = "encounterId";
    public static final String PRACTITIONER_ID = "practitionerId";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String PLANDEFINITION_ID = "planDefinitionId";
    public static final String PLANDEFINITION_URL = "http://test.fhir.org/fhir/PlanDefinition/";
    public static final String PROFILE_ID = "profileId";
    public static final String PROFILE_URL = "http://test.fhir.org/fhir/StructureDefinition/";

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        return newPDApplyRequestForVersion(fhirVersion, libraryEngine, null);
    }

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IInputParameterResolver inputParameterResolver) {
        var fhirContext = FhirContext.forCached(fhirVersion);
        IBaseResource planDefinition = null;
        try {
            planDefinition = fhirContext
                    .getResourceDefinition("PlanDefinition")
                    .newInstance()
                    .setId(PLANDEFINITION_ID);
        } catch (Exception e) {
            // Do nothing
        }
        return newPDApplyRequestForVersion(fhirVersion, planDefinition, libraryEngine, inputParameterResolver);
    }

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion,
            IBaseResource planDefinition,
            LibraryEngine libraryEngine,
            IInputParameterResolver inputParameterResolver) {
        var planDefinitionAdapter = (IPlanDefinitionAdapter) IAdapterFactory.createAdapterForResource(planDefinition);
        if (!planDefinitionAdapter.hasUrl()) {
            var url = PLANDEFINITION_URL + planDefinitionAdapter.getId();
            planDefinitionAdapter.setUrl(url);
        }
        IBaseDatatype userLanguage =
                switch (fhirVersion) {
                    case DSTU3 ->
                        new org.hl7.fhir.dstu3.model.CodeableConcept(
                                new org.hl7.fhir.dstu3.model.Coding("test", "test", "test"));
                    case R4 ->
                        new org.hl7.fhir.r4.model.CodeableConcept(
                                new org.hl7.fhir.r4.model.Coding("test", "test", "test"));
                    case R5 ->
                        new org.hl7.fhir.r5.model.CodeableConcept(
                                new org.hl7.fhir.r5.model.Coding("test", "test", "test"));
                    default -> null;
                };
        return new org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest(
                planDefinitionAdapter.get(),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Encounter")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Practitioner")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Organization")),
                null,
                userLanguage,
                null,
                null,
                null,
                null,
                null,
                null,
                libraryEngine,
                inputParameterResolver);
    }

    public static GenerateRequest newGenerateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        IBaseResource profile = null;
        try {
            var fhirContext = FhirContext.forCached(fhirVersion);
            profile = fhirContext
                    .getResourceDefinition("StructureDefinition")
                    .newInstance()
                    .setId(PROFILE_ID);
        } catch (Exception e) {
            // Do nothing
        }
        return newGenerateRequestForVersion(profile, libraryEngine);
    }

    public static GenerateRequest newGenerateRequestForVersion(IBaseResource profile, LibraryEngine libraryEngine) {
        return new GenerateRequest(profile, false, true, libraryEngine);
    }

    public static PopulateRequest newPopulateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource questionnaire) {
        return new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                null,
                libraryEngine);
    }

    public static ExtractRequest newExtractRequestForVersion(
            FhirVersionEnum fhirVersion,
            LibraryEngine libraryEngine,
            IBaseResource questionnaireResponse,
            IBaseResource questionnaire) {
        return new ExtractRequest(questionnaireResponse, questionnaire, null, null, libraryEngine, null);
    }

    public static EvaluateRequest newEvaluateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource library) {
        return new EvaluateRequest(
                library,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                null,
                null,
                libraryEngine);
    }
}
