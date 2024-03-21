package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class BundleHelper {
    private BundleHelper() {}

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return
     */
    public static IBaseBackboneElement getEntryFirstRep(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntryFirstRep();
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle) bundle).getEntryFirstRep();
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle) bundle).getEntryFirstRep();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return
     */
    public static IBaseResource getEntryResourceFirstRep(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Entry = ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntryFirstRep();
                return dstu3Entry != null && dstu3Entry.hasResource() ? dstu3Entry.getResource() : null;
            case R4:
                var r4Entry = ((org.hl7.fhir.r4.model.Bundle) bundle).getEntryFirstRep();
                return r4Entry != null && r4Entry.hasResource() ? r4Entry.getResource() : null;
            case R5:
                var r5Entry = ((org.hl7.fhir.r5.model.Bundle) bundle).getEntryFirstRep();
                return r5Entry != null && r5Entry.hasResource() ? r5Entry.getResource() : null;

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a list of resources from the Bundle entries
     *
     * @param bundle IBaseBundle type
     * @return
     */
    public static List<IBaseResource> getEntryResources(IBaseBundle bundle) {
        List<IBaseResource> resources = new ArrayList<>();
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Entry = ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntry();
                for (var entry : dstu3Entry) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
                break;
            case R4:
                var r4Entry = ((org.hl7.fhir.r4.model.Bundle) bundle).getEntry();
                for (var entry : r4Entry) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
                break;
            case R5:
                var r5Entry = ((org.hl7.fhir.r5.model.Bundle) bundle).getEntry();
                for (var entry : r5Entry) {
                    if (entry.hasResource()) {
                        resources.add(entry.getResource());
                    }
                }
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }

        return resources;
    }

    /**
     * Returns the Resource for a given entry
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return
     */
    public static IBaseResource getEntryResource(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry).getResource();
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry).getResource();
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getResource();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Checks if an entry has a request type of PUT
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return
     */
    public static boolean isEntryRequestPut(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        switch (fhirVersion) {
            case DSTU3:
                return Optional.ofNullable(((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT)
                        .isPresent();
            case R4:
                return Optional.ofNullable(((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT)
                        .isPresent();
            case R5:
                return Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.r5.model.Bundle.HTTPVerb.PUT)
                        .isPresent();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Checks if an entry has a request type of POST
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return
     */
    public static boolean isEntryRequestPost(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        switch (fhirVersion) {
            case DSTU3:
                return Optional.ofNullable(((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.POST)
                        .isPresent();
            case R4:
                return Optional.ofNullable(((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST)
                        .isPresent();
            case R5:
                return Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(r -> r.getMethod())
                        .filter(r -> r == org.hl7.fhir.r5.model.Bundle.HTTPVerb.POST)
                        .isPresent();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns the list of entries from the Bundle
     *
     * @param bundle IBaseBundle type
     * @return
     */
    public static List<? extends IBaseBackboneElement> getEntry(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntry();
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle) bundle).getEntry();
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle) bundle).getEntry();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Sets the list of entries of the Bundle
     *
     * @param bundle IBaseBundle type
     * @param entries List of IBaseBackboneElement type
     */
    @SuppressWarnings("unchecked")
    public static void setEntry(IBaseBundle bundle, List<? extends IBaseBackboneElement> entries) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                ((org.hl7.fhir.dstu3.model.Bundle) bundle)
                        .setEntry((List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent>) entries);
                break;
            case R4:
                ((org.hl7.fhir.r4.model.Bundle) bundle)
                        .setEntry((List<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent>) entries);
                break;
            case R5:
                ((org.hl7.fhir.r5.model.Bundle) bundle)
                        .setEntry((List<org.hl7.fhir.r5.model.Bundle.BundleEntryComponent>) entries);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Adds the entry to the Bundle and returns the Bundle
     *
     * @param bundle IBaseBundle type
     * @param entry IBaseBackboneElement type
     * @return
     */
    public static IBaseBundle addEntry(IBaseBundle bundle, IBaseBackboneElement entry) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                ((org.hl7.fhir.dstu3.model.Bundle) bundle)
                        .addEntry((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry);
                break;
            case R4:
                ((org.hl7.fhir.r4.model.Bundle) bundle)
                        .addEntry((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry);
                break;
            case R5:
                ((org.hl7.fhir.r5.model.Bundle) bundle)
                        .addEntry((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }

        return bundle;
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @return
     */
    public static IBaseBundle newBundle(FhirVersionEnum fhirVersion) {
        return newBundle(fhirVersion, null, null);
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @param id Id to set on the Bundle, will ignore if null
     * @param type The type of Bundle to return, defaults to COLLECTION
     * @return
     */
    public static IBaseBundle newBundle(FhirVersionEnum fhirVersion, String id, String type) {
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Bundle = new org.hl7.fhir.dstu3.model.Bundle();
                if (id != null && !id.isEmpty()) {
                    dstu3Bundle.setId(id);
                }
                dstu3Bundle.setType(
                        type == null || type.isEmpty()
                                ? org.hl7.fhir.dstu3.model.Bundle.BundleType.COLLECTION
                                : org.hl7.fhir.dstu3.model.Bundle.BundleType.fromCode(type));
                return dstu3Bundle;
            case R4:
                var r4Bundle = new org.hl7.fhir.r4.model.Bundle();
                if (id != null && !id.isEmpty()) {
                    r4Bundle.setId(id);
                }
                r4Bundle.setType(
                        type == null || type.isEmpty()
                                ? org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION
                                : org.hl7.fhir.r4.model.Bundle.BundleType.fromCode(type));
                return r4Bundle;
            case R5:
                var r5Bundle = new org.hl7.fhir.r5.model.Bundle();
                if (id != null && !id.isEmpty()) {
                    r5Bundle.setId(id);
                }
                r5Bundle.setType(
                        type == null || type.isEmpty()
                                ? org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION
                                : org.hl7.fhir.r5.model.Bundle.BundleType.fromCode(type));
                return r5Bundle;

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a new entry element with the Resource
     * @param fhirVersion
     * @param resource
     * @return
     */
    public static IBaseBackboneElement newEntryWithResource(FhirVersionEnum fhirVersion, IBaseResource resource) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                        .setResource((org.hl7.fhir.dstu3.model.Resource) resource);
            case R4:
                return new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                        .setResource((org.hl7.fhir.r4.model.Resource) resource);
            case R5:
                return new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setResource((org.hl7.fhir.r5.model.Resource) resource);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a new entry element with the Response
     * @param fhirVersion
     * @param response
     * @return
     */
    public static IBaseBackboneElement newEntryWithResponse(
            FhirVersionEnum fhirVersion, IBaseBackboneElement response) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                        .setResponse((org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent) response);
            case R4:
                return new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                        .setResponse((org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent) response);
            case R5:
                return new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setResponse((org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent) response);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a new BundleEntryResponse element with the location
     * @param fhirVersion
     * @param location
     * @return
     */
    public static IBaseBackboneElement newResponseWithLocation(FhirVersionEnum fhirVersion, String location) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent().setLocation(location);
            case R4:
                return new org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent().setLocation(location);
            case R5:
                return new org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent().setLocation(location);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a new BundleEntryRequest element with the method and url
     * @param fhirVersion
     * @param method
     * @param url
     * @return
     */
    public static IBaseBackboneElement newRequest(FhirVersionEnum fhirVersion, String method, String url) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);
            case R4:
                return new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);
            case R5:
                return new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Returns a new BundleEntryRequest element with the method
     * @param fhirVersion
     * @param method
     * @return
     */
    public static IBaseBackboneElement newRequest(FhirVersionEnum fhirVersion, String method) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.fromCode(method));
            case R4:
                return new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method));
            case R5:
                return new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method));

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Sets the BundleEntryRequest url
     * @param fhirVersion
     * @param request
     * @param url
     * @return
     */
    public static IBaseBackboneElement setRequestUrl(
            FhirVersionEnum fhirVersion, IBaseBackboneElement request, String url) {
        if (request == null) {
            request = newRequest(fhirVersion, null);
        }
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent) request).setUrl(url);
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent) request).setUrl(url);
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request).setUrl(url);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Sets the BundleEntryRequest ifNoneExist property
     * @param fhirVersion
     * @param request
     * @param ifNoneExist
     * @return
     */
    public static IBaseBackboneElement setRequestIfNoneExist(
            FhirVersionEnum fhirVersion, IBaseBackboneElement request, String ifNoneExist) {
        if (request == null) {
            request = newRequest(fhirVersion, null);
        }
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent) request)
                        .setIfNoneExist(ifNoneExist);
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent) request).setIfNoneExist(ifNoneExist);
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request).setIfNoneExist(ifNoneExist);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Sets the BundleEntry fullUrl property
     * @param fhirVersion
     * @param entry
     * @param fullUrl
     * @return
     */
    public static IBaseBackboneElement setEntryFullUrl(
            FhirVersionEnum fhirVersion, IBaseBackboneElement entry, String fullUrl) {
        if (entry == null) {
            entry = newEntryWithResource(fhirVersion, null);
        }
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry).setFullUrl(fullUrl);
            case R4:
                return ((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry).setFullUrl(fullUrl);
            case R5:
                return ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).setFullUrl(fullUrl);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Sets the BundleEntry request property
     * @param fhirVersion
     * @param entry
     * @param request
     * @return
     */
    public static IBaseBackboneElement setEntryRequest(
            FhirVersionEnum fhirVersion, IBaseBackboneElement entry, IBaseBackboneElement request) {
        if (entry == null) {
            entry = newEntryWithResource(fhirVersion, null);
        }
        if (request == null) {
            request = newRequest(fhirVersion, null);
        }
        switch (fhirVersion) {
            case DSTU3:
                if (request != null
                        && !(request instanceof org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(String.format(
                            "Request should be of type: %s",
                            "org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent"));
                }
                return ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entry)
                        .setRequest((org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent) request);
            case R4:
                if (request != null && !(request instanceof org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(String.format(
                            "Request should be of type: %s",
                            "org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent"));
                }
                return ((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry)
                        .setRequest((org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent) request);
            case R5:
                if (request != null && !(request instanceof org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(String.format(
                            "Request should be of type: %s",
                            "org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent"));
                }
                return ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry)
                        .setRequest((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request);

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }
}
