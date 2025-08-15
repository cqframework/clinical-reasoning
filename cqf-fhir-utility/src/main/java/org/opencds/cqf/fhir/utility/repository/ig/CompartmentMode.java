package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.Map;

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

    // | Compartment Type | Resource Type | Is Member? |
    // |------------------|---------------|------------|
    // | Patient          | Encounter     | true       |
    // | Patient          | Library       | false      |
    private static Table<String, String, Boolean> compartmentMembershipCache =
            Tables.newCustomTable(Maps.newLinkedHashMap(), new Supplier<>() {
                public Map<String, Boolean> get() {
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

    public boolean resourceBelongsToCompartment(FhirContext fhirContext, String resourceName) {
        if (this.type.equals(resourceName.toLowerCase())) {
            return true;
        }

        var resourceMap = compartmentMembershipCache.row(this.type);
        if (resourceMap.containsKey(resourceName)) {
            return resourceMap.get(resourceName);
        }

        var belongs = fhirContext.getResourceDefinition(resourceName).getSearchParams().stream()
                .filter(param -> param.getParamType() == RestSearchParameterTypeEnum.REFERENCE)
                .anyMatch(param -> param.getProvidesMembershipInCompartments() != null
                        && param.getProvidesMembershipInCompartments().contains(this.type));

        compartmentMembershipCache.put(this.type, resourceName, belongs);

        return belongs;
    }
}
