package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.Repository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class DraftVisitor extends BaseKnowledgeArtifactVisitor {
    public DraftVisitor(Repository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters draftParameters) {
        String version = VisitorHelper.getStringParameter("version", draftParameters)
                .orElseThrow(() -> new UnprocessableEntityException("The version argument is required"));
        var libRes = adapter.get();
        // check valid semver version
        checkVersionValidSemver(version);

        // remove release label and extension
        List<IBaseExtension<?, ?>> removeReleaseLabelAndDescription = libRes.getExtension().stream()
                .filter(ext -> !ext.getUrl().equals(IKnowledgeArtifactAdapter.RELEASE_DESCRIPTION_URL)
                        && !ext.getUrl().equals(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL))
                .collect(Collectors.toList());
        adapter.setExtension(removeReleaseLabelAndDescription);
        // remove approval date
        adapter.setApprovalDate(null);
        // new draft version
        String draftVersion = version + "-draft";
        String draftVersionUrl = Canonicals.getUrl(adapter.getUrl()) + "|" + draftVersion;

        // Root artifact must NOT have status of 'Active'. Existing drafts of
        // reference artifacts with the right version number will be adopted.
        // This check is performed here to facilitate that different treatment
        // for the root artifact and those referenced by it.
        if (!"active".equals(adapter.getStatus())) {
            throw new PreconditionFailedException(String.format(
                    "Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: '%s'",
                    adapter.getUrl(), adapter.getStatus()));
        }
        // Ensure only one resource exists with this URL
        var existingArtifactsForUrl = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, draftVersionUrl);
        if (!BundleHelper.getEntry(existingArtifactsForUrl).isEmpty()) {
            throw new PreconditionFailedException(String.format(
                    "A draft of Program '%s' already exists with version: '%s'. Only one draft of a program version can exist at a time.",
                    adapter.getUrl(), draftVersionUrl));
        }
        // create draft resources
        List<IDomainResource> resourcesToCreate = createDraftsOfArtifactAndRelated(libRes, version, new ArrayList<>());
        var transactionBundle = BundleHelper.newBundle(fhirVersion(), null, "transaction");
        List<String> urnList = resourcesToCreate.stream()
                .map(res -> "urn:uuid:" + UUID.randomUUID().toString())
                .collect(Collectors.toList());
        TreeSet<String> ownedResourceUrls = createOwnedResourceUrlCache(resourcesToCreate);
        for (int i = 0; i < resourcesToCreate.size(); i++) {
            IKnowledgeArtifactAdapter newResourceAdapter = IAdapterFactory.forFhirVersion(fhirVersion())
                    .createKnowledgeArtifactAdapter(resourcesToCreate.get(i));
            updateUsageContextReferencesWithUrns(newResourceAdapter, resourcesToCreate, urnList);
            updateRelatedArtifactUrlsWithNewVersions(
                    newResourceAdapter.combineComponentsAndDependencies(), draftVersion, ownedResourceUrls);
            var updateIdForBundle = newResourceAdapter.copy();
            updateIdForBundle.setId(urnList.get(i));
            BundleHelper.addEntry(transactionBundle, PackageHelper.createEntry(updateIdForBundle, false));
        }
        return repository.transaction(transactionBundle);

        // DependencyInfo --document here that there is a need for figuring out how to determine which package the
        // dependency is in.
        // what is dependency, where did it originate? potentially the package?
    }

    private List<IDomainResource> createDraftsOfArtifactAndRelated(
            IDomainResource resource, String version, List<IDomainResource> resourcesToCreate) {
        String draftVersion = version + "-draft";
        var sourceResourceAdapter =
                IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(resource);
        String draftVersionUrl = Canonicals.getUrl(sourceResourceAdapter.getUrl()) + "|" + draftVersion;

        // Decide if we need both of these checks
        var existingArtifactsWithMatchingUrl = IKnowledgeArtifactAdapter.findLatestVersion(
                SearchHelper.searchRepositoryByCanonicalWithPaging(repository, draftVersionUrl));
        var draftVersionAlreadyInBundle = resourcesToCreate.stream()
                .map(res -> IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(res))
                .filter(a -> a.getUrl().equals(Canonicals.getUrl(draftVersionUrl))
                        && a.getVersion().equals(draftVersion))
                .findAny();
        IDomainResource newResource = null;
        if (existingArtifactsWithMatchingUrl.isPresent()) {
            newResource = existingArtifactsWithMatchingUrl.get();
        } else if (draftVersionAlreadyInBundle.isPresent()) {
            newResource = draftVersionAlreadyInBundle.get().get();
        }

        if (newResource == null) {
            sourceResourceAdapter.setEffectivePeriod(null);
            newResource = sourceResourceAdapter.copy();
            var newResourceAdapter =
                    IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(newResource);
            newResourceAdapter.setStatus("draft");
            newResourceAdapter.setVersion(draftVersion);
            resourcesToCreate.add(newResource);
            var ownedRelatedArtifacts = sourceResourceAdapter.getOwnedRelatedArtifacts();
            for (var ra : ownedRelatedArtifacts) {
                var url = sourceResourceAdapter.resolvePathString(ra, "resource");
                processReferencedResourceForDraft(url, version, resourcesToCreate);
            }
        }
        return resourcesToCreate;
    }

    @SuppressWarnings("squid:S125")
    private void processReferencedResourceForDraft(
            String canonical, String version, List<IDomainResource> transactionBundle) {
        // If it’s an owned RelatedArtifact composed-of then we want to copy it
        // (references are updated in createDraftBundle before adding to the bundle
        // hence they are ignored here)
        // e.g.
        // relatedArtifact: [
        //  {
        //    resource: www.test.com/Library/123|0.0.1 <- try to update this reference to the latest version in
        // createDraftBundle
        //   },
        //  {
        //    extension: [{url: .../isOwned, valueBoolean: true}],
        //    resource: www.test.com/Library/190|1.2.3 <- resolve this resource, create a draft of it and
        // recursively check descendants
        //  }
        // ]

        Optional<IDomainResource> referencedResource = StringUtils.isBlank(canonical)
                ? Optional.empty()
                : Optional.of((IDomainResource)
                        BundleHelper.getEntryResourceFirstRep(SearchHelper.searchRepositoryByCanonicalWithPaging(
                                repository, VersionUtilities.canonicalTypeForVersion(fhirVersion(), canonical))));

        referencedResource.ifPresent(r -> createDraftsOfArtifactAndRelated(r, version, transactionBundle));
    }

    private void checkVersionValidSemver(String version) throws UnprocessableEntityException {
        if (version == null || version.isEmpty()) {
            throw new UnprocessableEntityException("The version argument is required");
        }
        if (version.contains("draft")) {
            throw new UnprocessableEntityException("The version cannot contain 'draft'");
        }
        if (version.contains("/") || version.contains("\\") || version.contains("|")) {
            throw new UnprocessableEntityException("The version contains illegal characters");
        }
        var pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(version);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH");
        }
    }

    private TreeSet<String> createOwnedResourceUrlCache(List<IDomainResource> resources) {
        TreeSet<String> retVal = new TreeSet<>();
        resources.stream()
                .map(resource -> IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(resource))
                .map(IKnowledgeArtifactAdapter::getOwnedRelatedArtifacts)
                .flatMap(List::stream)
                .map(IKnowledgeArtifactAdapter::getRelatedArtifactReference)
                .filter(r -> r != null)
                .map(Canonicals::getUrl)
                .forEach(retVal::add);
        return retVal;
    }

    private void updateUsageContextReferencesWithUrns(
            IKnowledgeArtifactAdapter newResource,
            List<IDomainResource> resourceListWithOriginalIds,
            List<String> idListForTransactionBundle) {
        var useContexts = newResource.getUseContext();
        for (var useContext : useContexts) {
            // will we ever need to resolve these references?
            var value = newResource.resolvePath(useContext, "value");
            if (value instanceof IBaseReference) {
                resourceListWithOriginalIds.stream()
                        .filter(resource -> (resource.getClass().getSimpleName() + "/"
                                        + resource.getIdElement().getIdPart())
                                .equals(newResource.resolvePathString(value, "reference")))
                        .findAny()
                        .ifPresent(resource -> {
                            int indexOfDraftInIdList = resourceListWithOriginalIds.indexOf(resource);
                            newResource
                                    .getModelResolver()
                                    .setValue(
                                            useContext,
                                            "value",
                                            VersionUtilities.referenceTypeForVersion(
                                                    fhirVersion(),
                                                    idListForTransactionBundle.get(indexOfDraftInIdList)));
                        });
            }
        }
    }

    private void updateRelatedArtifactUrlsWithNewVersions(
            List<IDependencyInfo> referenceList, String updatedVersion, TreeSet<String> ownedUrlCache) {
        // For each  relatedArtifact, update the version of the reference.
        referenceList.stream()
                // only update the references to owned resources (including dependencies)
                .filter(ra -> ownedUrlCache.contains(Canonicals.getUrl(ra.getReference())))
                .collect(Collectors.toList())
                .replaceAll(ra -> {
                    ra.setReference(Canonicals.getUrl(ra.getReference()) + "|" + updatedVersion);
                    return ra;
                });
    }
}
