package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import java.util.Optional;

public interface ICdsCrDiscoveryServiceRegistry {
    void register(
            @Nonnull FhirVersionEnum fhirVersion, @Nonnull Class<? extends ICrDiscoveryService> iCrDiscoveryService);

    void unregister(@Nonnull FhirVersionEnum fhirVersion);

    Optional<Class<? extends ICrDiscoveryService>> find(@Nonnull FhirVersionEnum fhirVersion);
}
