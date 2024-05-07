package org.opencds.cqf.fhir.utility.visitor.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.UsageContext;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.ResourceClassMapHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

public class KnowledgeArtifactPackageVisitor {

    public KnowledgeArtifactPackageVisitor() {
        this.terminologyServerClient = new TerminologyServerClient(FhirContext.forDstu3());
    }

    private TerminologyServerClient terminologyServerClient;

    // as per http://hl7.org/fhir/dstu3/resource.html#canonical
    public static final List<ResourceType> canonicalResourceTypes =
            // can't use List.of for Android 26 compatibility
            Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
                    ResourceType.ActivityDefinition,
                    ResourceType.CapabilityStatement,
                    ResourceType.CompartmentDefinition,
                    ResourceType.ConceptMap,
                    ResourceType.GraphDefinition,
                    ResourceType.ImplementationGuide,
                    ResourceType.Library,
                    ResourceType.Measure,
                    ResourceType.MessageDefinition,
                    ResourceType.NamingSystem,
                    ResourceType.OperationDefinition,
                    ResourceType.PlanDefinition,
                    ResourceType.Questionnaire,
                    ResourceType.SearchParameter,
                    ResourceType.StructureDefinition,
                    ResourceType.StructureMap,
                    ResourceType.TestScript,
                    ResourceType.ValueSet)));

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
                    ResourceType.GraphDefinition)));

    public static final List<ResourceType> knowledgeArtifactResourceTypes =
            // can't use List.of for Android 26 compatibility
            Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
                    ResourceType.Library,
                    ResourceType.Measure,
                    ResourceType.ActivityDefinition,
                    ResourceType.PlanDefinition)));

    public static final List<ResourceType> terminologyResourceTypes =
            // can't use List.of for Android 26 compatibility
            Collections.unmodifiableList(new ArrayList<ResourceType>(Arrays.asList(
                    ResourceType.CodeSystem,
                    ResourceType.ValueSet,
                    ResourceType.ConceptMap,
                    ResourceType.NamingSystem)));

    public static void setCorrectBundleType(Optional<Integer> count, Optional<Integer> offset, Bundle bundle) {
        // if the bundle is paged then it must be of type = collection and modified to follow bundle.type constraints
        // if not, set type = transaction
        // special case of count = 0 -> set type = searchset so we can display bundle.total
        if (count.isPresent() && count.get() == 0) {
            bundle.setType(BundleType.SEARCHSET);
            bundle.setTotal(bundle.getEntry().size());
        } else if ((offset.isPresent() && offset.get() > 0)
                || (count.isPresent() && count.get() < bundle.getEntry().size())) {
            bundle.setType(BundleType.COLLECTION);
            List<BundleEntryComponent> removedRequest = bundle.getEntry().stream()
                    .map(entry -> {
                        entry.setRequest(null);
                        return entry;
                    })
                    .collect(Collectors.toList());
            bundle.setEntry(removedRequest);
        } else {
            bundle.setType(BundleType.TRANSACTION);
        }
    }

    public static List<BundleEntryComponent> findUnsupportedInclude(
            List<BundleEntryComponent> entries, List<String> include) {
        if (include == null
                || include.isEmpty()
                || include.stream().anyMatch((includedType) -> includedType.equals("all"))) {
            return entries;
        }
        List<BundleEntryComponent> filteredList = new ArrayList<>();
        entries.stream().forEach(entry -> {
            if (include.stream().anyMatch((type) -> type.equals("knowledge"))) {
                Boolean resourceIsKnowledgeType = knowledgeArtifactResourceTypes.contains(
                        entry.getResource().getResourceType());
                if (resourceIsKnowledgeType) {
                    filteredList.add(entry);
                }
            }
            if (include.stream().anyMatch((type) -> type.equals("canonical"))) {
                Boolean resourceIsCanonicalType =
                        canonicalResourceTypes.contains(entry.getResource().getResourceType());
                if (resourceIsCanonicalType) {
                    filteredList.add(entry);
                }
            }
            if (include.stream().anyMatch((type) -> type.equals("terminology"))) {
                Boolean resourceIsTerminologyType =
                        terminologyResourceTypes.contains(entry.getResource().getResourceType());
                if (resourceIsTerminologyType) {
                    filteredList.add(entry);
                }
            }
            if (include.stream().anyMatch((type) -> type.equals("conformance"))) {
                Boolean resourceIsConformanceType =
                        conformanceResourceTypes.contains(entry.getResource().getResourceType());
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
            if (include.stream().anyMatch((type) -> type.equals("tests"))) {
                if (entry.getResource().getResourceType().equals(ResourceType.Library)
                        && ((Library) entry.getResource())
                                .getType().getCoding().stream()
                                        .anyMatch(coding -> coding.getCode().equals("test-case"))) {
                    filteredList.add(entry);
                } else if (((MetadataResource) entry.getResource())
                        .getExtension().stream()
                                .anyMatch(ext -> ext.getUrl().contains("isTestCase")
                                        && ((BooleanType) ext.getValue()).getValue())) {
                    filteredList.add(entry);
                }
            }
            if (include.stream().anyMatch((type) -> type.equals("examples"))) {
                // TODO: idk if this is legit just a placeholder for now
                if (((MetadataResource) entry.getResource())
                        .getExtension().stream()
                                .anyMatch(ext -> ext.getUrl().contains("isExample")
                                        && ((BooleanType) ext.getValue()).getValue())) {
                    filteredList.add(entry);
                }
            }
        });
        List<BundleEntryComponent> distinctFilteredEntries = new ArrayList<>();
        // remove duplicates
        for (BundleEntryComponent entry : filteredList) {
            if (!distinctFilteredEntries.stream()
                    .map((e) -> ((MetadataResource) e.getResource()))
                    .anyMatch(existingEntry ->
                            existingEntry.getUrl().equals(((MetadataResource) entry.getResource()).getUrl())
                                    && existingEntry
                                            .getVersion()
                                            .equals(((MetadataResource) entry.getResource()).getVersion()))) {
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
    public void handleValueSetReferenceExtensions(
            MetadataResource manifest,
            List<BundleEntryComponent> bundleEntries,
            Repository repository,
            Optional<IBaseResource> terminologyEndpoint)
            throws UnprocessableEntityException, IllegalArgumentException {
        KnowledgeArtifactAdapter adapter = AdapterFactory.forFhirVersion(manifest.getStructureFhirVersionEnum())
                .createKnowledgeArtifactAdapter(manifest);
        List<RelatedArtifact> relatedArtifactsWithPreservedExtension =
                getRelatedArtifactsWithPreservedExtensions(adapter.getRelatedArtifact());
        Parameters expansionParams = new Parameters();
        Library rootSpecificationLibrary = getRootSpecificationLibrary(bundleEntries);
        if (rootSpecificationLibrary != null) {
            Extension expansionParamsExtension =
                    rootSpecificationLibrary.getExtensionByUrl(KnowledgeArtifactAdapter.expansionParametersUrl);
            if (expansionParamsExtension != null && expansionParamsExtension.hasValue()) {
                Reference expansionReference = (Reference) expansionParamsExtension.getValue();
                expansionParams = getExpansionParams(rootSpecificationLibrary, expansionReference.getReference());
            }
        }
        Parameters params = expansionParams;
        bundleEntries.stream().forEach(entry -> {
            if (entry.getResource().getResourceType().equals(ResourceType.ValueSet)) {
                ValueSet valueSet = (ValueSet) entry.getResource();
                // remove any existing Priority and Conditions
                List<UsageContext> usageContexts = removeExistingReferenceExtensionData(valueSet.getUseContext());
                valueSet.setUseContext(usageContexts);
                Optional<RelatedArtifact> maybeVSRelatedArtifact = relatedArtifactsWithPreservedExtension.stream()
                        .filter(ra -> Canonicals.getUrl(ra.getResource().getReference())
                                .equals(valueSet.getUrl()))
                        .findFirst();
                checkIfValueSetNeedsCondition(valueSet, maybeVSRelatedArtifact.orElse(null), repository);
                // If leaf valueset
                if (!valueSet.hasCompose()
                        || (valueSet.hasCompose()
                                && valueSet.getCompose()
                                        .getIncludeFirstRep()
                                        .getValueSet()
                                        .isEmpty())) {
                    expandValueSet(valueSet, params, terminologyEndpoint);
                    // If Condition extension is present
                    maybeVSRelatedArtifact
                            .map(ra -> ra.getExtension())
                            .ifPresent(
                                    // add Conditions
                                    exts -> {
                                        exts.stream()
                                                .filter(ext -> ext.getUrl()
                                                        .equalsIgnoreCase(
                                                                KnowledgeArtifactAdapter.valueSetConditionUrl))
                                                .forEach(ext -> tryAddCondition(
                                                        usageContexts, (CodeableConcept) ext.getValue()));
                                    });
                }
                // update Priority
                UsageContext priority = getOrCreateUsageContext(
                        usageContexts,
                        KnowledgeArtifactAdapter.usPhContextTypeUrl,
                        KnowledgeArtifactAdapter.valueSetPriorityCode);
                Optional<Extension> ext = maybeVSRelatedArtifact.map(
                        ra -> ra.getExtensionByUrl(KnowledgeArtifactAdapter.valueSetPriorityUrl));
                if (ext.isPresent()) {
                    priority.setValue(ext.get().getValue());
                } else {
                    CodeableConcept routine = new CodeableConcept(
                                    new Coding(KnowledgeArtifactAdapter.contextUrl, "routine", null))
                            .setText("Routine");
                    priority.setValue(routine);
                }
            }
        });
    }

    public void expandValueSet(
            ValueSet valueSet, Parameters expansionParameters, Optional<IBaseResource> terminologyEndpoint) {
        // Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
        Extension authoritativeSource = valueSet.getExtensionByUrl(KnowledgeArtifactAdapter.authoritativeSourceUrl);
        String authoritativeSourceUrl = authoritativeSource != null && authoritativeSource.hasValue()
                ? authoritativeSource.getValue().primitiveValue()
                : valueSet.getUrl();

        ValueSet expandedValueSet;
        if (isVSMAuthoredValueSet(valueSet) && hasSimpleCompose(valueSet)) {
            // Perform naive expansion independent of terminology servers. Copy all codes from compose into expansion.
            ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
            expansion.setTimestamp(Date.from(Instant.now()));

            ArrayList<ValueSet.ValueSetExpansionParameterComponent> expansionParams = new ArrayList<>();
            ValueSet.ValueSetExpansionParameterComponent parameterNaive =
                    new ValueSet.ValueSetExpansionParameterComponent();
            parameterNaive.setName("naive");
            parameterNaive.setValue(new BooleanType(true));
            expansionParams.add(parameterNaive);
            expansion.setParameter(expansionParams);

            for (ValueSet.ConceptSetComponent csc : valueSet.getCompose().getInclude()) {
                for (ValueSet.ConceptReferenceComponent crc : csc.getConcept()) {
                    expansion
                            .addContains()
                            .setCode(crc.getCode())
                            .setSystem(csc.getSystem())
                            .setVersion(csc.getVersion())
                            .setDisplay(crc.getDisplay());
                }
            }
            valueSet.setExpansion(expansion);
        } else {
            Optional<StringType> username = Optional.empty();
            Optional<StringType> apiKey = Optional.empty();
            if (terminologyEndpoint.isPresent()) {
                Endpoint endPnt = (Endpoint) terminologyEndpoint.get();
                username = endPnt.getHeader().stream()
                        .filter(h -> h.getId().equals("username"))
                        .findFirst();
                apiKey = endPnt.getHeader().stream()
                        .filter(h -> h.getId().equals("apiKey"))
                        .findFirst();
            }

            if (!username.isPresent() || !apiKey.isPresent()) {
                throw new UnprocessableEntityException(
                        "Cannot expand ValueSet without credentials: " + valueSet.getId());
            }
            try {
                expandedValueSet = terminologyServerClient.expand(
                        valueSet,
                        authoritativeSourceUrl,
                        expansionParameters,
                        username.get().getValue(),
                        apiKey.get().getValue());
                valueSet.setExpansion(expandedValueSet.getExpansion());
            } catch (Exception ex) {
                System.out.println("Terminology Server expansion failed: {"
                        + valueSet.getIdElement().getValue() + "}");
            }
        }
    }

    public boolean isVSMAuthoredValueSet(ValueSet valueSet) {
        return valueSet.hasMeta()
                && valueSet.getMeta().hasTag()
                && valueSet.getMeta()
                                .getTag(
                                        KnowledgeArtifactAdapter.vsmWorkflowCodesCodeSystemUrl,
                                        KnowledgeArtifactAdapter.vsmValueSetTagVSMAuthoredCode)
                        != null;
    }

    // A simple compose element of a ValueSet must have a compose without an exclude element. Each
    // element of the include cannot reference a value set or have a filter, and must have a system
    // and enumerate concepts
    public boolean hasSimpleCompose(ValueSet valueSet) {
        return valueSet.hasCompose()
                && !valueSet.getCompose().hasExclude()
                && valueSet.getCompose().getInclude().stream()
                        .noneMatch(
                                csc -> csc.hasValueSet() || csc.hasFilter() || !csc.hasSystem() || !csc.hasConcept());
    }

    private List<RelatedArtifact> getRelatedArtifactsWithPreservedExtensions(List<RelatedArtifact> deps) {
        return deps.stream()
                .filter(ra -> KnowledgeArtifactAdapter.preservedExtensionUrls.stream()
                        .anyMatch(url -> ra.getExtension().stream()
                                .anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
                .collect(Collectors.toList());
    }

    private static Library getRootSpecificationLibrary(List<Bundle.BundleEntryComponent> bundleEntries) {
        Optional<Library> rootSpecLibrary = bundleEntries.stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.Library)
                .map(entry -> ((Library) entry.getResource()))
                .filter(entry -> entry.getType()
                                .hasCoding(
                                        KnowledgeArtifactAdapter.libraryType, KnowledgeArtifactAdapter.assetCollection)
                        && entry.getUseContext().stream()
                                .allMatch(useContext -> (useContext
                                                        .getCode()
                                                        .getSystem()
                                                        .equals(KnowledgeArtifactAdapter.usPhContextTypeUrl)
                                                && useContext
                                                        .getCode()
                                                        .getCode()
                                                        .equals("reporting")
                                                && useContext
                                                        .getValueCodeableConcept()
                                                        .hasCoding(
                                                                KnowledgeArtifactAdapter.usPhContextUrl, "triggering"))
                                        || (useContext
                                                        .getCode()
                                                        .getSystem()
                                                        .equals(KnowledgeArtifactAdapter.usPhContextTypeUrl)
                                                && useContext
                                                        .getCode()
                                                        .getCode()
                                                        .equals("specification-type")
                                                && useContext
                                                        .getValueCodeableConcept()
                                                        .hasCoding(
                                                                KnowledgeArtifactAdapter.usPhContextUrl, "program"))))
                .findFirst();
        return rootSpecLibrary.orElse(null);
    }

    private static Parameters getExpansionParams(Library rootSpecificationLibrary, String reference) {
        Optional<Resource> expansionParamResource = rootSpecificationLibrary.getContained().stream()
                .filter(contained -> contained.getId().equals(reference))
                .findFirst();
        return (Parameters) expansionParamResource.orElse(null);
    }

    private void checkIfValueSetNeedsCondition(
            MetadataResource resource, RelatedArtifact relatedArtifact, Repository repository)
            throws UnprocessableEntityException {
        if (resource == null
                && relatedArtifact != null
                && relatedArtifact.hasResource()
                && Canonicals.getResourceType(relatedArtifact.getResource().getReference())
                        .equals("ValueSet")) {
            List<MetadataResource> searchResults = getResourcesFromBundle(
                    searchResourceByUrl(relatedArtifact.getResource().getReference(), repository));
            if (searchResults.size() > 0) {
                resource = searchResults.get(0);
            }
        }
        if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
            ValueSet valueSet = (ValueSet) resource;
            boolean isLeaf = !valueSet.hasCompose()
                    || (valueSet.hasCompose()
                            && valueSet.getCompose()
                                            .getIncludeFirstRep()
                                            .getValueSet()
                                            .size()
                                    == 0);
            Optional<Extension> maybeConditionExtension = Optional.ofNullable(relatedArtifact)
                    .map(RelatedArtifact::getExtension)
                    .map(list -> {
                        return list.stream()
                                .filter(ext ->
                                        ext.getUrl().equalsIgnoreCase(KnowledgeArtifactAdapter.valueSetConditionUrl))
                                .findFirst()
                                .orElse(null);
                    });
            Optional<Extension> maybePriorityExtension = Optional.ofNullable(relatedArtifact)
                    .map(RelatedArtifact::getExtension)
                    .map(list -> {
                        return list.stream()
                                .filter(ext ->
                                        ext.getUrl().equalsIgnoreCase(KnowledgeArtifactAdapter.valueSetPriorityUrl))
                                .findFirst()
                                .orElse(null);
                    });
            if (isLeaf && (!maybeConditionExtension.isPresent() || !maybePriorityExtension.isPresent())) {
                if (!maybeConditionExtension.isPresent() && !maybePriorityExtension.isPresent()) {
                    throw new UnprocessableEntityException(
                            "Missing condition and priority references on ValueSet : " + valueSet.getUrl());
                } else if (!maybeConditionExtension.isPresent()) {
                    throw new UnprocessableEntityException(
                            "Missing condition reference on ValueSet : " + valueSet.getUrl());
                } else {
                    throw new UnprocessableEntityException(
                            "Missing priority reference on ValueSet : " + valueSet.getUrl());
                }
            }
        }
    }

    private List<MetadataResource> getResourcesFromBundle(Bundle bundle) {
        List<MetadataResource> resourceList = new ArrayList<>();

        if (!bundle.getEntryFirstRep().isEmpty()) {
            List<Bundle.BundleEntryComponent> referencedResourceEntries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : referencedResourceEntries) {
                if (entry.hasResource() && entry.getResource() instanceof MetadataResource) {
                    MetadataResource referencedResource = (MetadataResource) entry.getResource();
                    resourceList.add(referencedResource);
                }
            }
        }

        return resourceList;
    }

    /**
     * search by versioned Canonical URL
     * @param url canonical URL of the form www.example.com/Patient/123|0.1
     * @param repository to do the searching
     * @return a bundle of results
     */
    private Bundle searchResourceByUrl(String url, Repository repository) {
        Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();

        List<IQueryParameterType> urlList = new ArrayList<>();
        urlList.add(new UriParam(Canonicals.getUrl(url)));
        searchParams.put("url", urlList);

        List<IQueryParameterType> versionList = new ArrayList<>();
        String version = Canonicals.getVersion(url);
        if (version != null && !version.isEmpty()) {
            versionList.add(new TokenParam(Canonicals.getVersion((url))));
            searchParams.put("version", versionList);
        }

        Bundle searchResultsBundle = (Bundle) repository.search(
                Bundle.class, ResourceClassMapHelper.getClass(Canonicals.getResourceType(url)), searchParams);
        return searchResultsBundle;
    }

    /**
     * Removes any existing UsageContexts corresponding to the VSM specific extensions
     * @param usageContexts the list of usage contexts to modify
     */
    private List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
        List<String> useContextCodesToReplace = Collections.unmodifiableList(Arrays.asList(
                KnowledgeArtifactAdapter.valueSetConditionCode, KnowledgeArtifactAdapter.valueSetPriorityCode));
        return usageContexts.stream()
                // remove any useContexts which need to be replaced
                .filter(useContext -> !useContextCodesToReplace.stream()
                        .anyMatch(code -> useContext.getCode().getCode().equals(code)))
                .collect(Collectors.toList());
    }

    private void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
        boolean focusAlreadyExists = usageContexts.stream()
                .anyMatch(u -> u.getCode().getSystem().equals(KnowledgeArtifactAdapter.contextTypeUrl)
                        && u.getCode().getCode().equals(KnowledgeArtifactAdapter.valueSetConditionCode)
                        && u.getValueCodeableConcept()
                                .hasCoding(
                                        condition.getCoding().get(0).getSystem(),
                                        condition.getCoding().get(0).getCode()));
        if (!focusAlreadyExists) {
            UsageContext newFocus = new UsageContext(
                    new Coding(
                            KnowledgeArtifactAdapter.contextUrl, KnowledgeArtifactAdapter.valueSetConditionCode, null),
                    condition);
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
                .filter(useContext -> useContext.getCode().getSystem().equals(system)
                        && useContext.getCode().getCode().equals(code))
                .findFirst()
                .orElseGet(() -> {
                    // create the UseContext if it doesn't exist
                    Coding c = new Coding(system, code, null);
                    UsageContext n = new UsageContext(c, null);
                    // add it to the ValueSet before returning
                    usageContexts.add(n);
                    return n;
                });
    }
}
