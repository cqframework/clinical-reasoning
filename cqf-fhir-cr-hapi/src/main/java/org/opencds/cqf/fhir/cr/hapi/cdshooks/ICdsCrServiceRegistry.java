package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import java.util.Optional;

public interface ICdsCrServiceRegistry {
    void register(@Nonnull FhirVersionEnum fhirVersion, @Nonnull Class<? extends ICdsCrService> cdsCrService);

    void unregister(@Nonnull FhirVersionEnum fhirVersion);

    Optional<Class<? extends ICdsCrService>> find(@Nonnull FhirVersionEnum fhirVersion);
}
