package org.opencds.cqf.fhir.utility.visitor.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.EndpointCredentials;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

public class KnowledgeArtifactPackageVisitor {

    public KnowledgeArtifactPackageVisitor() {
        this.terminologyServerClient = new TerminologyServerClient(FhirContext.forR4());
    }

    private TerminologyServerClient terminologyServerClient;

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
                    ResourceType.GraphDefinition,
                    ResourceType.ExampleScenario)));

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
                    ResourceType.NamingSystem,
                    ResourceType.TerminologyCapabilities)));

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
                    rootSpecificationLibrary.getExtensionByUrl(Constants.EXPANSION_PARAMETERS_URL);
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
                        .filter(ra -> Canonicals.getUrl(ra.getResource()).equals(valueSet.getUrl()))
                        .findFirst();
                checkIfValueSetNeedsCondition(valueSet, maybeVSRelatedArtifact.orElse(null));
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
                                                        .equalsIgnoreCase(Constants.VALUE_SET_CONDITION_URL))
                                                .forEach(ext -> tryAddCondition(
                                                        usageContexts, (CodeableConcept) ext.getValue()));
                                    });
                }
                // update Priority
                UsageContext priority = getOrCreateUsageContext(
                        usageContexts, KnowledgeArtifactAdapter.usPhContextTypeUrl, Constants.VALUE_SET_PRIORITY_CODE);
                Optional<Extension> ext =
                        maybeVSRelatedArtifact.map(ra -> ra.getExtensionByUrl(Constants.VALUE_SET_PRIORITY_URL));

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

    protected void expandValueSet(
            ValueSet valueSet, Parameters expansionParameters, Optional<IBaseResource> terminologyEndpoint) {
        // Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
        Extension authoritativeSource = valueSet.getExtensionByUrl(Constants.AUTHORITATIVE_SOURCE_URL);
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
                EndpointCredentials endPnt = (EndpointCredentials) terminologyEndpoint.get();
                username = Optional.of(endPnt.getVsacUsername());
                apiKey = Optional.of(endPnt.getApiKey());
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

    protected boolean isVSMAuthoredValueSet(ValueSet valueSet) {
        return valueSet.hasMeta()
                && valueSet.getMeta().hasTag()
                && valueSet.getMeta()
                                .getTag(
                                        Constants.VSM_WORKFLOW_CODES_CODE_SYSTEM_URL,
                                        Constants.VSM_VALUE_SET_TAG_VSM_AUTHORED_CODE)
                        != null;
    }

    // A simple compose element of a ValueSet must have a compose without an exclude element. Each
    // element of the include cannot reference a value set or have a filter, and must have a system
    // and enumerate concepts
    protected boolean hasSimpleCompose(ValueSet valueSet) {
        return valueSet.hasCompose()
                && !valueSet.getCompose().hasExclude()
                && valueSet.getCompose().getInclude().stream()
                        .noneMatch(
                                csc -> csc.hasValueSet() || csc.hasFilter() || !csc.hasSystem() || !csc.hasConcept());
    }

    protected List<RelatedArtifact> getRelatedArtifactsWithPreservedExtensions(List<RelatedArtifact> deps) {
        return deps.stream()
                .filter(ra -> Constants.PRESERVED_EXTENSION_URLS.stream().anyMatch(url -> ra.getExtension().stream()
                        .anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
                .collect(Collectors.toList());
    }

    protected static Library getRootSpecificationLibrary(List<BundleEntryComponent> bundleEntries) {
        Optional<Library> rootSpecLibrary = bundleEntries.stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.Library)
                .map(entry -> ((Library) entry.getResource()))
                .filter(entry -> entry.getType().hasCoding(Constants.LIBRARY_TYPE, Constants.ASSET_COLLECTION)
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
                                                        .hasCoding(Constants.US_PH_CONTEXT_URL, "triggering"))
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
                                                        .hasCoding(Constants.US_PH_CONTEXT_URL, "program"))))
                .findFirst();
        return rootSpecLibrary.orElse(null);
    }

    protected static Parameters getExpansionParams(Library rootSpecificationLibrary, String reference) {
        Optional<Resource> expansionParamResource = rootSpecificationLibrary.getContained().stream()
                .filter(contained -> contained.getId().equals(reference))
                .findFirst();
        return (Parameters) expansionParamResource.orElse(null);
    }

    protected void checkIfValueSetNeedsCondition(IBaseResource resource, RelatedArtifact relatedArtifact)
            throws UnprocessableEntityException {
        if (resource != null && resource.fhirType().equals(ResourceType.ValueSet.name())) {
            ValueSet valueSet = (ValueSet) resource;
            // TODO:: do we need to update the definition of a leaf?
            boolean isLeaf = !valueSet.hasCompose()
                    || (valueSet.hasCompose()
                            && valueSet.getCompose()
                                    .getIncludeFirstRep()
                                    .getValueSet()
                                    .isEmpty());
            Optional<Extension> maybeConditionExtension = Optional.ofNullable(relatedArtifact)
                    .map(RelatedArtifact::getExtension)
                    .map(list -> {
                        return list.stream()
                                .filter(ext -> ext.getUrl().equalsIgnoreCase(Constants.VALUE_SET_CONDITION_URL))
                                .findFirst()
                                .orElse(null);
                    });
            Optional<Extension> maybePriorityExtension = Optional.ofNullable(relatedArtifact)
                    .map(RelatedArtifact::getExtension)
                    .map(list -> {
                        return list.stream()
                                .filter(ext -> ext.getUrl().equalsIgnoreCase(Constants.VALUE_SET_PRIORITY_URL))
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

    /**
     * Removes any existing UsageContexts corresponding to the VSM specific extensions
     * @param usageContexts the list of usage contexts to modify
     */
    protected List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
        List<String> useContextCodesToReplace = Collections.unmodifiableList(
                Arrays.asList(Constants.VALUE_SET_CONDITION_CODE, Constants.VALUE_SET_PRIORITY_CODE));
        return usageContexts.stream()
                // remove any useContexts which need to be replaced
                .filter(useContext -> !useContextCodesToReplace.stream()
                        .anyMatch(code -> useContext.getCode().getCode().equals(code)))
                .collect(Collectors.toList());
    }

    protected void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
        boolean focusAlreadyExists = usageContexts.stream()
                .anyMatch(u -> u.getCode().getSystem().equals(KnowledgeArtifactAdapter.contextTypeUrl)
                        && u.getCode().getCode().equals(Constants.VALUE_SET_CONDITION_CODE)
                        && u.getValueCodeableConcept()
                                .hasCoding(
                                        condition.getCoding().get(0).getSystem(),
                                        condition.getCoding().get(0).getCode()));
        if (!focusAlreadyExists) {
            UsageContext newFocus = new UsageContext(
                    new Coding(KnowledgeArtifactAdapter.contextUrl, Constants.VALUE_SET_CONDITION_CODE, null),
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
    protected UsageContext getOrCreateUsageContext(List<UsageContext> usageContexts, String system, String code) {
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
