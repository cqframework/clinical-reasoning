package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.repository.Repository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class CdsCrUtils {

    private CdsCrUtils() {
        // private constructor
    }

    public static IBaseResource readPlanDefinitionFromRepository(Repository repository, IIdType id) {
        return switch (repository.fhirContext().getVersion().getVersion()) {
            case DSTU3 -> repository.read(org.hl7.fhir.dstu3.model.PlanDefinition.class, id);
            case R4 -> repository.read(org.hl7.fhir.r4.model.PlanDefinition.class, id);
            case R5 -> repository.read(org.hl7.fhir.r5.model.PlanDefinition.class, id);
            default -> null;
        };
    }
}
