package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class that represents the compartment context for a given request within {@link IgRepository} only.
 */
public class IgRepositoryCompartment {

    // | Resource Type | Compartment Type | Is Member? |
    // |---------------|------------------|-----------|
    // | Encounter     | Patient          | true      |
    // | Library       | Patient          | false     |
    private static Table<String, String, Boolean> compartmentMembershipCache =
            Tables.newCustomTable(Maps.newLinkedHashMap(), new Supplier<>() {
                public Map<String, Boolean> get() {
                    return Maps.newLinkedHashMap();
                }
            });

    private final String type;
    private final String id;

    private static String typeOfContext(String context) {
        return context.split("/")[0];
    }

    private static String idOfContext(String context) {
        return context.split("/")[1];
    }

    // Empty context (i.e. no compartment context)
    public IgRepositoryCompartment() {
        this.type = null;
        this.id = null;
    }

    // Context in the format ResourceType/Id
    public IgRepositoryCompartment(String context) {
        this(typeOfContext(context), idOfContext(context));
    }

    // Context in the format type and id
    public IgRepositoryCompartment(String type, String id) {
        // Make this lowercase so the path will resolve on Linux (FYI: macOS is case-insensitive)
        this.type = requireNonNullOrEmpty("type", type);
        this.id = requireNonNullOrEmpty("id", id);
    }

    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public boolean isEmpty() {
        return this.type == null || this.id == null;
    }

    private static String requireNonNullOrEmpty(String name, String value) {
        requireNonNull(name, "name cannot be null");
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IgRepositoryCompartment that = (IgRepositoryCompartment) o;
        return Objects.equals(type, that.type) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IgRepositoryCompartment.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("id='" + id + "'")
                .toString();
    }

    public boolean resourceBelongsToCompartment(FhirContext fhirContext, String resourceName) {
        if (this.type.equals(resourceName.toLowerCase())) {
            return true;
        }

        var resourceMap = compartmentMembershipCache.column(resourceName);
        if (resourceMap.containsKey(this.type)) {
            return resourceMap.get(this.type);
        }

        var belongs = fhirContext.getResourceDefinition(resourceName).getSearchParams().stream()
                .filter(param -> param.getParamType() == RestSearchParameterTypeEnum.REFERENCE)
                .anyMatch(param -> param.getProvidesMembershipInCompartments() != null
                        && param.getProvidesMembershipInCompartments().contains(this.type));

        compartmentMembershipCache.put(this.type, resourceName, belongs);

        return belongs;
    }
}
