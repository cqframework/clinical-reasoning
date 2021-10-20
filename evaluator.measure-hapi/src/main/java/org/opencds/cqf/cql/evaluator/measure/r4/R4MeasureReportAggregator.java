package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.Resource;
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
            throw new IllegalArgumentException(String.format("Can not aggregate MeasureReports of type: " + MeasureReportType.INDIVIDUAL.toCode()));
        }
        
        while(iterator.hasNext()) {
            merge(carry, iterator.next());
        }

        return carry;
    }

    protected void merge(MeasureReport carry, MeasureReport current) {
        if (current == null) {
            return;
        }

        mergeContained(carry, current);
        mergeExtensions(carry, current);
        mergePopulation(carry, current);
        mergeStratifier(carry, current);

        if (carry.hasMeasure() ^ current.hasMeasure() || (carry.hasMeasure() && !carry.getMeasure().equals(current.getMeasure()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be for the same Measure. carry: %s, current: %s", carry.getMeasure(), current.getMeasure()));
        }

        if ((carry.hasPeriod() ^ current.hasPeriod()) || (carry.hasPeriod() && !carry.getPeriod().equalsDeep(current.getPeriod()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be for the same Period. carry: %s, current: %s", carry.getPeriod().toString(), current.getPeriod().toString()));  
        }

        if (carry.hasType() ^ current.hasType() || (carry.hasType() && !carry.getType().equals(current.getType()))) {
            throw new IllegalArgumentException(String.format("Aggregated MeasureReports must all be of the same type. carry: %s, current: %s", carry.getType().toCode(), current.getType().toCode()));  
        }
    }

    protected void mergeContained(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        List<String> carryIds = new ArrayList<>();

        for (Resource resource : carry.getContained()) {
            if (resource.hasId()) {
                carryIds.add(resource.getId());
            }
        }

        for (Resource resource : current.getContained()) {
            if (resource.hasId()) {
                if (!carryIds.contains(resource.getId())) {
                    carryIds.add(resource.getId());
                    carry.getContained().add(resource);
                }
            }
        }
    }

    protected void mergeExtensions(MeasureReport carry, MeasureReport current) {
        if (current == null || carry == null) {
            return;
        }

        List<String> carryIds = new ArrayList<>();

        for (Extension extension : carry.getExtension()) {
            if (extension.hasUrl()) {
                carryIds.add(extension.getUrl());
            }
        }

        for (Extension extension : current.getExtension()) {
            if (extension.hasUrl()) {
                if (!carryIds.contains(extension.getUrl())) {
                    carryIds.add(extension.getUrl());
                    carry.getExtension().add(extension);
                }
            }
        }
    }

    protected void mergePopulation(MeasureReport carry, MeasureReport current) {

        if (current == null || carry == null) {
            return;
        }

        HashMap<String, String> codeScore = new HashMap<String, String>();

        for (MeasureReport.MeasureReportGroupPopulationComponent populationComponent : current.getGroupFirstRep().getPopulation()) {
            CodeableConcept codeableConcept = populationComponent.getCode();
            if(StringUtils.isNotBlank(codeableConcept.getCodingFirstRep().getCode())) {
                codeScore.put(codeableConcept.getCodingFirstRep().getCode(), Integer.toString(populationComponent.getCount()));
            }
        }

        for (MeasureReport.MeasureReportGroupPopulationComponent populationComponent : carry.getGroupFirstRep().getPopulation()) {
            CodeableConcept codeableConcept = populationComponent.getCode();
            if(StringUtils.isNotBlank(codeableConcept.getCodingFirstRep().getCode())) {
                if(codeScore.get(codeableConcept.getCodingFirstRep().getCode()) != null) {
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

        HashMap<String, String> codeScore = new HashMap<String, String>();

        String stratifierCodeKey = "";
        String stratifierStratumKey = "";
        String stratifierStratumPopulationKey = "";
        for (MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent : current.getGroupFirstRep().getStratifier()) {
            CodeableConcept codeableConcept = stratifierComponent.getCodeFirstRep();
            stratifierCodeKey = getKeyValue(codeableConcept);

            if (stratifierComponent.hasStratum()) {
                for (MeasureReport.StratifierGroupComponent stratumComponent : stratifierComponent.getStratum()) {
                    if (stratumComponent.hasValue()) {
                        CodeableConcept value = stratumComponent.getValue();
                        stratifierStratumKey = getKeyValue(value);
                    }

                    if (stratumComponent.hasPopulation()) {
                        for (MeasureReport.StratifierGroupPopulationComponent stratumPopulationComp : stratumComponent.getPopulation()) {
                            if (stratumPopulationComp.hasCode()) {
                                CodeableConcept populationCodeableConcept = stratumPopulationComp.getCode();
                                stratifierStratumPopulationKey = getKeyValue(populationCodeableConcept);

                                if (stratumPopulationComp.hasCount()) {
                                    codeScore.put(generateKey(stratifierCodeKey, stratifierStratumKey, stratifierStratumPopulationKey),
                                            Integer.toString(stratumPopulationComp.getCount()));
                                }

                            }
                        }
                    }
                }

            }
        }

        stratifierCodeKey = "";
        stratifierStratumKey = "";
        stratifierStratumPopulationKey = "";

        for (MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent : carry.getGroupFirstRep().getStratifier()) {
            CodeableConcept codeableConcept = stratifierComponent.getCodeFirstRep();
            stratifierCodeKey = getKeyValue(codeableConcept);

            if (stratifierComponent.hasStratum()) {
                for (MeasureReport.StratifierGroupComponent stratumComponent : stratifierComponent.getStratum()) {
                    if (stratumComponent.hasValue()) {
                        CodeableConcept value = stratumComponent.getValue();
                        stratifierStratumKey = getKeyValue(value);
                    }

                    if (stratumComponent.hasPopulation()) {
                        for (MeasureReport.StratifierGroupPopulationComponent stratumPopulationComp : stratumComponent.getPopulation()) {
                            if (stratumPopulationComp.hasCode()) {
                                CodeableConcept populationCodeableConcept = stratumPopulationComp.getCode();
                                stratifierStratumPopulationKey = getKeyValue(populationCodeableConcept);

                                if (stratumPopulationComp.hasCount()) {
                                    String key = generateKey(stratifierCodeKey, stratifierStratumKey, stratifierStratumPopulationKey);
                                    if (codeScore.containsKey(key)) {
                                        stratumPopulationComp.setCount(stratumPopulationComp.getCount() +
                                                Integer.parseInt(codeScore.get(key)));
                                    }
                                }

                            }
                        }
                    }
                }

            }
        }


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
