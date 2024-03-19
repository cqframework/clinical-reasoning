package org.opencds.cqf.fhir.utility;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper {

    public static IBaseBackboneElement createEntry(IBaseResource resource, boolean isPut) {
        final var fhirVersion = resource.getStructureFhirVersionEnum();
        final var entry = BundleHelper.newEntryWithResource(resource.getStructureFhirVersionEnum(), resource);
        String method;
        var requestUrl = resource.fhirType();
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
        if (KnowledgeArtifactAdapter.isSupportedMetadataResource(resource)) {
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
        }
        return entry;
    }
}
