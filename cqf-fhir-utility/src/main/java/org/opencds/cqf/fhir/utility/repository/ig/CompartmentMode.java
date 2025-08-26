package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import java.util.Map;
import java.util.Set;
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

    // | Compartment Type | Resource Type | Candidate Search Params |
    // |------------------|---------------|-------------------------|
    // | Patient          | Encounter     | ["subject"]             |
    // | Patient          | Library       | []                      |
    private static Table<String, String, Set<RuntimeSearchParam>> compartmentMembershipCache =
            Tables.newCustomTable(Maps.newLinkedHashMap(), new Supplier<>() {
                public Map<String, Set<RuntimeSearchParam>> get() {
                    return Maps.newLinkedHashMap();
                }
            });

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

    private Set<RuntimeSearchParam> compartmentSearchParams(FhirContext fhirContext, String resourceType) {
        var compartmentMap = compartmentMembershipCache.row(this.type);
        if (compartmentMap.containsKey(resourceType)) {
            return compartmentMap.get(resourceType);
        }

        Set<RuntimeSearchParam> params;
        if (this.type.equals(resourceType)) {
            // If the compartment type is the same as the resource type, we can assume that the resource
            // belongs to its own compartment.
            params = fhirContext.getResourceDefinition(resourceType).getSearchParams().stream()
                    .filter(param -> param.getName().equals("_id"))
                    .collect(Collectors.toSet());
            if (params.isEmpty()) {
                // Should be impossible
                throw new IllegalStateException("No _id search parameter found for resource type: " + resourceType);
            }
        }
        else {
            params = fhirContext.getResourceDefinition(resourceType).getSearchParams().stream()
                .filter(param -> param.getParamType() == RestSearchParameterTypeEnum.REFERENCE)
                .filter(param -> param.getProvidesMembershipInCompartments() != null
                        && param.getProvidesMembershipInCompartments().contains(this.type)).collect(Collectors.toSet());
        }


        compartmentMembershipCache.put(this.type, resourceType, params);
        return params;
    }
}
