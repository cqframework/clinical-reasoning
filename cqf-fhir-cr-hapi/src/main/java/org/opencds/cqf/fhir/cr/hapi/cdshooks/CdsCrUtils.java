package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

public class CdsCrUtils {

    private CdsCrUtils() {
        // private constructor
    }

    public static IBaseResource readPlanDefinitionFromRepository(
            FhirVersionEnum fhirVersion, Repository repository, IIdType id) {
        switch (fhirVersion) {
            case DSTU3:
                return repository.read(org.hl7.fhir.dstu3.model.PlanDefinition.class, id);
            case R4:
                return repository.read(org.hl7.fhir.r4.model.PlanDefinition.class, id);
            case R5:
                return repository.read(org.hl7.fhir.r5.model.PlanDefinition.class, id);
            default:
                return null;
        }
    }
}
