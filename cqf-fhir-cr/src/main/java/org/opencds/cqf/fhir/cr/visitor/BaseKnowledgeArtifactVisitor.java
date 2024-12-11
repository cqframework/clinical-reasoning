package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.processCanonicals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;

public abstract class BaseKnowledgeArtifactVisitor implements IKnowledgeArtifactVisitor {
    String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";
    protected final Repository repository;
    protected Optional<IValueSetExpansionCache> valueSetExpansionCache = Optional.empty();

    protected BaseKnowledgeArtifactVisitor(Repository repository) {
        this.repository = repository;
        valueSetExpansionCache = null;
    }

    protected BaseKnowledgeArtifactVisitor(Repository repository, IValueSetExpansionCache valueSetExpansionCache) {
        this.repository = repository;
        this.valueSetExpansionCache = Optional.ofNullable(valueSetExpansionCache);
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }

    protected FhirVersionEnum fhirVersion() {
        return fhirContext().getVersion().getVersion();
    }

    protected Optional<IValueSetExpansionCache> getExpansionCache() {
        return valueSetExpansionCache;
    }

    protected List<IBaseBackboneElement> findArtifactCommentsToUpdate(
            IBaseResource artifact, String releaseVersion, Repository repository) {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            return org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            return org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            return org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Version not supported");
        }
    }

    protected List<IDomainResource> getComponents(
            IKnowledgeArtifactAdapter adapter, Repository repository, ArrayList<IDomainResource> resourcesToUpdate) {
        adapter.getOwnedRelatedArtifacts().stream().forEach(c -> {
            final var preReleaseReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(c);
            Optional<IKnowledgeArtifactAdapter> maybeArtifact =
                    VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
            if (maybeArtifact.isPresent()) {
                if (resourcesToUpdate.stream().noneMatch(rtu -> rtu.getId()
                        .equals(maybeArtifact.get().getId().toString()))) {
                    resourcesToUpdate.add(maybeArtifact.get().get());
                    getComponents(maybeArtifact.get(), repository, resourcesToUpdate);
                }
            } else {
                throw new ResourceNotFoundException("Unexpected resource not found when getting components");
            }
        });

        return resourcesToUpdate;
    }

    protected void recursiveGather(
            IKnowledgeArtifactAdapter adapter,
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            List<String> capability,
            List<String> include,
            ImmutableTriple<List<String>, List<String>, List<String>> versionTuple)
            throws PreconditionFailedException {
        if (adapter == null) {
            return;
        }
        if (!gatheredResources.keySet().contains(adapter.getCanonical())) {
            gatheredResources.put(adapter.getCanonical(), adapter);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, versionTuple);

            adapter.combineComponentsAndDependencies().stream()
                    // sometimes VS dependencies aren't FHIR resources
                    .filter(ra -> StringUtils.isNotBlank(ra.getReference())
                            && StringUtils.isNotBlank(Canonicals.getResourceType(ra.getReference())))
                    .filter(ra -> {
                        try {
                            var resourceDef =
                                    fhirContext().getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                            return resourceDef != null;
                        } catch (DataFormatException e) {
                            if (e.getMessage().contains("1684")) {
                                return false;
                            } else {
                                throw new DataFormatException(e.getMessage());
                            }
                        }
                    })
                    .map(ra -> SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getReference()))
                    .map(searchBundle -> (IDomainResource) BundleHelper.getEntryResourceFirstRep(searchBundle))
                    .filter(r -> r != null)
                    .map(r -> IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(r))
                    .forEach(component ->
                            recursiveGather(component, gatheredResources, capability, include, versionTuple));
        }
    }

    protected void addBundleEntry(IBaseBundle bundle, boolean isPut, IKnowledgeArtifactAdapter adapter) {
        if (bundle == null) {
            return;
        }
        if (BundleHelper.getEntryResources(bundle).stream()
                .map(e -> IAdapterFactory.forFhirVersion(fhirVersion())
                        .createKnowledgeArtifactAdapter((IDomainResource) e))
                .filter(mr -> mr.getUrl() != null)
                .noneMatch(mr -> mr.getUrl().equals(adapter.getUrl())
                        && (!mr.hasVersion() || mr.getVersion().equals(adapter.getVersion())))) {
            var entry = PackageHelper.createEntry(adapter.get(), isPut);
            BundleHelper.addEntry(bundle, entry);
        }
    }

    protected <T extends ICompositeType & IBaseHasExtensions> void addRelatedArtifact(
            List<T> relatedArtifacts, IKnowledgeArtifactAdapter adapter) {
        if (relatedArtifacts == null) {
            return;
        }
        var reference = adapter.hasVersion()
                ? adapter.getUrl().concat(String.format("|%s", adapter.getVersion()))
                : adapter.getUrl();
        if (relatedArtifacts.stream().noneMatch(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                .equals(reference))) {
            relatedArtifacts.add(IKnowledgeArtifactAdapter.newRelatedArtifact(
                    fhirVersion(), "depends-on", reference, adapter.getDescriptor()));
        }
    }
}
