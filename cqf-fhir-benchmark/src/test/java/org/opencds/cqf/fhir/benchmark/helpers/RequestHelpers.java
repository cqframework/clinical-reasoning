package org.opencds.cqf.fhir.benchmark.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractRequest;
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

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        return newPDApplyRequestForVersion(fhirVersion, libraryEngine, null, null);
    }

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, ModelResolver modelResolver) {
        return newPDApplyRequestForVersion(fhirVersion, libraryEngine, modelResolver, null);
    }

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
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
            // Do nothing
        }
        return newPDApplyRequestForVersion(
                fhirVersion, planDefinition, libraryEngine, modelResolver, inputParameterResolver);
    }

    public static org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest newPDApplyRequestForVersion(
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
                IBaseDatatype urlType =
                        switch (fhirVersion) {
                            case DSTU3 -> new org.hl7.fhir.dstu3.model.StringType(url);
                            case R4 -> new org.hl7.fhir.r4.model.CanonicalType(url);
                            case R5 -> new org.hl7.fhir.r5.model.CanonicalType(url);
                            default -> null;
                        };
                modelResolver.setValue(planDefinition, "url", urlType);
            }
        } catch (Exception e) {
            // Do nothing
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
                null,
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
            // Do nothing
        }
        return newGenerateRequestForVersion(fhirVersion, profile, libraryEngine);
    }

    public static GenerateRequest newGenerateRequestForVersion(
            FhirVersionEnum fhirVersion, IBaseResource profile, LibraryEngine libraryEngine) {
        return new GenerateRequest(
                profile, false, true, libraryEngine, FhirModelResolverCache.resolverForVersion(fhirVersion));
    }

    public static PopulateRequest newPopulateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource questionnaire) {
        return new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                null,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }

    public static ExtractRequest newExtractRequestForVersion(
            FhirVersionEnum fhirVersion,
            LibraryEngine libraryEngine,
            IBaseResource questionnaireResponse,
            IBaseResource questionnaire) {
        return new ExtractRequest(
                questionnaireResponse,
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion),
                null);
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
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }
}
