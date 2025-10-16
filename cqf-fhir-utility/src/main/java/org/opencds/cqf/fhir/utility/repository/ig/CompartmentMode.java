package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Whether or not the files are organized by compartment. This is primarily used for tests to
 * provide isolation between test cases.
 */
public enum CompartmentMode {
    PATIENT("Patient"),
    ENCOUNTER("Encounter"),
    RELATED_PERSON("RelatedPerson"),
    PRACTITIONER("Practitioner"),
    GROUP("Group"),
    NONE("");

    private static final ConcurrentMap<CacheKey, Set<RuntimeSearchParam>> COMPARTMENT_MEMBERSHIP_CACHE =
            new ConcurrentHashMap<>();

    private final String type;

    CompartmentMode(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static CompartmentMode fromType(String type) {
        for (CompartmentMode mode : values()) {
            if (mode.type.equalsIgnoreCase(type)) {
                return mode;
            }
        }
        return NONE;
    }

    public boolean resourceBelongsToCompartment(FhirContext fhirContext, String resourceType) {
        return !compartmentSearchParams(fhirContext, resourceType).isEmpty();
    }

    public Set<RuntimeSearchParam> compartmentSearchParams(FhirContext fhirContext, String resourceType) {
        var key = new CacheKey(this.type, fhirContext.getVersion().getVersion(), resourceType);
        return COMPARTMENT_MEMBERSHIP_CACHE.computeIfAbsent(key, k -> computeSearchParams(fhirContext, resourceType));
    }

    private Set<RuntimeSearchParam> computeSearchParams(FhirContext fhirContext, String resourceType) {
        Set<RuntimeSearchParam> params;
        if (this.type.equals(resourceType)) {
            params = fhirContext.getResourceDefinition(resourceType).getSearchParams().stream()
                    .filter(param -> param.getName().equals("_id"))
                    .collect(Collectors.toSet());
            if (params.isEmpty()) {
                throw new IllegalStateException("No _id search parameter found for resource type: " + resourceType);
            }
        } else {
            params = fhirContext.getResourceDefinition(resourceType).getSearchParams().stream()
                    .filter(param -> param.getParamType() == RestSearchParameterTypeEnum.REFERENCE)
                    .filter(param -> param.getProvidesMembershipInCompartments() != null
                            && param.getProvidesMembershipInCompartments().contains(this.type))
                    .collect(Collectors.toSet());
        }

        return params.isEmpty() ? Collections.emptySet() : Set.copyOf(params);
    }

    private record CacheKey(String compartmentType, FhirVersionEnum version, String resourceType) {}
}
