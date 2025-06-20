package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.booleanTypeForVersion;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.forFhirVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper {

    /**
     * Returns a FHIR Parameters resource of the specified version containing the supplied parameters with the correct parameter name.
     * @param fhirVersion the FHIR version to create Parameters for
     * @param terminologyEndpoint the FHIR Endpoint resource to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param isPut the boolean value to determine if the Bundle returned uses PUT or POST request methods.
     * @return FHIR Parameters resource
     */
    public static IBaseParameters packageParameters(
            FhirVersionEnum fhirVersion, IBaseResource terminologyEndpoint, boolean isPut) {
        var params = forFhirVersion(fhirVersion)
                .createParameters((IBaseParameters) newBaseForVersion("Parameters", fhirVersion));
        if (terminologyEndpoint != null) {
            params.addParameter("terminologyEndpoint", terminologyEndpoint);
        }
        params.addParameter("isPut", booleanTypeForVersion(fhirVersion, isPut));
        return (IBaseParameters) params.get();
    }

    public static IBaseBackboneElement createEntry(IBaseResource resource, boolean isPut) {
        final var fhirVersion = resource.getStructureFhirVersionEnum();
        final var entry = BundleHelper.newEntryWithResource(resource);
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
        if (IKnowledgeArtifactAdapter.isSupportedMetadataResource(resource)) {
            final var adapter = IAdapterFactory.forFhirVersion(fhirVersion)
                    .createKnowledgeArtifactAdapter((IDomainResource) resource);
            if (adapter.hasUrl()) {
                final var url = adapter.getUrl();
                if (adapter.hasVersion()) {
                    BundleHelper.setEntryFullUrl(fhirVersion, entry, url + "|" + adapter.getVersion());
                    if (!isPut) {
                        BundleHelper.setRequestIfNoneExist(
                                fhirVersion, request, "url=%s&version=%s".formatted(url, adapter.getVersion()));
                    }
                } else {
                    BundleHelper.setEntryFullUrl(fhirVersion, entry, url);
                    if (!isPut) {
                        BundleHelper.setRequestIfNoneExist(fhirVersion, request, "url=%s".formatted(url));
                    }
                }
            }
        }
        return entry;
    }

    public static IBaseBackboneElement deleteEntry(IBaseResource resource) {
        final var fhirVersion = resource.getStructureFhirVersionEnum();
        final var entry = BundleHelper.newEntryWithResource(resource);
        var requestUrl = resource.fhirType() + "/" + resource.getIdElement().getIdPart();

        final var request = BundleHelper.newRequest(fhirVersion, "DELETE", requestUrl);
        BundleHelper.setEntryRequest(fhirVersion, entry, request);

        return entry;
    }
}
