package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInCompose;

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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

public class KnowledgeArtifactPackageVisitor {
    // private Logger logger = LoggerFactory.getLogger(KnowledgeArtifactPackageVisitor.class);

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    public KnowledgeArtifactPackageVisitor() {
        this.terminologyServerClient = new TerminologyServerClient(fhirContext);
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
            Optional<Endpoint> terminologyEndpoint)
            throws UnprocessableEntityException, IllegalArgumentException {
        Parameters expansionParams = new Parameters();
        Library rootSpecificationLibrary = getRootSpecificationLibrary(bundleEntries);
        if (rootSpecificationLibrary != null) {
            Extension expansionParamsExtension =
                    rootSpecificationLibrary.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
            if (expansionParamsExtension != null && expansionParamsExtension.hasValue()) {
                Reference expansionReference = (Reference) expansionParamsExtension.getValue();
                expansionParams = getExpansionParams(rootSpecificationLibrary, expansionReference.getReference());
            }
        }
        Parameters params = expansionParams;
        bundleEntries.stream().forEach(entry -> {
            if (entry.getResource().getResourceType().equals(ResourceType.ValueSet)) {
                ValueSet valueSet = (ValueSet) entry.getResource();
                expandValueSet(valueSet, params, terminologyEndpoint);
            }
        });
    }

    public void expandValueSet(
            ValueSet valueSet, Parameters expansionParameters, Optional<Endpoint> terminologyEndpoint) {
        // Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
        Extension authoritativeSource = valueSet.getExtensionByUrl(Constants.AUTHORITATIVE_SOURCE_URL);
        String authoritativeSourceUrl = authoritativeSource != null && authoritativeSource.hasValue()
                ? authoritativeSource.getValue().primitiveValue()
                : valueSet.getUrl();

        ValueSet expandedValueSet;
        if (hasSimpleCompose(valueSet)) {
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

            for (var code : getCodesInCompose(fhirContext, valueSet)) {
                expansion
                        .addContains()
                        .setCode(code.getCode())
                        .setSystem(code.getSystem())
                        .setVersion(code.getVersion())
                        .setDisplay(code.getDisplay());
            }
            valueSet.setExpansion(expansion);
        } else {
            String username;
            String apiKey;
            if (terminologyEndpoint.isPresent()) {
                username = terminologyEndpoint.get().getExtensionsByUrl(Constants.VSAC_USERNAME).stream()
                        .findFirst()
                        .map(ext -> ext.getValue().toString())
                        .orElseThrow(() -> new UnprocessableEntityException(
                                "Cannot expand ValueSet without VSAC Username: " + valueSet.getId()));
                apiKey = terminologyEndpoint.get().getExtensionsByUrl(Constants.APIKEY).stream()
                        .findFirst()
                        .map(ext -> ext.getValue().toString())
                        .orElseThrow(() -> new UnprocessableEntityException(
                                "Cannot expand ValueSet without VSAC API Key: " + valueSet.getId()));
            } else {
                throw new UnprocessableEntityException(
                        "Cannot expand ValueSet without a terminology server: " + valueSet.getId());
            }

            try {
                expandedValueSet = terminologyServerClient.expand(
                        valueSet, authoritativeSourceUrl, expansionParameters, username, apiKey);
                valueSet.setExpansion(expandedValueSet.getExpansion());
            } catch (Exception ex) {
                throw new UnprocessableEntityException(
                        "Terminology Server expansion failed for: " + valueSet.getId(), ex.getMessage());
            }
        }
    }

    // A simple compose element of a ValueSet must have a compose without an exclude element. Each element of the
    // include cannot have a filter, and must reference a ValueSet or have a system and enumerate concepts
    protected boolean hasSimpleCompose(ValueSet valueSet) {
        return valueSet.hasCompose()
                && !valueSet.getCompose().hasExclude()
                && valueSet.getCompose().getInclude().stream()
                        .noneMatch(csc ->
                                csc.hasFilter() || (!csc.hasValueSet() && (!csc.hasSystem() && !csc.hasConcept())));
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
