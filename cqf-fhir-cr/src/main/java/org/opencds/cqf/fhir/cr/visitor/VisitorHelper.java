package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;

public class VisitorHelper {

    private VisitorHelper() {}

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<T> getParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> (T) parametersParameters.getValue());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseResource> Optional<T> getResourceParameter(
            String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> (T) parametersParameters.getResource());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseDatatype> Optional<List<T>> getListParameter(
            String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameterValues(name))
                .map(values -> values.stream().map(value -> (T) value).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public static Optional<List<String>> getStringListParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameterValues(name))
                .map(values -> values.stream()
                        .map(value -> ((IPrimitiveType<String>) value).getValue())
                        .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public static Optional<Boolean> getBooleanParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> ((IPrimitiveType<Boolean>) parametersParameters.getValue()).getValue());
    }

    @SuppressWarnings("unchecked")
    public static Optional<Date> getDateParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> ((IPrimitiveType<Date>) parametersParameters.getValue()).getValue());
    }

    @SuppressWarnings("unchecked")
    public static Optional<Integer> getIntegerParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> ((IPrimitiveType<Integer>) parametersParameters.getValue()).getValue());
    }

    @SuppressWarnings("unchecked")
    public static Optional<String> getStringParameter(String name, IBaseParameters operationParameters) {
        var factory = IAdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(factory::createParameters)
                .map(p -> p.getParameter(name))
                .map(parametersParameters -> ((IPrimitiveType<String>) parametersParameters.getValue()).getValue());
    }

    public static List<IDomainResource> getMetadataResourcesFromBundle(IBaseBundle bundle) {
        List<IDomainResource> resourceList = new ArrayList<>();
        var version = bundle.getStructureFhirVersionEnum();
        BundleHelper.getEntry(bundle).stream()
                .map(e -> BundleHelper.getEntryResource(version, e))
                .filter(r -> r != null)
                .forEach(r -> {
                    switch (version) {
                        case DSTU3:
                            if (r instanceof IDomainResource resource) {
                                resourceList.add(resource);
                            }
                            break;
                        case R4:
                            if (r instanceof IDomainResource resource) {
                                resourceList.add(resource);
                            }
                            break;
                        case R5:
                            if (r instanceof IDomainResource resource) {
                                resourceList.add(resource);
                            }
                            break;
                        default:
                            break;
                    }
                });

        return resourceList;
    }

    public static void findUnsupportedCapability(IKnowledgeArtifactAdapter resource, List<String> capability)
            throws PreconditionFailedException {
        if (capability != null && !capability.isEmpty()) {
            List<IBaseExtension<?, ?>> knowledgeCapabilityExtension = resource.get().getExtension().stream()
                    .filter(ext -> ext.getUrl().contains("cqf-knowledgeCapability"))
                    .collect(Collectors.toList());
            if (knowledgeCapabilityExtension.isEmpty()) {
                // consider resource unsupported if it's knowledgeCapability is undefined
                throw new PreconditionFailedException(
                        "Resource with url: '%s' does not specify capability.".formatted(resource.getUrl()));
            }
            knowledgeCapabilityExtension.stream()
                    .filter(ext -> !capability.contains(((IPrimitiveType<?>) ext.getValue()).getValue()))
                    .findAny()
                    .ifPresent(ext -> {
                        throw new PreconditionFailedException("Resource with url: '%s' is not one of '%s'."
                                .formatted(resource.getUrl(), String.join(", ", capability)));
                    });
        }
    }

    public static void processCanonicals(
            IKnowledgeArtifactAdapter resource, ImmutableTriple<List<String>, List<String>, List<String>> versionTuple)
            throws PreconditionFailedException {
        var canonicalVersion = versionTuple.left;
        var checkArtifactVersion = versionTuple.middle;
        var forceArtifactVersion = versionTuple.right;
        if (checkArtifactVersion != null && !checkArtifactVersion.isEmpty()) {
            // check throws an error
            findVersionInListMatchingResource(checkArtifactVersion, resource).ifPresent(version -> {
                if (!resource.getVersion().equals(version)) {
                    throw new PreconditionFailedException(
                            "Resource with url '%s' has version '%s' but checkVersion specifies '%s'"
                                    .formatted(resource.getUrl(), resource.getVersion(), version));
                }
            });
        } else if (forceArtifactVersion != null && !forceArtifactVersion.isEmpty()) {
            // force just does a silent override
            findVersionInListMatchingResource(forceArtifactVersion, resource).ifPresent(resource::setVersion);
        } else if (canonicalVersion != null && !canonicalVersion.isEmpty() && !resource.hasVersion()) {
            // canonicalVersion adds a version if it's missing
            findVersionInListMatchingResource(canonicalVersion, resource).ifPresent(resource::setVersion);
        }
    }

    private static Optional<String> findVersionInListMatchingResource(
            List<String> list, IKnowledgeArtifactAdapter resource) {
        return list.stream()
                .filter(canonical -> Canonicals.getUrl(canonical).equals(resource.getUrl()))
                .map(Canonicals::getVersion)
                .findAny();
    }

    public static Optional<IKnowledgeArtifactAdapter> tryGetLatestVersion(
            String inputReference, IRepository repository) {
        return IKnowledgeArtifactAdapter.findLatestVersion(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .map(res -> IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }

    public static Optional<IKnowledgeArtifactAdapter> tryGetLatestVersionWithStatus(
            String inputReference, IRepository repository, String status) {
        return IKnowledgeArtifactAdapter.findLatestVersion(SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
                        repository, inputReference, Searches.byStatus(status)))
                .map(res -> IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }

    public static Optional<IKnowledgeArtifactAdapter> tryGetLatestVersionExceptStatus(
            String inputReference, IRepository repository, String status) {
        return IKnowledgeArtifactAdapter.findLatestVersion(SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
                        repository, inputReference, Searches.exceptStatus(status)))
                .map(res -> IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter(res));
    }
}
