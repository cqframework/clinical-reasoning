package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;

public class KnowledgeArtifactDraftVisitor implements KnowledgeArtifactVisitor {
    @Override
    public IBaseBundle visit(LibraryAdapter library, Repository repository, IBaseParameters draftParameters) {
        var fhirVersion = library.get().getStructureFhirVersionEnum();
        String version = VisitorHelper.getParameter("version", draftParameters, IPrimitiveType.class)
                .map(r -> (String) r.getValue())
                .orElseThrow(() -> new UnprocessableEntityException("The version argument is required"));
        var libRes = library.get();
        // check valid semverversion
        checkVersionValidSemver(version);

        // remove release label and extension
        List<IBaseExtension<?, ?>> removeReleaseLabelAndDescription = libRes.getExtension().stream()
                .filter(ext -> !ext.getUrl().equals(KnowledgeArtifactAdapter.releaseDescriptionUrl)
                        && !ext.getUrl().equals(KnowledgeArtifactAdapter.releaseLabelUrl))
                .collect(Collectors.toList());
        library.setExtension(removeReleaseLabelAndDescription);
        // remove approval date
        library.setApprovalDate(null);
        // new draft version
        String draftVersion = version + "-draft";
        String draftVersionUrl = Canonicals.getUrl(library.getUrl()) + "|" + draftVersion;

        // Root artifact must NOT have status of 'Active'. Existing drafts of
        // reference artifacts with the right verison number will be adopted.
        // This check is performed here to facilitate that different treatment
        // for the root artifact and those referenced by it.
        if (library.getStatus() != "active") {
            throw new PreconditionFailedException(String.format(
                    "Drafts can only be created from artifacts with status of 'active'. Resource '%s' has a status of: '%s'",
                    library.getUrl(), library.getStatus()));
        }
        // Ensure only one resource exists with this URL
        var existingArtifactsForUrl = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, draftVersionUrl);
        if (BundleHelper.getEntry(existingArtifactsForUrl).size() != 0) {
            throw new PreconditionFailedException(String.format(
                    "A draft of Program '%s' already exists with version: '%s'. Only one draft of a program version can exist at a time.",
                    library.getUrl(), draftVersionUrl));
        }
        // create draft resources
        List<IDomainResource> resourcesToCreate =
                createDraftsOfArtifactAndRelated(libRes, repository, version, new ArrayList<>(), fhirVersion);
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");
        List<String> urnList = resourcesToCreate.stream()
                .map(res -> "urn:uuid:" + UUID.randomUUID().toString())
                .collect(Collectors.toList());
        TreeSet<String> ownedResourceUrls = createOwnedResourceUrlCache(resourcesToCreate, fhirVersion);
        for (int i = 0; i < resourcesToCreate.size(); i++) {
            KnowledgeArtifactAdapter newResourceAdapter =
                    AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resourcesToCreate.get(i));
            updateUsageContextReferencesWithUrns(resourcesToCreate.get(i), resourcesToCreate, urnList, fhirVersion);
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

    @Override
    public IBase visit(KnowledgeArtifactAdapter library, Repository repository, IBaseParameters draftParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(
            PlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    private List<IDomainResource> createDraftsOfArtifactAndRelated(
            IDomainResource resource,
            Repository repository,
            String version,
            List<IDomainResource> resourcesToCreate,
            FhirVersionEnum fhirVersion) {
        String draftVersion = version + "-draft";
        var sourceResourceAdapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource);
        String draftVersionUrl = Canonicals.getUrl(sourceResourceAdapter.getUrl()) + "|" + draftVersion;

        // TODO: Decide if we need both of these checks
        var existingArtifactsWithMatchingUrl = KnowledgeArtifactAdapter.findLatestVersion(
                SearchHelper.searchRepositoryByCanonicalWithPaging(repository, draftVersionUrl));
        var draftVersionAlreadyInBundle = resourcesToCreate.stream()
                .map(res -> AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(res))
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
                    AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(newResource);
            newResourceAdapter.setStatus("draft");
            newResourceAdapter.setVersion(draftVersion);
            resourcesToCreate.add(newResource);
            var ownedRelatedArtifacts = sourceResourceAdapter.getOwnedRelatedArtifacts();
            for (var ra : ownedRelatedArtifacts) {
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
                processReferencedResourceForDraft(repository, ra, version, resourcesToCreate, fhirVersion);
            }
        }
        return resourcesToCreate;
    }

    private void processReferencedResourceForDraft(
            Repository repository,
            ICompositeType ra,
            String version,
            List<IDomainResource> transactionBundle,
            FhirVersionEnum fhirVersion) {

        Optional<IDomainResource> referencedResource = Optional.empty();
        switch (fhirVersion) {
            case DSTU3:
                referencedResource =
                        org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactDraftVisitor
                                .processReferencedResourceForDraft(
                                        repository, (org.hl7.fhir.dstu3.model.RelatedArtifact) ra, version)
                                .map(r -> (IDomainResource) r);
                break;
            case R4:
                referencedResource =
                        org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactDraftVisitor
                                .processReferencedResourceForDraft(
                                        repository, (org.hl7.fhir.r4.model.RelatedArtifact) ra, version)
                                .map(r -> (IDomainResource) r);
                break;
            case R5:
                referencedResource =
                        org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactDraftVisitor
                                .processReferencedResourceForDraft(
                                        repository, (org.hl7.fhir.r5.model.RelatedArtifact) ra, version)
                                .map(r -> (IDomainResource) r);
                break;
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
        referencedResource.ifPresent(
                r -> createDraftsOfArtifactAndRelated(r, repository, version, transactionBundle, fhirVersion));
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
        Pattern pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\d+\\.)?(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(version);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            throw new UnprocessableEntityException(
                    "The version must be in the format MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH.REVISION");
        }
    }

    private TreeSet<String> createOwnedResourceUrlCache(List<IDomainResource> resources, FhirVersionEnum fhirVersion) {
        TreeSet<String> retval = new TreeSet<String>();
        resources.stream()
                .map(resource -> AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource))
                .map(KnowledgeArtifactAdapter::getOwnedRelatedArtifacts)
                .flatMap(List::stream)
                .map(KnowledgeArtifactAdapter::getRelatedArtifactReference)
                .filter(r -> r != null)
                .map(Canonicals::getUrl)
                .forEach(retval::add);
        return retval;
    }

    private void updateUsageContextReferencesWithUrns(
            IDomainResource newResource,
            List<IDomainResource> resourceListWithOriginalIds,
            List<String> idListForTransactionBundle,
            FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactDraftVisitor
                        .updateUsageContextReferencesWithUrns(
                                (org.hl7.fhir.dstu3.model.MetadataResource) newResource,
                                resourceListWithOriginalIds.stream()
                                        .map(ra -> (org.hl7.fhir.dstu3.model.MetadataResource) ra)
                                        .collect(Collectors.toList()),
                                idListForTransactionBundle.stream()
                                        .map(id -> new org.hl7.fhir.dstu3.model.IdType(id))
                                        .collect(Collectors.toList()));
                break;
            case R4:
                org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactDraftVisitor
                        .updateUsageContextReferencesWithUrns(
                                (org.hl7.fhir.r4.model.MetadataResource) newResource,
                                resourceListWithOriginalIds.stream()
                                        .map(ra -> (org.hl7.fhir.r4.model.MetadataResource) ra)
                                        .collect(Collectors.toList()),
                                idListForTransactionBundle.stream()
                                        .map(id -> new org.hl7.fhir.r4.model.IdType(id))
                                        .collect(Collectors.toList()));
                break;
            case R5:
                org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactDraftVisitor
                        .updateUsageContextReferencesWithUrns(
                                (org.hl7.fhir.r5.model.MetadataResource) newResource,
                                resourceListWithOriginalIds.stream()
                                        .map(ra -> (org.hl7.fhir.r5.model.MetadataResource) ra)
                                        .collect(Collectors.toList()),
                                idListForTransactionBundle.stream()
                                        .map(id -> new org.hl7.fhir.r5.model.IdType(id))
                                        .collect(Collectors.toList()));
                break;

            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
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
