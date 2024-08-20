package org.opencds.cqf.fhir.cr.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class RequestHelpers {
    public static final String PATIENT_ID = "patientId";
    public static final String ENCOUNTER_ID = "encounterId";
    public static final String PRACTITIONER_ID = "practitionerId";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String PLANDEFINITION_ID = "planDefinitionId";
    public static final String PLANDEFINITION_URL = "http://test.fhir.org/fhir/PlanDefinition/";
    public static final String PROFILE_ID = "profileId";
    public static final String PROFILE_URL = "http://test.fhir.org/fhir/StructureDefinition/";

    public static ApplyRequest newPDApplyRequestForVersion(FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        return newPDApplyRequestForVersion(fhirVersion, libraryEngine, null, null);
    }

    public static ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, ModelResolver modelResolver) {
        return newPDApplyRequestForVersion(fhirVersion, libraryEngine, modelResolver, null);
    }

    public static ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            IInputParameterResolver inputParameterResolver) {
        var fhirContext = FhirContext.forCached(fhirVersion);
        IBaseResource planDefinition = null;
        try {
            planDefinition = fhirContext
                    .getResourceDefinition("PlanDefinition")
                    .newInstance()
                    .setId(PLANDEFINITION_ID);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return newPDApplyRequestForVersion(
                fhirVersion, planDefinition, libraryEngine, modelResolver, inputParameterResolver);
    }

    public static ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion,
            IBaseResource planDefinition,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            IInputParameterResolver inputParameterResolver) {
        try {
            if (modelResolver == null) {
                modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
            }
            var planDefinitionUrl = modelResolver.resolvePath(planDefinition, "url");
            if (planDefinitionUrl == null) {
                var url = PLANDEFINITION_URL + planDefinition.getIdElement().getIdPart();
                IBaseDatatype urlType;
                switch (fhirVersion) {
                    case DSTU3:
                        urlType = new org.hl7.fhir.dstu3.model.StringType(url);
                        break;
                    case R4:
                        urlType = new org.hl7.fhir.r4.model.CanonicalType(url);
                        break;
                    case R5:
                        urlType = new org.hl7.fhir.r5.model.CanonicalType(url);
                        break;

                    default:
                        urlType = null;
                        break;
                }
                modelResolver.setValue(planDefinition, "url", urlType);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        IBaseDatatype userLanguage;
        switch (fhirVersion) {
            case DSTU3:
                userLanguage = new org.hl7.fhir.dstu3.model.CodeableConcept(
                        new org.hl7.fhir.dstu3.model.Coding("test", "test", "test"));
                break;
            case R4:
                userLanguage = new org.hl7.fhir.r4.model.CodeableConcept(
                        new org.hl7.fhir.r4.model.Coding("test", "test", "test"));
                break;
            case R5:
                userLanguage = new org.hl7.fhir.r5.model.CodeableConcept(
                        new org.hl7.fhir.r5.model.Coding("test", "test", "test"));
                break;

            default:
                userLanguage = null;
                break;
        }
        return new ApplyRequest(
                planDefinition,
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
                true,
                null,
                libraryEngine,
                modelResolver,
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
            // TODO: handle exception
        }
        return newGenerateRequestForVersion(fhirVersion, libraryEngine, profile);
    }

    public static GenerateRequest newGenerateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource profile) {
        return new GenerateRequest(
                profile,
                false,
                true,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                true,
                null,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }

    public static PopulateRequest newPopulateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource questionnaire) {
        return newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire, "populate");
    }

    public static PopulateRequest newPopulateRequestForVersion(
            FhirVersionEnum fhirVersion,
            LibraryEngine libraryEngine,
            IBaseResource questionnaire,
            String operationName) {
        return new PopulateRequest(
                operationName,
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                true,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }
}
