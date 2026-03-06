package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.context.RuntimeSearchParam.RuntimeSearchParamStatusEnum;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.SearchParameter;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r5.model.PrimitiveType;

public class BundleHelper {
    private static final String UNSUPPORTED_VERSION_OF_FHIR = "Unsupported version of FHIR: %s";

    private BundleHelper() {}

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement getEntryFirstRep(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle) bundle).getEntryFirstRep();
            case R4 -> ((org.hl7.fhir.r4.model.Bundle) bundle).getEntryFirstRep();
            case R5 -> ((org.hl7.fhir.r5.model.Bundle) bundle).getEntryFirstRep();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseResource
     */
    public static IBaseResource getEntryResourceFirstRep(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> {
                var dstu3Entry = ((Bundle) bundle).getEntryFirstRep();
                yield dstu3Entry != null && dstu3Entry.hasResource() ? dstu3Entry.getResource() : null;
            }
            case R4 -> {
                var r4Entry = ((org.hl7.fhir.r4.model.Bundle) bundle).getEntryFirstRep();
                yield r4Entry != null && r4Entry.hasResource() ? r4Entry.getResource() : null;
            }
            case R5 -> {
                var r5Entry = ((org.hl7.fhir.r5.model.Bundle) bundle).getEntryFirstRep();
                yield r5Entry != null && r5Entry.hasResource() ? r5Entry.getResource() : null;
            }
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a list of resources from the Bundle entries
     *
     * @param bundle IBaseBundle type
     * @return List of IBaseResource
     */
    public static List<IBaseResource> getEntryResources(IBaseBundle bundle) {
        List<IBaseResource> resources = new ArrayList<>();
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Entry = ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntry();
                dstu3Entry.stream()
                        .filter(Bundle.BundleEntryComponent::hasResource)
                        .forEach(entry -> resources.add(entry.getResource()));
                break;
            case R4:
                var r4Entry = ((org.hl7.fhir.r4.model.Bundle) bundle).getEntry();
                r4Entry.stream()
                        .filter(BundleEntryComponent::hasResource)
                        .forEach(entry -> resources.add(entry.getResource()));
                break;
            case R5:
                var r5Entry = ((org.hl7.fhir.r5.model.Bundle) bundle).getEntry();
                r5Entry.stream()
                        .filter(org.hl7.fhir.r5.model.Bundle.BundleEntryComponent::hasResource)
                        .forEach(entry -> resources.add(entry.getResource()));
                break;

            default:
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        }

        return resources;
    }

    /**
     * Returns the Resource for a given entry
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return IBaseResource
     */
    public static IBaseResource getEntryResource(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle.BundleEntryComponent) entry).getResource();
            case R4 -> ((BundleEntryComponent) entry).getResource();
            case R5 -> ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getResource();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Checks if an entry has a request type of PUT
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    public static boolean isEntryRequestPut(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 ->
                Optional.ofNullable(((Bundle.BundleEntryComponent) entry).getRequest())
                        .map(Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == Bundle.HTTPVerb.PUT)
                        .isPresent();
            case R4 ->
                Optional.ofNullable(((BundleEntryComponent) entry).getRequest())
                        .map(BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT)
                        .isPresent();
            case R5 ->
                Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r5.model.Bundle.HTTPVerb.PUT)
                        .isPresent();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Checks if an entry has a request type of POST
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    public static boolean isEntryRequestPost(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 ->
                Optional.ofNullable(((Bundle.BundleEntryComponent) entry).getRequest())
                        .map(Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == Bundle.HTTPVerb.POST)
                        .isPresent();
            case R4 ->
                Optional.ofNullable(((BundleEntryComponent) entry).getRequest())
                        .map(BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST)
                        .isPresent();
            case R5 ->
                Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r5.model.Bundle.HTTPVerb.POST)
                        .isPresent();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Checks if an entry has a request type of DELETE
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    public static boolean isEntryRequestDelete(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 ->
                Optional.ofNullable(((Bundle.BundleEntryComponent) entry).getRequest())
                        .map(Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == Bundle.HTTPVerb.DELETE)
                        .isPresent();
            case R4 ->
                Optional.ofNullable(((BundleEntryComponent) entry).getRequest())
                        .map(BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r4.model.Bundle.HTTPVerb.DELETE)
                        .isPresent();
            case R5 ->
                Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).getRequest())
                        .map(org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent::getMethod)
                        .filter(r -> r == org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE)
                        .isPresent();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns the list of entries from the Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseBackboneElement
     */
    @SuppressWarnings("unchecked")
    public static <T extends IBaseBackboneElement> List<T> getEntry(IBaseBundle bundle) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> (List<T>) ((Bundle) bundle).getEntry();
            case R4 -> (List<T>) ((org.hl7.fhir.r4.model.Bundle) bundle).getEntry();
            case R5 -> (List<T>) ((org.hl7.fhir.r5.model.Bundle) bundle).getEntry();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Gets request id if present
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return Optional IIdType
     */
    public static Optional<IIdType> getEntryRequestId(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 ->
                Optional.ofNullable(((Bundle.BundleEntryComponent) entry)
                                .getRequest()
                                .getUrl())
                        .map(Canonicals::getIdPart)
                        .map(IdType::new);
            case R4 ->
                Optional.ofNullable(((BundleEntryComponent) entry).getRequest().getUrl())
                        .map(Canonicals::getIdPart)
                        .map(org.hl7.fhir.r4.model.IdType::new);
            case R5 ->
                Optional.ofNullable(((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry)
                                .getRequest()
                                .getUrl())
                        .map(Canonicals::getIdPart)
                        .map(org.hl7.fhir.r5.model.IdType::new);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    public static List<IIdType> getBundleEntryResourceIds(FhirContext fhirContext, IBaseBundle bundle) {
        return getBundleEntryResourceIds(fhirContext.getVersion().getVersion(), bundle);
    }

    public static List<IIdType> getBundleEntryResourceIds(FhirVersionEnum fhirVersion, IBaseBundle bundle) {

        List<IBaseBackboneElement> entry = getEntry(bundle);
        if (entry.isEmpty()) {
            return Collections.emptyList();
        }

        List<IIdType> retVal = new ArrayList<>(entry.size());

        entry.forEach(e -> {
            retVal.add(getEntryResource(fhirVersion, e).getIdElement());
        });

        return retVal;
    }

    /**
     * Gets request url if present
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return url of request
     */
    public static String getEntryRequestUrl(FhirVersionEnum fhirVersion, IBaseBackboneElement entry) {
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle.BundleEntryComponent) entry).getRequest().getUrl();
            case R4 ->
                ((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry)
                        .getRequest()
                        .getUrl();
            case R5 ->
                ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry)
                        .getRequest()
                        .getUrl();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
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
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * Adds the entry to the Bundle and returns the Bundle
     *
     * @param bundle IBaseBundle type
     * @param entry IBaseBackboneElement type
     * @return IBaseBundle
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
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        }

        return bundle;
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @return IBaseBundle
     */
    public static IBaseBundle newBundle(FhirVersionEnum fhirVersion) {
        return newBundle(fhirVersion, null, null);
    }

    /**
     * Returns a new Bundle for the given version of FHIR and type
     *
     * @param fhirVersion FhirVersionEnum
     * @param type The type of Bundle to return, defaults to COLLECTION
     * @return IBaseBundle
     */
    public static IBaseBundle newBundle(FhirVersionEnum fhirVersion, String type) {
        return newBundle(fhirVersion, null, type);
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @param id Id to set on the Bundle, will ignore if null
     * @param type The type of Bundle to return, defaults to COLLECTION
     * @return IBaseBundle
     */
    public static IBaseBundle newBundle(FhirVersionEnum fhirVersion, String id, String type) {
        return switch (fhirVersion) {
            case DSTU3 -> newDstu3Bundle(id, type);
            case R4 -> newR4Bundle(id, type);
            case R5 -> newR5Bundle(id, type);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    @Nonnull
    private static org.hl7.fhir.r5.model.Bundle newR5Bundle(String id, String type) {
        var r5Bundle = new org.hl7.fhir.r5.model.Bundle();
        if (id != null && !id.isEmpty()) {
            r5Bundle.setId(id);
        }
        r5Bundle.setType(
                type == null || type.isEmpty()
                        ? org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION
                        : org.hl7.fhir.r5.model.Bundle.BundleType.fromCode(type));
        return r5Bundle;
    }

    @Nonnull
    private static org.hl7.fhir.r4.model.Bundle newR4Bundle(String id, String type) {
        var r4Bundle = new org.hl7.fhir.r4.model.Bundle();
        if (id != null && !id.isEmpty()) {
            r4Bundle.setId(id);
        }
        r4Bundle.setType(
                type == null || type.isEmpty()
                        ? org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION
                        : org.hl7.fhir.r4.model.Bundle.BundleType.fromCode(type));
        return r4Bundle;
    }

    @Nonnull
    private static Bundle newDstu3Bundle(String id, String type) {
        var dstu3Bundle = new Bundle();
        if (id != null && !id.isEmpty()) {
            dstu3Bundle.setId(id);
        }
        dstu3Bundle.setType(
                type == null || type.isEmpty() ? Bundle.BundleType.COLLECTION : Bundle.BundleType.fromCode(type));
        return dstu3Bundle;
    }

    /**
     * Sets the BundleType of the Bundle and returns the Bundle
     * @param bundle IBaseBundle
     * @param bundleType String
     * @return IBaseBundle
     */
    public static IBaseBundle setBundleType(IBaseBundle bundle, String bundleType) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle) bundle).setType(Bundle.BundleType.fromCode(bundleType));
            case R4 ->
                ((org.hl7.fhir.r4.model.Bundle) bundle)
                        .setType(org.hl7.fhir.r4.model.Bundle.BundleType.fromCode(bundleType));
            case R5 ->
                ((org.hl7.fhir.r5.model.Bundle) bundle)
                        .setType(org.hl7.fhir.r5.model.Bundle.BundleType.fromCode(bundleType));
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Sets the total property of the Bundle and returns the Bundle
     * @param bundle IBaseBundle
     * @param total int
     * @return IBaseBundle
     */
    public static IBaseBundle setBundleTotal(IBaseBundle bundle, int total) {
        var fhirVersion = bundle.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle) bundle).setTotal(total);
            case R4 -> ((org.hl7.fhir.r4.model.Bundle) bundle).setTotal(total);
            case R5 -> ((org.hl7.fhir.r5.model.Bundle) bundle).setTotal(total);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new entry element
     * @param fhirVersion FhirVersionEnum
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newEntry(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case DSTU3 -> new Bundle.BundleEntryComponent();
            case R4 -> new BundleEntryComponent();
            case R5 -> new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent();
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new entry element with the Resource
     * @param resource IBaseResource
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newEntryWithResource(IBaseResource resource) {
        var fhirVersion = resource.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> new Bundle.BundleEntryComponent().setResource((org.hl7.fhir.dstu3.model.Resource) resource);
            case R4 -> new BundleEntryComponent().setResource((org.hl7.fhir.r4.model.Resource) resource);
            case R5 ->
                new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setResource((org.hl7.fhir.r5.model.Resource) resource);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new entry element with the Response
     * @param fhirVersion FhirVersionEnum
     * @param response IBaseBackboneElement
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newEntryWithResponse(
            FhirVersionEnum fhirVersion, IBaseBackboneElement response) {
        return switch (fhirVersion) {
            case DSTU3 -> new Bundle.BundleEntryComponent().setResponse((Bundle.BundleEntryResponseComponent) response);
            case R4 ->
                new BundleEntryComponent()
                        .setResponse((org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent) response);
            case R5 ->
                new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setResponse((org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent) response);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new BundleEntryResponse element with the location
     * @param fhirVersion FhirVersionEnum
     * @param location String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newResponseWithLocation(FhirVersionEnum fhirVersion, String location) {
        return switch (fhirVersion) {
            case DSTU3 -> new Bundle.BundleEntryResponseComponent().setLocation(location);
            case R4 -> new org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent().setLocation(location);
            case R5 -> new org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent().setLocation(location);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new BundleEntryRequest element with the method and url
     * @param fhirVersion FhirVersionEnum
     * @param method String
     * @param url String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newRequest(FhirVersionEnum fhirVersion, String method, String url) {
        return switch (fhirVersion) {
            case DSTU3 ->
                new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);
            case R4 ->
                new BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);
            case R5 ->
                new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method))
                        .setUrl(url);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Returns a new BundleEntryRequest element with the method
     * @param fhirVersion FhirVersionEnum
     * @param method String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement newRequest(FhirVersionEnum fhirVersion, String method) {
        return switch (fhirVersion) {
            case DSTU3 -> new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.fromCode(method));
            case R4 ->
                new BundleEntryRequestComponent().setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method));
            case R5 ->
                new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method));
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Sets the BundleEntryRequest url
     * @param fhirVersion FhirVersionEnum
     * @param request IBaseBackboneElement
     * @param url String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement setRequestUrl(
            FhirVersionEnum fhirVersion, IBaseBackboneElement request, String url) {
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle.BundleEntryRequestComponent) request).setUrl(url);
            case R4 -> ((BundleEntryRequestComponent) request).setUrl(url);
            case R5 -> ((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request).setUrl(url);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Sets the BundleEntryRequest ifNoneExist property
     * @param fhirVersion FhirVersionEnum
     * @param request IBaseBackboneElement
     * @param ifNoneExist String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement setRequestIfNoneExist(
            FhirVersionEnum fhirVersion, IBaseBackboneElement request, String ifNoneExist) {
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle.BundleEntryRequestComponent) request).setIfNoneExist(ifNoneExist);
            case R4 -> ((BundleEntryRequestComponent) request).setIfNoneExist(ifNoneExist);
            case R5 -> ((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request).setIfNoneExist(ifNoneExist);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Sets the BundleEntry fullUrl property
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement
     * @param fullUrl String
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement setEntryFullUrl(
            FhirVersionEnum fhirVersion, IBaseBackboneElement entry, String fullUrl) {
        return switch (fhirVersion) {
            case DSTU3 -> ((Bundle.BundleEntryComponent) entry).setFullUrl(fullUrl);
            case R4 -> ((BundleEntryComponent) entry).setFullUrl(fullUrl);
            case R5 -> ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry).setFullUrl(fullUrl);
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    /**
     * Sets the BundleEntry request property
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement
     * @param request IBaseBackboneElement
     * @return IBaseBackboneElement
     */
    public static IBaseBackboneElement setEntryRequest(
            FhirVersionEnum fhirVersion, IBaseBackboneElement entry, IBaseBackboneElement request) {
        String requestTypeError = "Request should be of type: %s";
        return switch (fhirVersion) {
            case DSTU3 -> {
                if (request != null && !(request instanceof Bundle.BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(
                            requestTypeError.formatted("org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent"));
                }
                yield ((Bundle.BundleEntryComponent) entry).setRequest((Bundle.BundleEntryRequestComponent) request);
            }
            case R4 -> {
                if (request != null && !(request instanceof BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(
                            requestTypeError.formatted("org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent"));
                }
                yield ((BundleEntryComponent) entry).setRequest((BundleEntryRequestComponent) request);
            }
            case R5 -> {
                if (request != null && !(request instanceof org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent)) {
                    throw new IllegalArgumentException(
                            requestTypeError.formatted("org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent"));
                }
                yield ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entry)
                        .setRequest((org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent) request);
            }
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }

    public static RuntimeSearchParam resourceToRuntimeSearchParam(IBaseResource resource) {
        var fhirVersion = resource.getStructureFhirVersionEnum();
        return switch (fhirVersion) {
            case DSTU3 -> {
                var res = (SearchParameter) resource;
                yield new RuntimeSearchParam(
                        res.getIdElement(),
                        res.getUrl(),
                        res.getCode(),
                        res.getDescription(),
                        res.getExpression(),
                        RestSearchParameterTypeEnum.REFERENCE,
                        null,
                        res.getTarget().stream().map(StringType::toString).collect(Collectors.toSet()),
                        RuntimeSearchParamStatusEnum.ACTIVE,
                        res.getBase().stream().map(StringType::toString).toList());
            }
            case R4 -> {
                var resR4 = (org.hl7.fhir.r4.model.SearchParameter) resource;
                yield new RuntimeSearchParam(
                        resR4.getIdElement(),
                        resR4.getUrl(),
                        resR4.getCode(),
                        resR4.getDescription(),
                        resR4.getExpression(),
                        RestSearchParameterTypeEnum.REFERENCE,
                        null,
                        resR4.getTarget().stream()
                                .map(org.hl7.fhir.r4.model.StringType::toString)
                                .collect(Collectors.toSet()),
                        RuntimeSearchParamStatusEnum.ACTIVE,
                        resR4.getBase().stream()
                                .map(org.hl7.fhir.r4.model.StringType::toString)
                                .toList());
            }
            case R5 -> {
                var resR5 = (org.hl7.fhir.r5.model.SearchParameter) resource;
                yield new RuntimeSearchParam(
                        resR5.getIdElement(),
                        resR5.getUrl(),
                        resR5.getCode(),
                        resR5.getDescription(),
                        resR5.getExpression(),
                        RestSearchParameterTypeEnum.REFERENCE,
                        null,
                        resR5.getTarget().stream().map(PrimitiveType::toString).collect(Collectors.toSet()),
                        RuntimeSearchParamStatusEnum.ACTIVE,
                        resR5.getBase().stream().map(PrimitiveType::toString).toList());
            }
            default ->
                throw new IllegalArgumentException(
                        UNSUPPORTED_VERSION_OF_FHIR.formatted(fhirVersion.getFhirVersionString()));
        };
    }
}
