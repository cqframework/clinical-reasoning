package org.opencds.cqf.fhir.cr.questionnaire.helpers;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;

public class PopulateRequestHelpers {
    public static final String PATIENT_ID = "patientId";

    public static PopulateRequest newPopulateRequestForVersion(
            FhirVersionEnum fhirVersion, LibraryEngine libraryEngine, IBaseResource questionnaire) {
        return new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(PATIENT_ID, "Patient")),
                null,
                null,
                libraryEngine,
                FhirModelResolverCache.resolverForVersion(fhirVersion));
    }
}
