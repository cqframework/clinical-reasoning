package org.opencds.cqf.fhir.cr.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;

public class RequestHelpers {
    public static final String PATIENT_ID = "patientId";
    public static final String ENCOUNTER_ID = "encounterId";
    public static final String PRACTITIONER_ID = "practitionerId";
    public static final String ORGANIZATION_ID = "organizationId";

    // public static ApplyRequest newPDApplyRequestForVersion(FhirVersionEnum fhirVersion) {
    //     return new ApplyRequest(fhirVersion);
    // }

    public static ApplyRequest newPDApplyRequestForVersion(FhirVersionEnum fhirVersion) {
        return newPDApplyRequestForVersion(fhirVersion, null, null);
    }

    public static ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IInputParameterResolver inputParameterResolver) {
        var fhirContext = FhirContext.forCached(fhirVersion);
        IBaseResource planDefinition = null;
        try {
            planDefinition = fhirContext.getResourceDefinition("PlanDefinition").newInstance();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return newPDApplyRequestForVersion(fhirVersion, planDefinition, libraryEngine, inputParameterResolver);
    }

    public static ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion,
            IBaseResource planDefinition,
            LibraryEngine libraryEngine,
            IInputParameterResolver inputParameterResolver) {
        ModelResolver modelResolver = null;
        try {
            modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return new ApplyRequest(
                planDefinition,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Encounter")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Practitioner")),
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Organization")),
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                libraryEngine,
                inputParameterResolver,
                modelResolver);
    }

    public static GenerateRequest newGenerateRequestForVersion(FhirVersionEnum fhirVersion) {
        return new GenerateRequest(fhirVersion);
    }

    public static GenerateRequest newGenerateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        return new GenerateRequest(
                false,
                true,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }

    public static PopulateRequest newPopulateRequestForVersion(FhirVersionEnum fhirVersion, String operationName) {
        return new PopulateRequest(fhirVersion, operationName);
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
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }
}
