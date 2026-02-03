package org.opencds.cqf.fhir.utility.client.terminology;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public interface ITerminologyServerClient extends ITerminologyProvider {

    String versionParamName = "valueSetVersion";
    String urlParamName = "url";

    // Strips resource and id from the endpoint address URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    static String getAddressBase(String address, FhirContext ctx) {
        requireNonNull(address, "address must not be null");
        if (address.startsWith("http://")) {
            address = address.replaceFirst("http://", "https://");
        }
        // remove trailing slashes
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        // check if URL is in the format [base URL]/[resource type]/[id]
        var maybeFhirType = Canonicals.getResourceType(address);
        if (StringUtils.isNotBlank(maybeFhirType)) {
            IBaseEnumFactory<?> factory = getEnumFactory(ctx);
            try {
                factory.fromCode(maybeFhirType);
            } catch (IllegalArgumentException e) {
                // check if URL is in the format [base URL]/[resource type]
                var lastSlashIndex = address.lastIndexOf("/");
                if (lastSlashIndex > 0) {
                    maybeFhirType = address.substring(lastSlashIndex + 1);
                    try {
                        factory.fromCode(maybeFhirType);
                    } catch (IllegalArgumentException e2) {
                        return address;
                    }
                } else {
                    return address;
                }
            }
            address = address.substring(0, address.indexOf(maybeFhirType) - 1);
        }
        return address;
    }

    static IBaseEnumFactory<?> getEnumFactory(FhirContext ctx) {
        return switch (ctx.getVersion().getVersion()) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Enumerations.ResourceTypeEnumFactory();
            case R4 -> new org.hl7.fhir.r4.model.Enumerations.ResourceTypeEnumFactory();
            case R5 -> new org.hl7.fhir.r5.model.Enumerations.ResourceTypeEnumEnumFactory();
            default -> throw new UnprocessableEntityException(
                    "unsupported FHIR version: " + ctx.getVersion().getVersion().toString());
        };
    }

    TerminologyServerClientSettings getTerminologyServerClientSettings();

    IGenericClient initializeClientWithAuth(IEndpointAdapter endpoint);

    /**
     * Checks if the endpoint base URL matches the Terminology Server URL
     * @param address expected ValueSet url
     * @return default to false, and expect server specific clients, e.g. VSAC, to override
     */
    default boolean isCanonicalMatch(String address) {
        return false;
    }
}
