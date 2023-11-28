package org.opencds.cqf.fhir.cr.inputparameters;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

public class InputParameterResolverFactory {

    @SuppressWarnings("unchecked")
    public static <T extends IInputParameterResolver> T create(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle bundle) {
        var fhirVersion = repository.fhirContext().getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.dstu3.InputParameterResolver(
                        repository, subjectId, encounterId, practitionerId, parameters, useServerData, bundle);
            case R4:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.r4.InputParameterResolver(
                        repository, subjectId, encounterId, practitionerId, parameters, useServerData, bundle);
            case R5:
                return (T) new org.opencds.cqf.fhir.cr.inputparameters.r5.InputParameterResolver(
                        repository, subjectId, encounterId, practitionerId, parameters, useServerData, bundle);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported FHIR version for parameter resolution: %s", fhirVersion));
        }
    }
}
