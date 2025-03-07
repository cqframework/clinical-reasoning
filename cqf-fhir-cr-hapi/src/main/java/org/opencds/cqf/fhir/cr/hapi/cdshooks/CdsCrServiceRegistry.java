package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class CdsCrServiceRegistry implements ICdsCrServiceRegistry {
    private final Map<FhirVersionEnum, Class<? extends ICdsCrService>> cdsCrServices;

    public CdsCrServiceRegistry() {
        cdsCrServices = new EnumMap<>(FhirVersionEnum.class);
        cdsCrServices.put(FhirVersionEnum.DSTU3, CdsCrServiceDstu3.class);
        cdsCrServices.put(FhirVersionEnum.R4, CdsCrServiceR4.class);
        cdsCrServices.put(FhirVersionEnum.R5, CdsCrServiceR5.class);
    }

    public void register(@Nonnull FhirVersionEnum fhirVersion, @Nonnull Class<? extends ICdsCrService> cdsCrService) {
        cdsCrServices.put(fhirVersion, cdsCrService);
    }

    public void unregister(@Nonnull FhirVersionEnum fhirVersion) {
        cdsCrServices.remove(fhirVersion);
    }

    public Optional<Class<? extends ICdsCrService>> find(@Nonnull FhirVersionEnum fhirVersion) {
        return Optional.ofNullable(cdsCrServices.get(fhirVersion));
    }
}
