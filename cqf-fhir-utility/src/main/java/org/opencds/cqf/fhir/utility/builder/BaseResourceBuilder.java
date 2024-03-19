package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Resources;

public abstract class BaseResourceBuilder<SELF, T extends IBaseResource> {

    public static final String DEFAULT_IDENTIFIER_SYSTEM = "urn:ietf:rfc:3986";
    public static final String DEFAULT_IDENTIFIER_VALUE_PREFIX = "urn:uuid:";

    private final Class<T> resourceClass;

    private List<String> profile;

    private String id = UUID.randomUUID().toString();
    private Pair<String, String> identifier = new ImmutablePair<>(
            DEFAULT_IDENTIFIER_SYSTEM,
            DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString());

    protected BaseResourceBuilder(Class<T> resourceClass) {
        checkNotNull(resourceClass);
        this.resourceClass = resourceClass;
    }

    protected BaseResourceBuilder(Class<T> resourceClass, String id) {
        this(resourceClass);
        checkNotNull(id);

        this.id = id;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    @SuppressWarnings("null")
    public static String ensurePatientReference(String patientId) {
        if (Strings.isNullOrEmpty(patientId) || patientId.startsWith("Patient/")) {
            return patientId;
        }
        return "Patient/" + patientId;
    }

    @SuppressWarnings("null")
    public static String ensureOrganizationReference(String organizationId) {
        if (Strings.isNullOrEmpty(organizationId) || organizationId.startsWith("Organization/")) {
            return organizationId;
        }
        return "Organization/" + organizationId;
    }

    private void addProfile(String profile) {
        if (this.profile == null) {
            this.profile = new ArrayList<>();
        }

        this.profile.add(profile);
    }

    protected List<String> getProfiles() {
        if (profile == null) {
            return Collections.emptyList();
        }

        return profile;
    }

    protected String getId() {
        return id;
    }

    protected Pair<String, String> getIdentifier() {
        return identifier;
    }

    public SELF withId(String id) {
        checkNotNull(id);

        this.id = id;

        return self();
    }

    public SELF withProfile(String profile) {
        checkNotNull(profile);

        addProfile(profile);

        return self();
    }

    public SELF withIdentifier(Pair<String, String> identifier) {
        this.identifier = identifier;

        return self();
    }

    public T build() {
        T resource = Resources.newResource(resourceClass, id);

        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                initializeDstu3(resource);
                break;
            case R4:
                initializeR4(resource);
                break;
            case R5:
                initializeR5(resource);
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "ResourceBuilder.initializeResource does not support FHIR version %s",
                        resource.getStructureFhirVersionEnum().getFhirVersionString()));
        }

        return resource;
    }

    private void addProfiles(T resource) {
        getProfiles().forEach(p -> resource.getMeta().addProfile(p));
    }

    protected void initializeDstu3(T resource) {
        addProfiles(resource);
    }

    protected void initializeR4(T resource) {
        addProfiles(resource);
    }

    protected void initializeR5(T resource) {
        addProfiles(resource);
    }
}
