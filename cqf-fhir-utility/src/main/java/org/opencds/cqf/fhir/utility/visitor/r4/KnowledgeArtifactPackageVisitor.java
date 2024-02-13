package org.opencds.cqf.fhir.utility.visitor.r4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.r4.ResourceClassMapHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;


public class KnowledgeArtifactPackageVisitor implements r4KnowledgeArtifactVisitor  {
  public Bundle visit(r4LibraryAdapter library, Repository theFhirRepository, Parameters thePackageParameters) {
    Optional<UriType> artifactRoute = MetadataResourceHelper.getParameter("artifactRoute", thePackageParameters, UriType.class)
    Optional<UriType> endpointUri = MetadataResourceHelper.getParameter("endpointUri", thePackageParameters, UriType.class)
    Optional<Endpoint> endpoint = MetadataResourceHelper.getParameter("endpoint", thePackageParameters, Endpoint.class)
    Optional<Endpoint> terminologyEndpoint = MetadataResourceHelper.getParameter("terminologyEndpoint", thePackageParameters, Endpoint.class)
    if (
				(artifactRoute.isPresent() && !artifactRoute.get().isBlank() && !artifactRoute.get().isEmpty())
					|| (endpointUri != null && !endpointUri.isBlank() && !endpointUri.isEmpty())
					|| contentEndpoint != null
					|| terminologyEndpoint != null
			) {
			throw new NotImplementedOperationException("This repository is not implementing custom Content and Terminology endpoints at this time");
		}
		if (packageOnly != null) {
			throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
		}
		if (count != null && count < 0) {
			throw new UnprocessableEntityException("'count' must be non-negative");
		}
		MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(id.getResourceType()), id);
		// TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a comprehensive manifest
		Bundle packagedBundle = new Bundle();
		if (include != null
			&& include.size() == 1
			&& include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			BundleEntryComponent entry = createEntry(resource);
			entry.getRequest().setUrl(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
			entry.getRequest().setMethod(HTTPVerb.POST);
			entry.getRequest().setIfNoneExist("url="+resource.getUrl()+"&version="+resource.getVersion());
			packagedBundle.addEntry(entry);
		} else {
			recursivePackage(resource, packagedBundle, hapiFhirRepository, capability, include, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			List<BundleEntryComponent> included = findUnsupportedInclude(packagedBundle.getEntry(),include);
			packagedBundle.setEntry(included);
		}
		setCorrectBundleType(count,offset,packagedBundle);
		pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
		handleValueSetReferenceExtensions(resource, packagedBundle.getEntry(), hapiFhirRepository);
    return theFhirRepository.transaction(packagedBundle);

    // DependencyInfo --document here that there is a need for figuring out how to determine which package the dependency is in.
    // what is dependency, where did it originate? potentially the package?
  }
  public IBase visit(IBaseLibraryAdapter library, Repository theFhirRepository, IBaseParameters draftParameters){
    return new OperationOutcome();
  }
  public IBase visit(IBaseKnowledgeArtifactAdapter library, Repository theFhirRepository, IBaseParameters draftParameters){
    return new OperationOutcome();
  }
//   public IBase visit(r4KnowledgeArtifactAdapter library, Repository theFhirRepository, IBaseParameters draftParameters){
//     return new OperationOutcome();
//   }
  public IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository theRepository, IBaseParameters theParameters) {
    List<DependencyInfo> dependencies = planDefinition.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(ValueSetAdapter valueSet, Repository theRepository, IBaseParameters theParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }
    private List<MetadataResource> createDraftsOfArtifactAndRelated(MetadataResource theResource, Repository theRepository, String theVersion, List<MetadataResource> theNewResourcesToCreate) {
        String draftVersion = theVersion + "-draft";
        String draftVersionUrl = Canonicals.getUrl(theResource.getUrl()) + "|" + draftVersion;

        // TODO: Decide if we need both of these checks
        Optional<MetadataResource> existingArtifactsWithMatchingUrl = KnowledgeArtifactAdapter.findLatestVersion(searchResourceByUrl(draftVersionUrl, theRepository));
        Optional<MetadataResource> draftVersionAlreadyInBundle = theNewResourcesToCreate.stream().filter(res -> res.getUrl().equals(Canonicals.getUrl(draftVersionUrl)) && res.getVersion().equals(draftVersion)).findAny();
        MetadataResource newResource = null;
        if (existingArtifactsWithMatchingUrl.isPresent()) {
            newResource = existingArtifactsWithMatchingUrl.get();
        } else if(draftVersionAlreadyInBundle.isPresent()) {
            newResource = draftVersionAlreadyInBundle.get();
        }

        if (newResource == null) {
            r4KnowledgeArtifactAdapter sourceResourceAdapter = new AdapterFactory().createKnowledgeArtifactAdapter(theResource);
            sourceResourceAdapter.setEffectivePeriod(null);
            newResource = theResource.copy();
            newResource.setStatus(Enumerations.PublicationStatus.DRAFT);
            newResource.setVersion(draftVersion);
            theNewResourcesToCreate.add(newResource);
            for (RelatedArtifact ra : sourceResourceAdapter.getOwnedRelatedArtifacts()) {
            // If it’s an owned RelatedArtifact composed-of then we want to copy it
            // (references are updated in createDraftBundle before adding to the bundle
            // hence they are ignored here)
            // e.g. 
            // relatedArtifact: [
            //  { 
            //    resource: www.test.com/Library/123|0.0.1 <- try to update this reference to the latest version in createDraftBundle
            //   },
            //  { 
            //    extension: [{url: .../isOwned, valueBoolean: true}],
            //    resource: www.test.com/Library/190|1.2.3 <- resolve this resource, create a draft of it and recursively check descendants
            //  }
            // ]
                if (ra.hasUrl()) {
                    Bundle referencedResourceBundle = searchResourceByUrl(ra.getUrl(), theRepository);
                    processReferencedResourceForDraft(theRepository, referencedResourceBundle, ra, theVersion, theNewResourcesToCreate);
                } else if (ra.hasResource()) {
                    Bundle referencedResourceBundle = searchResourceByUrl(ra.getResourceElement().getValueAsString(), theRepository);
                    processReferencedResourceForDraft(theRepository, referencedResourceBundle, ra, theVersion, theNewResourcesToCreate);
                }
            }
        }
        return theNewResourcesToCreate;
    }
	
	private void processReferencedResourceForDraft(Repository theRepository, Bundle referencedResourceBundle, RelatedArtifact ra, String version, List<MetadataResource> transactionBundle) {
		if (!referencedResourceBundle.getEntryFirstRep().isEmpty()) {
			Bundle.BundleEntryComponent referencedResourceEntry = referencedResourceBundle.getEntry().get(0);
			if (referencedResourceEntry.hasResource() && referencedResourceEntry.getResource() instanceof MetadataResource) {
				MetadataResource referencedResource = (MetadataResource) referencedResourceEntry.getResource();

				createDraftsOfArtifactAndRelated(referencedResource, theRepository, version, transactionBundle);
			}
		}
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
			throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH.REVISION");
		}
	}
    private TreeSet<String> createOwnedResourceUrlCache(List<MetadataResource> resources) {
		TreeSet<String> retval = new TreeSet<String>();
		resources.stream()
			.map(KnowledgeArtifactAdapter::new)
            .map(KnowledgeArtifactAdapter::getOwnedRelatedArtifacts).flatMap(List::stream)
            .filter(RelatedArtifact::hasResource).map(RelatedArtifact::getResource)
			.map(Canonicals::getUrl)
			.forEach(retval::add);
		return retval;
	}
	private void updateUsageContextReferencesWithUrns(MetadataResource newResource, List<MetadataResource> resourceListWithOriginalIds, List<IdType> idListForTransactionBundle) {
		List<UsageContext> useContexts = newResource.getUseContext();
		for (UsageContext useContext : useContexts) {
			// TODO: will we ever need to resolve these references?
			if (useContext.hasValueReference()) {
				Reference useContextRef = useContext.getValueReference();
				if (useContextRef != null) {
					resourceListWithOriginalIds.stream()
						.filter(resource -> (resource.getClass().getSimpleName() + "/" + resource.getIdElement().getIdPart()).equals(useContextRef.getReference()))
						.findAny()
						.ifPresent(resource -> {
							int indexOfDraftInIdList = resourceListWithOriginalIds.indexOf(resource);
							useContext.setValue(new Reference(idListForTransactionBundle.get(indexOfDraftInIdList)));
						});
				}
			}
		}
	}

	private void updateRelatedArtifactUrlsWithNewVersions(List<DependencyInfo> referenceList, String updatedVersion, TreeSet<String> ownedUrlCache){
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
  private List<DependencyInfo> combineComponentsAndDependencies(r4LibraryAdapter adapter) {
		return Stream.concat(adapter.getComponents().stream().map(ra -> convertRelatedArtifact(ra, adapter.getUrl() + "|" + adapter.getVersion())), adapter.getDependencies().stream()).collect(Collectors.toList());
	}
    private DependencyInfo convertRelatedArtifact(RelatedArtifact ra, String source) {
        return new DependencyInfo(source, ra.getResource(), ra.getExtension());
    }
  private BundleEntryComponent createEntry(IBaseResource theResource) {
		BundleEntryComponent entry = new Bundle.BundleEntryComponent()
				.setResource((Resource) theResource)
				.setRequest(createRequest(theResource));
		String fullUrl = entry.getRequest().getUrl();
		if (theResource instanceof MetadataResource) {
			MetadataResource resource = (MetadataResource) theResource;
			if (resource.hasUrl()) {
				fullUrl = resource.getUrl();
				if (resource.hasVersion()) {
					fullUrl += "|" + resource.getVersion();
				}
			}
		}
		entry.setFullUrl(fullUrl);
		return entry;
	}

	private BundleEntryRequestComponent createRequest(IBaseResource theResource) {
		Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
		if (theResource.getIdElement().hasValue() && !theResource.getIdElement().getValue().contains("urn:uuid")) {
			request
				.setMethod(Bundle.HTTPVerb.PUT)
				.setUrl(theResource.getIdElement().getValue());
		} else {
			request
				.setMethod(Bundle.HTTPVerb.POST)
				.setUrl(theResource.fhirType());
		}
		return request;
	}
  /**
 * search by versioned Canonical URL
 * @param theUrl canonical URL of the form www.example.com/Patient/123|0.1
 * @param theRepository to do the searching
 * @return a bundle of results
 */
	private Bundle searchResourceByUrl(String theUrl, Repository theRepository) {
		Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();

		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new UriParam(Canonicals.getUrl(theUrl)));
		searchParams.put("url", urlList);

		List<IQueryParameterType> versionList = new ArrayList<>();
		String version = Canonicals.getVersion(theUrl);
		if (version != null && !version.isEmpty()) {
			versionList.add(new TokenParam(Canonicals.getVersion((theUrl))));
			searchParams.put("version", versionList);
		}

		Bundle searchResultsBundle = theRepository.search(Bundle.class,ResourceClassMapHelper.getClass(Canonicals.getResourceType(theUrl)), searchParams);
		return searchResultsBundle;
	}
}