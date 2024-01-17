package org.opencds.cqf.fhir.cr.helpers;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;

public class RequestHelpers {
    public static final String PATIENT_ID = "patientId";

    public static GenerateRequest newGenerateRequestForVersion(FhirVersionEnum fhirVersion) {
        return new GenerateRequest(fhirVersion);
    }

    public static GenerateRequest newGenerateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine) {
        return new GenerateRequest(
                false,
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
