package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportAggregator;

public class R4MeasureReportAggregator implements MeasureReportAggregator<MeasureReport> {
    public MeasureReport aggregate(Iterable<MeasureReport> reports) {
        if (reports == null) {
            return null;
        }

        Iterator<MeasureReport> iterator = reports.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        MeasureReport carry = iterator.next();
        if (carry.hasType() && carry.getType().equals(MeasureReportType.INDIVIDUAL)) {
            throw new IllegalArgumentException(
                    String.format("Can not aggregate MeasureReports of type: %s",
                            MeasureReportType.INDIVIDUAL.toCode()));
        }

        while (iterator.hasNext()) {
            merge(carry, iterator.next());
        }

        return carry;
    }

    protected void merge(MeasureReport carry, MeasureReport current) {
        if (current == null) {
            return;
        }

        if (carry.hasMeasure() ^ current.hasMeasure()
                || (carry.hasMeasure() && !carry.getMeasure().equals(current.getMeasure()))) {
            throw new IllegalArgumentException(String.format(
                    "Aggregated MeasureReports must all be for the same Measure. carry: %s, current: %s",
                    carry.getMeasure(), current.getMeasure()));
        }

        if ((carry.hasPeriod() ^ current.hasPeriod())
                || (carry.hasPeriod() && !carry.getPeriod().equalsDeep(current.getPeriod()))) {
            throw new IllegalArgumentException(String.format(
                    "Aggregated MeasureReports must all be for the same Period. carry: %s, current: %s",
                    carry.getPeriod().toString(), current.getPeriod().toString()));
        }

        if (carry.hasType() ^ current.hasType()
                || (carry.hasType() && !carry.getType().equals(current.getType()))) {
            throw new IllegalArgumentException(String.format(
                    "Aggregated MeasureReports must all be of the same type. carry: %s, current: %s",
                    carry.getType().toCode(), current.getType().toCode()));
        }

        mergePopulation(carry, current);
        mergeStratifier(carry, current);
        mergeContained(carry, current);
        mergeExtensions(carry, current);
        mergeEvaluatedResources(carry, current);

    }

    protected void mergeContained(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        // this map will store population code as key and subjectReference as list item
        Map<String, List<Reference>> populationCodeSubjectsReferenceMap = new HashMap<>();

        harvestSubjectReferencesAgainstPopulationCode(populationCodeSubjectsReferenceMap, carry);
        harvestSubjectReferencesAgainstPopulationCode(populationCodeSubjectsReferenceMap, current);

        Map<String, Resource> resourceMap = new HashMap<>();
        Map<String, Resource> carryListResourceMap = new HashMap<>();
        Map<String, Resource> currentListResourceMap = new HashMap<>();

        populateMapsWithResource(carry, resourceMap, carryListResourceMap);

        populateMapsWithResource(current, resourceMap, currentListResourceMap);


        // this map will contain the added and vanished observations new id reference
        Map<String, String> changedIdMap = new HashMap<>();
        // this map will contain resources and summed observation where applicable
        Map<String, Resource> reducedMap = new HashMap<>();
        addObservationIfApplicable(resourceMap, reducedMap, changedIdMap);

        populationCodeSubjectsReferenceMap.values().forEach(list -> {
            ListResource carryList = null, currentList = null;

            if (list.size() > 1) {
                String carryReference = extractId(list.get(0).getReference());
                String currentReference = extractId(list.get(1).getReference());

                carryList = getMatchedListResource(carryListResourceMap, carryReference);
                currentList = getMatchedListResource(currentListResourceMap, currentReference);

            } else if (list.size() == 1) {
                String reference = extractId(list.get(0).getReference());

                carryList = getMatchedListResource(carryListResourceMap, reference);
                if (carryList == null) {
                    carryList = getMatchedListResource(currentListResourceMap, reference);
                }
            }

            if (carryList != null && currentList != null) {
                mergeList(carryList, currentList);
            }
            if (carryList != null) {
                reducedMap.put(carryList.hasId() ? carryList.getId() : UUID.randomUUID().toString(),
                        carryList);
            }
        });


        carry.getContained().clear();
        carry.getContained().addAll(reducedMap.values());
        carry.getContained().addAll(carryListResourceMap.values());
        carry.getContained().addAll(currentListResourceMap.values());
        updateExtensionListResourceReference(carry, changedIdMap);
    }

    private void addObservationIfApplicable(Map<String, Resource> resourceMap,
            Map<String, Resource> reducedMap, Map<String, String> changedIdMap) {

        Map<String, Observation> codeReducedMap = new HashMap<>();

        resourceMap.values().forEach(resource -> {
            if (resource.getResourceType().equals(ResourceType.Observation)) {
                Observation observation = ((Observation) resource);
                if (checkIfObservationIsAdditive(observation)) {
                    if (observation.getCode().getCodingFirstRep().hasCode()) {
                        String code = observation.getCode().getCodingFirstRep().getCode();
                        if (!codeReducedMap.containsKey(code)) {
                            codeReducedMap.put(code, observation);
                        } else {
                            int sum = observation.getValueIntegerType().getValue().intValue()
                                    + codeReducedMap.get(code).getValueIntegerType().getValue()
                                            .intValue();
                            codeReducedMap.get(code).setValue(new IntegerType(sum));
                            changedIdMap.put(observation.getId(), codeReducedMap.get(code).getId());
                        }
                    }
                }
            } else {
                reducedMap.put(resource.getId(), resource);
            }
        });
        codeReducedMap.values().forEach(resource -> {
            reducedMap.put(resource.getId(), resource);
        });
    }

    private boolean checkIfObservationIsAdditive(Observation observation) {
        return (observation.getValue() instanceof IntegerType);
    }

    private void updateExtensionListResourceReference(MeasureReport report,
            Map<String, String> changedIdMap) {
        report.getExtension().forEach(extension -> {
            if (extension.getValue() instanceof Reference) {
                Reference reference = (Reference) extension.getValue();
                String key = reference.getReference();
                if (changedIdMap.containsKey(key)) {
                    reference.setReference(changedIdMap.get(key));
                }
            }
        });
    }

    private String extractId(String reference) {
        return (reference != null && reference.startsWith("#")) ? reference.substring(1)
                : reference;
    }

    private ListResource getMatchedListResource(Map<String, Resource> listResourceMap, String key) {
        ListResource listResource = null;
        if (listResourceMap.containsKey(key)) {
            listResource = (ListResource) listResourceMap.get(key);
            listResourceMap.remove(key);
        }
        return listResource;
    }

    // eligible means having id, it is important having id as population subjectReference will match
    // this id
    private boolean isEligibleListResourceType(Resource resource) {
        return resource.hasId() && (resource.getResourceType() != null
                && resource.getResourceType().equals(ResourceType.List));
    }

    private void populateMapsWithResource(MeasureReport measureReport,
            Map<String, Resource> resourceMap, Map<String, Resource> listResourceMap) {
        measureReport.getContained().forEach(resource -> {
            if (!isEligibleListResourceType(resource)) {
                if (!resourceMap.containsKey(extractId(resource.getId()))) {
                    resourceMap.put(extractId(resource.getId()), resource);
                }
            } else if (isEligibleListResourceType(resource)) {
                listResourceMap.put(extractId(resource.getId()), resource);
            }
        });
    }

    private void harvestSubjectReferencesAgainstPopulationCode(
            Map<String, List<Reference>> populationCodeSubjectsReferenceMap,
            MeasureReport measureReport) {
        measureReport.getGroupFirstRep().getPopulation().forEach(populationComponent -> {
            if (populationComponent.hasCode()) {
                CodeableConcept codeableConcept = populationComponent.getCode();
                String key = codeableConcept.getCodingFirstRep().getCode();
                if (StringUtils.isNotBlank(key) && populationComponent.hasSubjectResults()) {

                    if (populationCodeSubjectsReferenceMap.containsKey(key)) {
                        (populationCodeSubjectsReferenceMap.get(key))
                                .add(populationComponent.getSubjectResults());
                    } else {
                        List<Reference> list = new ArrayList<>();
                        list.add(populationComponent.getSubjectResults());
                        populationCodeSubjectsReferenceMap.put(key, list);
                    }
                }
            }
        });
    }

    private void mergeList(ListResource carry, ListResource current) {
        List<String> itemIds = new ArrayList<>();
        carry.getEntry().forEach(comp -> itemIds.add(comp.getItem().getReference()));

        current.getEntry().forEach(comp -> {
            if (!itemIds.contains(comp.getItem().getReference())) {
                carry.getEntry().add(comp);
            }
        });
    }

    protected void mergeExtensions(MeasureReport carry, MeasureReport current) {
        if (current == null || carry == null) {
            return;
        }

        Map<String, Extension> extensionMap = new HashMap<>();
        carry.getExtension().addAll(current.getExtension());

        carry.getExtension().forEach(extension -> {
            if (extension.hasValue()) {
                String key;
                if (extension.getValue() instanceof StringType) {
                    key = generateKey(extension.getUrl(),
                            ((StringType) extension.getValue()).getValue(), "");
                    if (!extensionMap.containsKey(key)) {
                        extensionMap.put(key, extension);
                    }
                } else if (extension.getValue() instanceof Reference) {
                    key = generateKey(extension.getUrl(),
                            ((Reference) extension.getValue()).getReference(), "");
                    if (extensionMap.containsKey(key)) {
                        extensionMap.get(key).getValue().getExtension()
                                .addAll(extension.getValue().getExtension());
                    } else {
                        extensionMap.put(key, extension);
                    }
                } else {
                    extensionMap.put(UUID.randomUUID().toString(), extension);
                }
            }
        });
        carry.getExtension().clear();
        carry.getExtension().addAll(extensionMap.values());
    }

    protected void mergeEvaluatedResources(MeasureReport carry, MeasureReport current) {
        if (current == null || carry == null) {
            return;
        }

        Map<String, List<Extension>> extensionMap = new HashMap<>();

        carry.getEvaluatedResource().forEach(reference -> {
            extensionMap.put(reference.getReference(), reference.getExtension());
        });

        current.getEvaluatedResource().forEach(reference -> {
            if (!extensionMap.containsKey(reference.getReference())) {
                carry.getEvaluatedResource().add(reference);
            } else {
                List<Extension> list = extensionMap.get(reference.getReference());
                Set<String> mergeSet = new HashSet<>();
                list.forEach(item -> mergeSet.add(
                        generateKey(item.getUrl(), ((StringType) item.getValue()).getValue(), "")));
                reference.getExtension().forEach(item -> {
                    if (!mergeSet.contains(generateKey(item.getUrl(),
                            ((StringType) item.getValue()).getValue(), ""))) {
                        list.add(item);
                    }
                });
                reference.setExtension(list);
            }
        });
    }

    protected void mergePopulation(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        HashMap<String, MeasureReport.MeasureReportGroupPopulationComponent> codeScore =
                new HashMap<>();

        current.getGroupFirstRep().getPopulation().forEach(populationComponent -> {
            String code = populationComponent.getCode().getCodingFirstRep().getCode();
            if (StringUtils.isNotBlank(code)) {
                codeScore.put(code, populationComponent);
            }
        });

        carry.getGroupFirstRep().getPopulation().forEach(populationComponent -> {
            String code = populationComponent.getCode().getCodingFirstRep().getCode();
            if (StringUtils.isNotBlank(code)) {
                if (codeScore.get(code) != null) {
                    populationComponent.setCount(
                            populationComponent.getCount() + codeScore.get(code).getCount());
                    codeScore.remove(code);
                }
            }
        });
        carry.getGroupFirstRep().getPopulation().addAll(codeScore.values());
    }

    protected void mergeStratifier(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        HashMap<String, MeasureReport.StratifierGroupPopulationComponent> codeScore =
                new HashMap<>();

        AtomicReference<String> stratifierCodeKey = new AtomicReference<>("");
        AtomicReference<String> stratifierStratumKey = new AtomicReference<>("");
        AtomicReference<String> stratifierStratumPopulationKey = new AtomicReference<>("");

        current.getGroupFirstRep().getStratifier().forEach(stratifierComponent -> {
            if (stratifierComponent.hasCode()) {
                CodeableConcept codeableConcept = stratifierComponent.getCodeFirstRep();
                if (codeableConcept != null) {
                    stratifierCodeKey.set(getKeyValue(codeableConcept));
                }

                if (stratifierComponent.hasStratum()) {
                    stratifierComponent.getStratum().forEach(stratumComponent -> {
                        if (stratumComponent.hasValue()) {
                            CodeableConcept value = stratumComponent.getValue();
                            stratifierStratumKey.set(getKeyValue(value));
                        }

                        if (stratumComponent.hasPopulation()) {
                            stratumComponent.getPopulation().forEach(stratumPopulationComp -> {
                                if (stratumPopulationComp.hasCode()) {
                                    CodeableConcept populationCodeableConcept =
                                            stratumPopulationComp.getCode();
                                    stratifierStratumPopulationKey
                                            .set(getKeyValue(populationCodeableConcept));

                                    if (stratumPopulationComp.hasCount()) {
                                        codeScore.put(
                                                generateKey(stratifierCodeKey.get(),
                                                        stratifierStratumKey.get(),
                                                        stratifierStratumPopulationKey.get()),
                                                stratumPopulationComp);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        stratifierCodeKey.set("");
        stratifierStratumKey.set("");
        stratifierStratumPopulationKey.set("");

        carry.getGroupFirstRep().getStratifier().forEach(stratifierComponent -> {
            if (stratifierComponent.hasCode()) {
                CodeableConcept codeableConcept = stratifierComponent.getCodeFirstRep();
                if (codeableConcept != null) {
                    stratifierCodeKey.set(getKeyValue(codeableConcept));
                }

                if (stratifierComponent.hasStratum()) {
                    stratifierComponent.getStratum().forEach(stratumComponent -> {
                        if (stratumComponent.hasValue()) {
                            CodeableConcept value = stratumComponent.getValue();
                            stratifierStratumKey.set(getKeyValue(value));
                        }

                        if (stratumComponent.hasPopulation()) {
                            stratumComponent.getPopulation().forEach(stratumPopulationComp -> {
                                if (stratumPopulationComp.hasCode()) {
                                    CodeableConcept populationCodeableConcept =
                                            stratumPopulationComp.getCode();
                                    stratifierStratumPopulationKey
                                            .set(getKeyValue(populationCodeableConcept));

                                    if (stratumPopulationComp.hasCount()) {
                                        String key = generateKey(stratifierCodeKey.get(),
                                                stratifierStratumKey.get(),
                                                stratifierStratumPopulationKey.get());
                                        if (codeScore.containsKey(key)) {
                                            stratumPopulationComp
                                                    .setCount(stratumPopulationComp.getCount()
                                                            + codeScore.get(key).getCount());
                                            codeScore.remove(key);
                                        }
                                    }

                                }
                            });
                            stratumComponent
                                    .getPopulation().addAll(
                                            codeScore
                                                    .entrySet().stream().filter(
                                                            map -> map.getKey()
                                                                    .startsWith(generateKey(
                                                                            stratifierCodeKey.get(),
                                                                            stratifierStratumKey
                                                                                    .get(),
                                                                            "")))
                                                    .collect(Collectors.toMap(map -> map.getKey(),
                                                            map -> map.getValue()))
                                                    .values());
                        }
                    });
                }
            }
        });
    }

    private String getKeyValue(CodeableConcept codeableConcept) {
        if (codeableConcept.hasCoding()) {
            if (StringUtils.isNotBlank(codeableConcept.getCodingFirstRep().getCode())) {
                return codeableConcept.getCodingFirstRep().getCode().trim();
            }
        } else if (codeableConcept.hasText()) {
            return codeableConcept.getText().trim();
        }
        return "";
    }

    private String generateKey(String part1, String part2, String part3) {
        return new StringBuilder(part1).append(part2).append(part3).toString();
    }

}
