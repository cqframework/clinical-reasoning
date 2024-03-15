package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper {

    public static IBaseBackboneElement createEntry(IBaseResource resource, boolean isPut) {
        final var fhirVersion = resource.getStructureFhirVersionEnum();
        final var entry = BundleHelper.newEntryWithResource(resource.getStructureFhirVersionEnum(), resource);
        String method;
        var requestUrl = getResourceType(resource);
        if (isPut) {
            method = "PUT";
            if (resource.getIdElement() != null
                    && !StringUtils.isBlank(resource.getIdElement().getIdPart())) {
                requestUrl += "/" + resource.getIdElement().getIdPart();
            }
        } else {
            method = "POST";
        }
        final var request = BundleHelper.newRequest(fhirVersion, method, requestUrl);
        BundleHelper.setEntryRequest(fhirVersion, entry, request);
        try {
            final var adapter = AdapterFactory.forFhirVersion(fhirVersion)
                    .createKnowledgeArtifactAdapter((IDomainResource) resource);
            if (adapter.hasUrl()) {
                final var url = adapter.getUrl();
                if (adapter.hasVersion()) {
                    BundleHelper.setEntryFullUrl(fhirVersion, entry, url + "|" + adapter.getVersion());
                    if (!isPut) {
                        BundleHelper.setRequestIfNoneExist(
                                fhirVersion, request, String.format("url=%s&version=%s", url, adapter.getVersion()));
                    }
                } else {
                    BundleHelper.setEntryFullUrl(fhirVersion, entry, url);
                    if (!isPut) {
                        BundleHelper.setRequestIfNoneExist(fhirVersion, request, String.format("url=%s", url));
                    }
                }
            }
        } catch (UnprocessableEntityException e) {
            // ignore fullURL for non-canonical resources
        }
        return entry;
    }

    private static String getResourceType(IBaseResource resource) {
        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Resource) resource)
                        .getResourceType()
                        .toString();
            case R4:
                return ((org.hl7.fhir.r4.model.Resource) resource)
                        .getResourceType()
                        .toString();
            case R5:
                return ((org.hl7.fhir.r5.model.Resource) resource)
                        .getResourceType()
                        .toString();
            default:
                throw new UnprocessableEntityException(String.format(
                        "Unsupported version of FHIR: %s",
                        resource.getStructureFhirVersionEnum().getFhirVersionString()));
        }
    }
}
