package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportAggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Dstu3MeasureReportAggregator implements MeasureReportAggregator<MeasureReport> {

    @Override
    public MeasureReport aggregate(Iterable<MeasureReport> reports) {
        if (reports == null) {
            return null;
        }

        Iterator<MeasureReport> iterator = reports.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        MeasureReport carry = iterator.next();
        if (carry.hasType() && carry.getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)) {
            throw new IllegalArgumentException(String.format("Can not aggregate MeasureReports of type: %s", MeasureReport.MeasureReportType.INDIVIDUAL.toCode()));
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

        if (carry.hasMeasure() ^ current.hasMeasure() || (carry.hasMeasure() && !carry.getMeasure().equals(current.getMeasure()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be for the same Measure. carry: %s, current: %s", carry.getMeasure(), current.getMeasure()));
        }

        if ((carry.hasPeriod() ^ current.hasPeriod()) || (carry.hasPeriod() && !carry.getPeriod().equalsDeep(current.getPeriod()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be for the same Period. carry: %s, current: %s", carry.getPeriod().toString(), current.getPeriod().toString()));
        }

        if (carry.hasType() ^ current.hasType() || (carry.hasType() && !carry.getType().equals(current.getType()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be of the same type. carry: %s, current: %s", carry.getType().toCode(), current.getType().toCode()));
        }

        mergeContained(carry, current);
        mergeExtensions(carry, current);
        mergePopulation(carry, current);
        mergeStratifier(carry, current);

    }


    protected void mergeContained(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        //this map will store population code as key and subjectReference as list item
        Map<String, List<Reference>> populationCodeSubjectsReferenceMap = new HashMap<>();

        harvestSubjectReferencesAgainstPopulationCode(populationCodeSubjectsReferenceMap, carry);
        harvestSubjectReferencesAgainstPopulationCode(populationCodeSubjectsReferenceMap, current);

        Map<String, Resource> resourceMap = new HashMap<>();
        Map<String, Resource> carryListResourceMap = new HashMap<>();
        Map<String, Resource> currentListResourceMap = new HashMap<>();

        populateMapsWithResourceAndListResource(carry, resourceMap, carryListResourceMap);
        carry.getContained().clear();
        populateMapsWithResourceAndListResource(current, resourceMap, currentListResourceMap);


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
                carryList = getMatchedListResource(currentListResourceMap, reference);

            }

            if (carryList != null && currentList != null) {
                mergeList(carryList, currentList);
            }
            if (carryList != null) {
                resourceMap.put(carryList.hasId() ? carryList.getId() : UUID.randomUUID().toString(), carryList);
            }
        });


        carry.getContained().addAll(resourceMap.values());
        carry.getContained().addAll(carryListResourceMap.values());
        carry.getContained().addAll(currentListResourceMap.values());

    }

    private String extractId(String reference) {
        if (reference != null) {
            if (reference.startsWith("#")) {
                return reference.substring(1);
            } else if (reference.contains("/") && reference.length() > 1) {
                return reference.substring(reference.indexOf("/") + 1);
            }
        }
        return reference;
    }

    private ListResource getMatchedListResource(Map<String, Resource> listResourceMap, String key) {
        ListResource listResource = null;
        if (listResourceMap.containsKey(key)) {
            listResource = (ListResource) listResourceMap.get(key);
            listResourceMap.remove(key);
        }
        return listResource;
    }

    //eligible means having id, it is important having id as population subjectReference will match this id
    private boolean isEligibleListResourceType(Resource resource) {
        return resource.hasId() &&
                (resource.getResourceType() != null &&
                        resource.getResourceType().equals(ResourceType.List));
    }

    private void populateMapsWithResourceAndListResource(MeasureReport measureReport,
                                                         Map<String, Resource> resourceMap,
                                                         Map<String, Resource> listResourceMap) {
        measureReport.getContained().forEach(resource -> {
            String id = extractId(resource.getId());
            if (!isEligibleListResourceType(resource)) {
                if (!resourceMap.containsKey(id)) {
                    resourceMap.put(id, resource);
                }
            } else if (isEligibleListResourceType(resource)) {
                listResourceMap.put(id, resource);
            }
        });
    }

    private void harvestSubjectReferencesAgainstPopulationCode(Map<String, List<Reference>> populationCodeSubjectsReferenceMap,
                                                               MeasureReport measureReport) {
        measureReport.getGroupFirstRep().getPopulation().forEach(populationComponent -> {
            if (populationComponent.hasCode()) {
                CodeableConcept codeableConcept = populationComponent.getCode();
                String key = codeableConcept.getCodingFirstRep().getCode();

                if (StringUtils.isNotBlank(key) && populationComponent.hasPatients()) {
                    if (populationCodeSubjectsReferenceMap.containsKey(key)) {
                        (populationCodeSubjectsReferenceMap.get(key))
                                .add(populationComponent.getPatients());
                    } else {
                        List<Reference> list = new ArrayList<>();
                        list.add(populationComponent.getPatients());
                        populationCodeSubjectsReferenceMap.put(key, list);
                    }
                }
            }
        });
    }

    private void mergeList(ListResource carry, ListResource current) {
        List<String> itemIds = new ArrayList<>();
        for (ListResource.ListEntryComponent comp : carry.getEntry()) {
            itemIds.add(comp.getItem().getReference());
        }

        for (ListResource.ListEntryComponent comp : current.getEntry()) {
            if (!itemIds.contains(comp.getItem().getReference())) {
                carry.getEntry().add(comp);
            }
        }
    }

    protected void mergeExtensions(MeasureReport carry, MeasureReport current) {
        if (current == null || carry == null) {
            return;
        }

        List<String> extensionDummyIds = new ArrayList<>();

        for (Extension extension : carry.getExtension()) {
            if (extension.hasValue()) {
                if (extension.getValue() instanceof StringType) {
                    extensionDummyIds.add(((StringType) extension.getValue()).getValue());
                } else if (extension.getValue() instanceof Reference) {
                    extensionDummyIds.add(((Reference) extension.getValue()).getReference());
                }
            }
        }

        for (Extension extension : current.getExtension()) {
            if (extension.hasValue()) {
                if (extension.getValue() instanceof StringType) {
                    if (!extensionDummyIds.contains(((StringType) extension.getValue()).getValue())) {
                        carry.getExtension().add(extension);
                    }
                } else if (extension.getValue() instanceof Reference) {
                    if (!extensionDummyIds.contains(((Reference) extension.getValue()).getReference())) {
                        carry.getExtension().add(extension);
                    }
                }
            }
        }
    }

    protected void mergePopulation(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        HashMap<String, String> codeScore = new HashMap<>();

        for (MeasureReport.MeasureReportGroupPopulationComponent populationComponent : current.getGroupFirstRep().getPopulation()) {
            CodeableConcept codeableConcept = populationComponent.getCode();
            if (StringUtils.isNotBlank(codeableConcept.getCodingFirstRep().getCode())) {
                codeScore.put(codeableConcept.getCodingFirstRep().getCode(), Integer.toString(populationComponent.getCount()));
            }
        }

        for (MeasureReport.MeasureReportGroupPopulationComponent populationComponent : carry.getGroupFirstRep().getPopulation()) {
            CodeableConcept codeableConcept = populationComponent.getCode();
            if (StringUtils.isNotBlank(codeableConcept.getCodingFirstRep().getCode())) {
                if (codeScore.get(codeableConcept.getCodingFirstRep().getCode()) != null) {
                    populationComponent.setCount(populationComponent.getCount() +
                            Integer.parseInt(codeScore.get(codeableConcept.getCodingFirstRep().getCode())));
                }
            }
        }
    }

    protected void mergeStratifier(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        HashMap<String, String> codeScore = new HashMap<>();

        AtomicReference<String> stratifierCodeKey = new AtomicReference<>("");
        AtomicReference<String> stratifierStratumKey = new AtomicReference<>("");
        AtomicReference<String> stratifierStratumPopulationKey = new AtomicReference<>("");

        current.getGroupFirstRep().getStratifier().forEach(stratifierComponent -> {
            if (stratifierComponent.hasIdentifier() && stratifierComponent.getIdentifier().hasValue()) {
                String key = stratifierComponent.getIdentifier().getValue();
                if (StringUtils.isNotBlank(key)) {
                    stratifierCodeKey.set(key);
                }

                if (stratifierComponent.hasStratum()) {
                    stratifierComponent.getStratum().forEach(stratumComponent -> {
                        if (stratumComponent.hasValue()) {
                            String value = stratumComponent.getValue();
                            stratifierStratumKey.set(value);
                        }

                        if (stratumComponent.hasPopulation()) {
                            stratumComponent.getPopulation().forEach(stratumPopulationComp -> {
                                if (stratumPopulationComp.hasCode()) {
                                    CodeableConcept populationCodeableConcept = stratumPopulationComp.getCode();
                                    stratifierStratumPopulationKey.set(getKeyValue(populationCodeableConcept));

                                    if (stratumPopulationComp.hasCount()) {
                                        codeScore.put(generateKey(stratifierCodeKey.get(), stratifierStratumKey.get(), stratifierStratumPopulationKey.get()),
                                                Integer.toString(stratumPopulationComp.getCount()));
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
            if (stratifierComponent.hasIdentifier() && stratifierComponent.getIdentifier().hasValue()) {
                String key = stratifierComponent.getIdentifier().getValue();
                if (StringUtils.isNotBlank(key)) {
                    stratifierCodeKey.set(key);
                }

                if (stratifierComponent.hasStratum()) {
                    stratifierComponent.getStratum().forEach(stratumComponent -> {
                        if (stratumComponent.hasValue()) {
                            String value = stratumComponent.getValue();
                            stratifierStratumKey.set(value);
                        }

                        if (stratumComponent.hasPopulation()) {
                            stratumComponent.getPopulation().forEach(stratumPopulationComp -> {
                                if (stratumPopulationComp.hasCode()) {
                                    CodeableConcept populationCodeableConcept = stratumPopulationComp.getCode();
                                    stratifierStratumPopulationKey.set(getKeyValue(populationCodeableConcept));

                                    if (stratumPopulationComp.hasCount()) {
                                        String combinedKey = generateKey(stratifierCodeKey.get(), stratifierStratumKey.get(), stratifierStratumPopulationKey.get());
                                        if (codeScore.containsKey(combinedKey)) {
                                            stratumPopulationComp.setCount(stratumPopulationComp.getCount() +
                                                    Integer.parseInt(codeScore.get(combinedKey)));
                                        }
                                    }

                                }
                            });
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
