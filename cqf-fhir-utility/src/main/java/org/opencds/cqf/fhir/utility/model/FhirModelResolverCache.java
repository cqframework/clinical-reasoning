package org.opencds.cqf.fhir.utility.model;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.EnumMap;
import java.util.Map;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R5FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;

public class FhirModelResolverCache {

    private FhirModelResolverCache() {
        // intentionally empty
    }

    private static Map<FhirVersionEnum, ModelResolver> cache = new EnumMap<>(FhirVersionEnum.class);

    public static ModelResolver resolverForVersion(FhirVersionEnum fhirVersionEnum) {
        requireNonNull(fhirVersionEnum, "fhirVersionEnum can not be null");
        if (!cache.containsKey(fhirVersionEnum)) {
            var resolver =
                    switch (fhirVersionEnum) {
                        case DSTU2 -> new DynamicModelResolver(new Dstu2FhirModelResolver());
                        case DSTU3 -> new DynamicModelResolver(new Dstu3FhirModelResolver());
                        case R4 -> new DynamicModelResolver(new R4FhirModelResolver());
                        case R5 -> new DynamicModelResolver(new R5FhirModelResolver());
                        default -> throw new IllegalArgumentException("unknown or unsupported FHIR version");
                    };

            cache.put(fhirVersionEnum, resolver);
        }

        return cache.get(fhirVersionEnum);
    }
}
