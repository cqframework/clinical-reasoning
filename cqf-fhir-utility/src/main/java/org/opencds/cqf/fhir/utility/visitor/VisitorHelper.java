package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;

public class VisitorHelper {

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<T> getParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(parametersParameters -> (T) parametersParameters.getValue());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseResource> Optional<T> getResourceParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(parametersParameters -> (T) parametersParameters.getResource());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<List<T>> getListParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameterValues(name))
                .map(vals -> vals.stream().map(val -> (T) val).collect(Collectors.toList()));
    }

    public static List<IDomainResource> getMetadataResourcesFromBundle(IBaseBundle bundle) {
        List<IDomainResource> resourceList = new ArrayList<>();
        var version = bundle.getStructureFhirVersionEnum();
        if (!BundleHelper.getEntryFirstRep(bundle).isEmpty()) {
            BundleHelper.getEntry(bundle).stream()
                    .map(e -> BundleHelper.getEntryResource(version, e))
                    .filter(r -> r != null)
                    .forEach(r -> {
                        switch (version) {
                            case DSTU3:
                                if (r instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
                                    resourceList.add((IDomainResource) r);
                                }
                                break;
                            case R4:
                                if (r instanceof org.hl7.fhir.r4.model.MetadataResource) {
                                    resourceList.add((IDomainResource) r);
                                }
                                break;
                            case R5:
                                if (r instanceof org.hl7.fhir.r5.model.MetadataResource) {
                                    resourceList.add((IDomainResource) r);
                                }
                                break;
                            default:
                                break;
                        }
                    });
        }

        return resourceList;
    }

    public static void findUnsupportedCapability(KnowledgeArtifactAdapter resource, List<String> capability)
            throws PreconditionFailedException {
        if (capability != null && !capability.isEmpty()) {
            List<IBaseExtension<?, ?>> knowledgeCapabilityExtension = resource.get().getExtension().stream()
                    .filter(ext -> ext.getUrl().contains("cqf-knowledgeCapability"))
                    .collect(Collectors.toList());
            if (knowledgeCapabilityExtension.isEmpty()) {
                // consider resource unsupported if it's knowledgeCapability is undefined
                throw new PreconditionFailedException(
                        String.format("Resource with url: '%s' does not specify capability.", resource.getUrl()));
            }
            knowledgeCapabilityExtension.stream()
                    .filter(ext -> !capability.contains(((IPrimitiveType<?>) ext.getValue()).getValue()))
                    .findAny()
                    .ifPresent((ext) -> {
                        throw new PreconditionFailedException(String.format(
                                "Resource with url: '%s' is not one of '%s'.",
                                resource.getUrl(), String.join(", ", capability)));
                    });
        }
    }

    public static void processCanonicals(
            KnowledgeArtifactAdapter resource,
            List<String> canonicalVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion)
            throws PreconditionFailedException {
        if (checkArtifactVersion != null && !checkArtifactVersion.isEmpty()) {
            // check throws an error
            findVersionInListMatchingResource(checkArtifactVersion, resource).ifPresent((version) -> {
                if (!resource.getVersion().equals(version)) {
                    throw new PreconditionFailedException(String.format(
                            "Resource with url '%s' has version '%s' but checkVersion specifies '%s'",
                            resource.getUrl(), resource.getVersion(), version));
                }
            });
        } else if (forceArtifactVersion != null && !forceArtifactVersion.isEmpty()) {
            // force just does a silent override
            findVersionInListMatchingResource(forceArtifactVersion, resource)
                    .ifPresent((version) -> resource.setVersion(version));
        } else if (canonicalVersion != null && !canonicalVersion.isEmpty() && !resource.hasVersion()) {
            // canonicalVersion adds a version if it's missing
            findVersionInListMatchingResource(canonicalVersion, resource)
                    .ifPresent((version) -> resource.setVersion(version));
        }
    }

    private static Optional<String> findVersionInListMatchingResource(
            List<String> list, KnowledgeArtifactAdapter resource) {
        return list.stream()
                .filter((canonical) -> Canonicals.getUrl(canonical).equals(resource.getUrl()))
                .map((canonical) -> Canonicals.getVersion(canonical))
                .findAny();
    }

    public static boolean typeHasCoding(ICompositeType type, String system, String code) {
        return false;
    }

    public static Optional<KnowledgeArtifactAdapter> tryGetLatestVersion(String inputReference, Repository repository) {
        return KnowledgeArtifactAdapter.findLatestVersion(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .map(res -> AdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }

    public static Optional<KnowledgeArtifactAdapter> tryGetLatestVersionWithStatus(
            String inputReference, Repository repository, String status) {
        return KnowledgeArtifactAdapter.findLatestVersion(SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
                        repository, inputReference, Searches.byStatus(status)))
                .map(res -> AdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }
}
