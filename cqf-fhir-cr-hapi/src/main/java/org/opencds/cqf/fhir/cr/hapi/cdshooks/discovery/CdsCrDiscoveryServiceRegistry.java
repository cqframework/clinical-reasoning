package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class CdsCrDiscoveryServiceRegistry implements ICdsCrDiscoveryServiceRegistry {
    private final Map<FhirVersionEnum, Class<? extends ICrDiscoveryService>> crDiscoveryServices;

    public CdsCrDiscoveryServiceRegistry() {
        new EnumMap<>(FhirVersionEnum.class);
        crDiscoveryServices = new EnumMap<>(FhirVersionEnum.class);
        crDiscoveryServices.put(FhirVersionEnum.DSTU3, CrDiscoveryServiceDstu3.class);
        crDiscoveryServices.put(FhirVersionEnum.R4, CrDiscoveryServiceR4.class);
        crDiscoveryServices.put(FhirVersionEnum.R5, CrDiscoveryServiceR5.class);
    }

    public void register(
            @Nonnull FhirVersionEnum fhirVersion, @Nonnull Class<? extends ICrDiscoveryService> crDiscoveryService) {
        crDiscoveryServices.put(fhirVersion, crDiscoveryService);
    }

    public void unregister(@Nonnull FhirVersionEnum fhirVersion) {
        crDiscoveryServices.remove(fhirVersion);
    }

    @Override
    public Optional<Class<? extends ICrDiscoveryService>> find(@Nonnull FhirVersionEnum fhirVersion) {
        return Optional.ofNullable(crDiscoveryServices.get(fhirVersion));
    }
}
