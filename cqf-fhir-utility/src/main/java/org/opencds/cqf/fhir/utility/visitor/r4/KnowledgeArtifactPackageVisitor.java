package org.opencds.cqf.fhir.utility.visitor.r4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;

import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;


public class KnowledgeArtifactPackageVisitor implements r4KnowledgeArtifactVisitor  {
    // as per http://hl7.org/fhir/R4/resource.html#canonical
	public static final List<ResourceType> canonicalResourceTypes =
    // can't use List.of for Android 26 compatibility
    Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
            ResourceType.ActivityDefinition,
            ResourceType.CapabilityStatement,
            ResourceType.ChargeItemDefinition,
            ResourceType.CompartmentDefinition,
            ResourceType.ConceptMap,
            ResourceType.EffectEvidenceSynthesis,
            ResourceType.EventDefinition,
            ResourceType.Evidence,
            ResourceType.EvidenceVariable,
            ResourceType.ExampleScenario,
            ResourceType.GraphDefinition,
            ResourceType.ImplementationGuide,
            ResourceType.Library,
            ResourceType.Measure,
            ResourceType.MessageDefinition,
            ResourceType.NamingSystem,
            ResourceType.OperationDefinition,
            ResourceType.PlanDefinition,
            ResourceType.Questionnaire,
            ResourceType.ResearchDefinition,
            ResourceType.ResearchElementDefinition,
            ResourceType.RiskEvidenceSynthesis,
            ResourceType.SearchParameter,
            ResourceType.StructureDefinition,
            ResourceType.StructureMap,
            ResourceType.TerminologyCapabilities,
            ResourceType.TestScript,
            ResourceType.ValueSet
        )
    ));

public static final List<ResourceType> conformanceResourceTypes =
// can't use List.of for Android 26 compatibility
Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
            ResourceType.CapabilityStatement,
            ResourceType.StructureDefinition,
            ResourceType.ImplementationGuide,
            ResourceType.SearchParameter,
            ResourceType.MessageDefinition,
            ResourceType.OperationDefinition,
            ResourceType.CompartmentDefinition,
            ResourceType.StructureMap,
            ResourceType.GraphDefinition,
            ResourceType.ExampleScenario
        )
    ));

public static final List<ResourceType> knowledgeArtifactResourceTypes =
// can't use List.of for Android 26 compatibility
Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
            ResourceType.Library,
            ResourceType.Measure,
            ResourceType.ActivityDefinition,
            ResourceType.PlanDefinition
        )
));

public static final List<ResourceType> terminologyResourceTypes =
// can't use List.of for Android 26 compatibility
Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
            ResourceType.CodeSystem,
            ResourceType.ValueSet,
            ResourceType.ConceptMap,
            ResourceType.NamingSystem,
            ResourceType.TerminologyCapabilities
        )
    ));
  public Bundle visit(r4LibraryAdapter library, Repository repository, Parameters packageParameters) {
    Optional<String> artifactRoute = MetadataResourceHelper.getParameter("artifactRoute", packageParameters, UriType.class).map(r -> r.getValue());
    Optional<String> endpointUri = MetadataResourceHelper.getParameter("endpointUri", packageParameters, UriType.class).map(r -> r.getValue());
    Optional<Endpoint> endpoint = MetadataResourceHelper.getResourceParameter("endpoint", packageParameters, Endpoint.class);
    Optional<Endpoint> terminologyEndpoint = MetadataResourceHelper.getResourceParameter("terminologyEndpoint", packageParameters, Endpoint.class);
    Optional<Boolean> packageOnly = MetadataResourceHelper.getParameter("packageOnly", packageParameters, BooleanType.class).map(r -> r.getValue());
    Optional<Integer> count = MetadataResourceHelper.getParameter("count", packageParameters, IntegerType.class).map(r -> r.getValue());
    Optional<Integer> offset = MetadataResourceHelper.getParameter("offset", packageParameters, IntegerType.class).map(r -> r.getValue());
    List<String> include = MetadataResourceHelper.getListParameter("include", packageParameters, StringType.class).map(list -> list.stream().map(r -> r.getValue()).collect(Collectors.toList())).orElseGet(() -> new ArrayList<>());
    List<String> capability = MetadataResourceHelper.getListParameter("capability", packageParameters, StringType.class).map(list -> list.stream().map(r -> r.getValue()).collect(Collectors.toList())).orElseGet(() -> new ArrayList<>());
    List<CanonicalType> artifactVersion = MetadataResourceHelper.getListParameter("artifactVersion", packageParameters, CanonicalType.class).orElseGet(() -> new ArrayList<>());
    List<CanonicalType> checkArtifactVersion = MetadataResourceHelper.getListParameter("checkArtifactVersion", packageParameters, CanonicalType.class).orElseGet(() -> new ArrayList<>());
    List<CanonicalType> forceArtifactVersion = MetadataResourceHelper.getListParameter("forceArtifactVersion", packageParameters, CanonicalType.class).orElseGet(() -> new ArrayList<>());

    if (
				(artifactRoute.isPresent() && !StringUtils.isBlank(artifactRoute.get()) && !artifactRoute.get().isEmpty())
					|| (endpointUri.isPresent() && !StringUtils.isBlank(endpointUri.get()) && !endpointUri.get().isEmpty())
					|| endpoint.isPresent()
					|| terminologyEndpoint.isPresent()
			) {
			throw new NotImplementedOperationException("This repository is not implementing custom Content and Terminology endpoints at this time");
		}
		if (packageOnly.isPresent()) {
			throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
		}
		if (count.isPresent() && count.get() < 0) {
			throw new UnprocessableEntityException("'count' must be non-negative");
		}
		Library resource = library.get();
		// TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a comprehensive manifest
		Bundle packagedBundle = new Bundle();
		if (   include.size() == 1
			&& include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			BundleEntryComponent entry = PackageHelper.createEntry(resource, false);
			packagedBundle.addEntry(entry);
		} else {
			recursivePackage(resource, packagedBundle, repository, capability, include, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			List<BundleEntryComponent> included = findUnsupportedInclude(packagedBundle.getEntry(),include);
			packagedBundle.setEntry(included);
		}
		setCorrectBundleType(count,offset,packagedBundle);
		pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
		handleValueSetReferenceExtensions(resource, packagedBundle.getEntry(), repository);
    return packagedBundle;

    // DependencyInfo --document here that there is a need for figuring out how to determine which package the dependency is in.
    // what is dependency, where did it originate? potentially the package?
  }
  void recursivePackage(
		MetadataResource resource,
		Bundle bundle,
		Repository repository,
		List<String> capability,
		List<String> include,
		List<CanonicalType> artifactVersion,
		List<CanonicalType> checkArtifactVersion,
		List<CanonicalType> forceArtifactVersion
		) throws PreconditionFailedException{
		if (resource != null) {
			r4KnowledgeArtifactAdapter adapter = new AdapterFactory().createKnowledgeArtifactAdapter(resource);
			findUnsupportedCapability(resource, capability);
			processCanonicals(resource, artifactVersion, checkArtifactVersion, forceArtifactVersion);
			boolean entryExists = bundle.getEntry().stream()
				.map(e -> (MetadataResource)e.getResource())
				.filter(mr -> mr.getUrl() != null && mr.getVersion() != null)
				.anyMatch(mr -> mr.getUrl().equals(resource.getUrl()) && mr.getVersion().equals(resource.getVersion()));
			if (!entryExists) {
				BundleEntryComponent entry = PackageHelper.createEntry(resource, false);
				bundle.addEntry(entry);
			}

			adapter.combineComponentsAndDependencies().stream()
				.map(ra -> (Bundle) SearchHelper.searchRepositoryByCanonicalWithPaging(repository, new CanonicalType(ra.getReference())))
				.map(searchBundle -> searchBundle.getEntry().stream().findFirst().orElseGet(()-> new BundleEntryComponent()).getResource())
				.forEach(component -> recursivePackage((MetadataResource)component, bundle, repository, capability, include, artifactVersion, checkArtifactVersion, forceArtifactVersion));
		}
	}
  public IBase visit(IBaseLibraryAdapter library, Repository repository, IBaseParameters draftParameters){
    return new OperationOutcome();
  }
  public IBase visit(IBaseKnowledgeArtifactAdapter library, Repository repository, IBaseParameters draftParameters){
    return new OperationOutcome();
  }
//   public IBase visit(r4KnowledgeArtifactAdapter library, Repository repository, IBaseParameters draftParameters){
//     return new OperationOutcome();
//   }
  public IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = planDefinition.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }
  private void findUnsupportedCapability(MetadataResource resource, List<String> capability) throws PreconditionFailedException{
    if (capability != null && !capability.isEmpty()) {
        List<Extension> knowledgeCapabilityExtension = resource.getExtension().stream()
        .filter(ext -> ext.getUrl().contains("cqf-knowledgeCapability"))
        .collect(Collectors.toList());
        if (knowledgeCapabilityExtension.isEmpty()) {
            // consider resource unsupported if it's knowledgeCapability is undefined
            throw new PreconditionFailedException(String.format("Resource with url: '%s' does not specify capability.", resource.getUrl()));
        }
        knowledgeCapabilityExtension.stream()
            .filter(ext -> !capability.contains(((CodeType) ext.getValue()).getValue()))
            .findAny()
            .ifPresent((ext) -> {
                throw new PreconditionFailedException(String.format("Resource with url: '%s' is not one of '%s'.",
                resource.getUrl(),
                String.join(", ", capability)));
            });
    }
}
private void processCanonicals(MetadataResource resource, List<CanonicalType> canonicalVersion,  List<CanonicalType> checkArtifactVersion,  List<CanonicalType> forceArtifactVersion) throws PreconditionFailedException {
    if (checkArtifactVersion != null && !checkArtifactVersion.isEmpty()) {
        // check throws an error
        findVersionInListMatchingResource(checkArtifactVersion, resource)
            .ifPresent((version) -> {
                if (!resource.getVersion().equals(version)) {
                    throw new PreconditionFailedException(String.format("Resource with url '%s' has version '%s' but checkVersion specifies '%s'",
                    resource.getUrl(),
                    resource.getVersion(),
                    version
                    ));
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
private Optional<String> findVersionInListMatchingResource(List<CanonicalType> list, MetadataResource resource){
    return list.stream()
                .filter((canonical) -> Canonicals.getUrl(canonical).equals(resource.getUrl()))
                .map((canonical) -> Canonicals.getVersion(canonical))
                .findAny();
}
private void setCorrectBundleType(Optional<Integer> count, Optional<Integer> offset, Bundle bundle) {
    // if the bundle is paged then it must be of type = collection and modified to follow bundle.type constraints
    // if not, set type = transaction
    // special case of count = 0 -> set type = searchset so we can display bundle.total
    if (count.isPresent() && count.get() == 0) {
        bundle.setType(BundleType.SEARCHSET);
        bundle.setTotal(bundle.getEntry().size());
    } else if (
        (offset.isPresent() && offset.get() > 0) || 
        (count.isPresent() && count.get() < bundle.getEntry().size())
    ) {
        bundle.setType(BundleType.COLLECTION);
        List<BundleEntryComponent> removedRequest = bundle.getEntry().stream()
            .map(entry -> {
                entry.setRequest(null);
                return entry;
            }).collect(Collectors.toList());
        bundle.setEntry(removedRequest);
    } else {
        bundle.setType(BundleType.TRANSACTION);
    }
}
/**
 * $package allows for a bundle to be paged
 * @param count the maximum number of resources to be returned
 * @param offset the number of resources to skip beginning from the start of the bundle (starts from 1)
 * @param bundle the bundle to page
 */
private void pageBundleBasedOnCountAndOffset(Optional<Integer> count, Optional<Integer> offset, Bundle bundle) {
    if (offset.isPresent()) {
        List<BundleEntryComponent> entries = bundle.getEntry();
        Integer bundleSize = entries.size();
        if (offset.get() < bundleSize) {
            bundle.setEntry(entries.subList(offset.get(), bundleSize));
        } else {
            bundle.setEntry(Arrays.asList());
        }
    }
    if (count.isPresent()) {
        // repeat these two from earlier because we might modify / replace the entries list at any time
        List<BundleEntryComponent> entries = bundle.getEntry();
        Integer bundleSize = entries.size();
        if (count.get() < bundleSize){
            bundle.setEntry(entries.subList(0, count.get()));
        } else {
            // there are not enough entries in the bundle to page, so we return all of them no change
        }
    }
}

private List<BundleEntryComponent> findUnsupportedInclude(List<BundleEntryComponent> entries, List<String> include) {
    if (include == null || include.isEmpty() || include.stream().anyMatch((includedType) -> includedType.equals("all"))) {
        return entries;
    }
    List<BundleEntryComponent> filteredList = new ArrayList<>();
    entries.stream().forEach(entry -> {
        if (include.stream().anyMatch((type) -> type.equals("knowledge"))) {
            Boolean resourceIsKnowledgeType = knowledgeArtifactResourceTypes.contains(entry.getResource().getResourceType());
            if (resourceIsKnowledgeType) {
                filteredList.add(entry);
            }
        }
        if (include.stream().anyMatch((type) -> type.equals("canonical"))) {
            Boolean resourceIsCanonicalType = canonicalResourceTypes.contains(entry.getResource().getResourceType());
            if (resourceIsCanonicalType) {
                filteredList.add(entry);
            }
        }
        if (include.stream().anyMatch((type) -> type.equals("terminology"))) {
            Boolean resourceIsTerminologyType = terminologyResourceTypes.contains(entry.getResource().getResourceType());
            if (resourceIsTerminologyType) {
                filteredList.add(entry);
            }
        }
        if (include.stream().anyMatch((type) -> type.equals("conformance"))) {
            Boolean resourceIsConformanceType = conformanceResourceTypes.contains(entry.getResource().getResourceType());
            if (resourceIsConformanceType) {
                filteredList.add(entry);
            }
        }
        if (include.stream().anyMatch((type) -> type.equals("extensions"))
            && entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
            && ((StructureDefinition) entry.getResource()).getType().equals("Extension")) {
                filteredList.add(entry);
        }
        if (include.stream().anyMatch((type) -> type.equals("profiles"))
            && entry.getResource().getResourceType().equals(ResourceType.StructureDefinition)
            && !((StructureDefinition) entry.getResource()).getType().equals("Extension")) {
                filteredList.add(entry);
        }
        if (include.stream().anyMatch((type) -> type.equals("tests"))){
            if (entry.getResource().getResourceType().equals(ResourceType.Library)
                && ((Library) entry.getResource()).getType().getCoding().stream().anyMatch(coding -> coding.getCode().equals("test-case"))) {
                filteredList.add(entry);
            } else if (((MetadataResource) entry.getResource()).getExtension().stream().anyMatch(ext -> ext.getUrl().contains("isTestCase")
                && ((BooleanType) ext.getValue()).getValue())) {
                filteredList.add(entry);
            }
        }
        if (include.stream().anyMatch((type) -> type.equals("examples"))){
            // TODO: idk if this is legit just a placeholder for now
            if (((MetadataResource) entry.getResource()).getExtension().stream().anyMatch(ext -> ext.getUrl().contains("isExample")
                && ((BooleanType) ext.getValue()).getValue())) {
                filteredList.add(entry);
            }
        }
    });
    List<BundleEntryComponent> distinctFilteredEntries = new ArrayList<>();
    // remove duplicates
    for (BundleEntryComponent entry: filteredList) {
        if (!distinctFilteredEntries.stream()
            .map((e) -> ((MetadataResource) e.getResource()))
            .anyMatch(existingEntry -> existingEntry.getUrl().equals(((MetadataResource) entry.getResource()).getUrl()) && existingEntry.getVersion().equals(((MetadataResource) entry.getResource()).getVersion()))
        ) {
            distinctFilteredEntries.add(entry);
        }
    }
    return distinctFilteredEntries;
}
/**
	 * ValueSets can be part of multiple artifacts at the same time. Certain properties are tracked/managed in the manifest to avoid conflicts with other artifacts. This function sets those properties on the ValueSets themselves at export / $package time
	 * @param manifest the resource containing all RelatedArtifact references
	 * @param bundleEntries the list of packaged resources to modify according to the extensions on the manifest relatedArtifact references
	 */
	private void handleValueSetReferenceExtensions(MetadataResource manifest, List<BundleEntryComponent> bundleEntries, Repository repository) throws UnprocessableEntityException, IllegalArgumentException {
		r4KnowledgeArtifactAdapter adapter = new AdapterFactory().createKnowledgeArtifactAdapter(manifest);
		List<DependencyInfo> relatedArtifactsWithPreservedExtension = getRelatedArtifactsWithPreservedExtensions(adapter.getDependencies());
		bundleEntries.stream()
			.forEach(entry -> {
				if (entry.getResource().getResourceType().equals(ResourceType.ValueSet)) {
					ValueSet valueSet = (ValueSet) entry.getResource();
					// remove any existing Priority and Conditions
					List<UsageContext> usageContexts = removeExistingReferenceExtensionData(valueSet.getUseContext());
					valueSet.setUseContext(usageContexts);
					Optional<DependencyInfo> maybeVSRelatedArtifact = relatedArtifactsWithPreservedExtension.stream().filter(ra -> Canonicals.getUrl(ra.getReference()).equals(valueSet.getUrl())).findFirst();
					// If leaf valueset
					if (!valueSet.hasCompose()
					 || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0)) {
						// If Condition extension is present
						maybeVSRelatedArtifact
							.map(ra -> ra.getExtension())
							.ifPresent(
								// add Conditions
								exts -> {
									exts.stream()
										.filter(ext -> ext.getUrl().equalsIgnoreCase(IBaseKnowledgeArtifactAdapter.valueSetConditionUrl))
										.forEach(ext -> tryAddCondition(usageContexts, (CodeableConcept) ext.getValue()));
								});		
					}
					// update Priority
					UsageContext priority = getOrCreateUsageContext(usageContexts, IBaseKnowledgeArtifactAdapter.usPhContextTypeUrl, IBaseKnowledgeArtifactAdapter.valueSetPriorityCode);
					Optional<? extends IBaseExtension> maybeExtension = maybeVSRelatedArtifact
						.flatMap(ra -> ra.getExtension().stream().filter(ext -> ext.getUrl().equals(IBaseKnowledgeArtifactAdapter.valueSetPriorityUrl)).findFirst());
					if (maybeExtension.isPresent()) {
                        priority.setValue(((Extension)maybeExtension.get()).getValue());
                    } else {
                        CodeableConcept routine = new CodeableConcept(new Coding(IBaseKnowledgeArtifactAdapter.contextUrl, "routine", null)).setText("Routine");
                        priority.setValue(routine);
                    }
				}
			});
	}
    private List<DependencyInfo> getRelatedArtifactsWithPreservedExtensions(List<DependencyInfo> deps) {
		return deps.stream()
			.filter(ra -> IBaseKnowledgeArtifactAdapter.preservedExtensionUrls
				.stream().anyMatch(url -> ra.getExtension()
					.stream().anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
			.collect(Collectors.toList());
	}
    /**
	 * Removes any existing UsageContexts corresponding the the VSM specific extensions
	 * @param usageContexts the list of usage contexts to modify
	 */
	private List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
		// can't use List.of for Android 26 compatibility
        List<String> useContextCodesToReplace = Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(IBaseKnowledgeArtifactAdapter.valueSetConditionCode,IBaseKnowledgeArtifactAdapter.valueSetPriorityCode)));
		return usageContexts.stream()
		// remove any useContexts which need to be replaced
			.filter(useContext -> !useContextCodesToReplace.stream()
				.anyMatch(code -> useContext.getCode().getCode().equals(code)))
			.collect(Collectors.toList());
	}
    private void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
		boolean focusAlreadyExists = usageContexts.stream().anyMatch(u -> 
			u.getCode().getSystem().equals(IBaseKnowledgeArtifactAdapter.contextTypeUrl) 
			&& u.getCode().getCode().equals(IBaseKnowledgeArtifactAdapter.valueSetConditionCode) 
			&& u.getValueCodeableConcept().hasCoding(condition.getCoding().get(0).getSystem(), condition.getCoding().get(0).getCode())
		);
		if (!focusAlreadyExists) {
			UsageContext newFocus = new UsageContext(new Coding(IBaseKnowledgeArtifactAdapter.contextTypeUrl,IBaseKnowledgeArtifactAdapter.valueSetConditionCode,null),condition);
			newFocus.setValue(condition);
			usageContexts.add(newFocus);
		}
	}
	/**
	 * 
	 * Either finds a usageContext with the same system and code or creates an empty one
	 * and appends it
	 * 
	 * @param usageContexts the list of usageContexts to search and/or append to
	 * @param system the usageContext.code.system to find / create
	 * @param code the usage.code.code to find / create
	 * @return the found / created usageContext
	 */
	private UsageContext getOrCreateUsageContext(List<UsageContext> usageContexts, String system, String code) {
		return usageContexts.stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(system) && useContext.getCode().getCode().equals(code))
			.findFirst().orElseGet(()-> {
				// create the UseContext if it doesn't exist
				Coding c = new Coding(system, code, null);
				UsageContext n = new UsageContext(c, null);
				// add it to the ValueSet before returning
				usageContexts.add(n);
				return n;
			});
	}
}