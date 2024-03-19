package org.opencds.cqf.fhir.utility.visitor.r4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;

public class KnowledgeArtifactPackageVisitor {
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
}
