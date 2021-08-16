package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureInfo;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportScorer;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.PopulationDef;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

public class R4MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    protected MeasureReportScorer<MeasureReport> measureReportScorer;

    public R4MeasureReportBuilder() {
        this.measureReportScorer = new R4MeasureReportScorer();
    }

    protected Measure measure = null;
    protected MeasureReport report = null;
    protected HashMap<String, Reference> evaluatedResourceReferences = null;

    protected void reset() {
        this.measure = null;
        this.report = null;
        this.evaluatedResourceReferences = null;
    }

    @Override
    public MeasureReport build(Measure measure, MeasureDef measureDef, MeasureReportType measureReportType,
            Interval measurementPeriod, List<String> subjectIds) {
        this.reset();

        this.measure = measure;
        this.report = this.createMeasureReport(measure, measureReportType, subjectIds, measurementPeriod);

        buildGroups(measure, measureDef);
        processSdes(measure, measureDef, subjectIds);

        this.measureReportScorer.score(measureDef.getMeasureScoring(), this.report);

        for (Reference r : this.getEvaluatedResourceReferences().values()) {
            report.addEvaluatedResource(r);
        }

        return this.report;
    }

    protected void buildGroups(Measure measure, MeasureDef measureDef) {
        if (measure.getGroup().size() != measureDef.getGroups().size()) {
            throw new IllegalArgumentException(
                    "The Measure has a different number of groups defined than the MeasureDef");
        }

        // ASSUMPTION: The groups are in the same order in both the Measure and the
        // MeasureDef
        for (int i = 0; i < measure.getGroup().size(); i++) {
            MeasureGroupComponent mgc = measure.getGroup().get(i);
            String groupKey = this.getKey("group", mgc.getId(), mgc.getCode(), i);
            buildGroup(groupKey, mgc, this.report.addGroup(), measureDef.getGroups().get(i));
        }
    }

    protected void buildGroup(String groupKey, MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        if (measureGroup.getPopulation().size() != groupDef.values().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef");
        }

        if (measureGroup.getStratifier().size() != groupDef.getStratifiers().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef");
        }

        reportGroup.setCode(measureGroup.getCode());
        reportGroup.setId(measureGroup.getId());

        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    this.createStringExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/extension-description",
                            measureGroup.getDescription()));
        }

        for (MeasureGroupPopulationComponent mgpc : measureGroup.getPopulation()) {
            buildPopulation(groupKey, mgpc, reportGroup.addPopulation(),
                    groupDef.get(MeasurePopulationType.fromCode(mgpc.getCode().getCodingFirstRep().getCode())));
        }

        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            buildStratifier(groupKey, i, measureGroup.getStratifier().get(i), reportGroup.addStratifier(),
                    groupDef.getStratifiers().get(i), measureGroup.getPopulation());
        }
    }

    protected String getKey(String prefix, String id, CodeableConcept code, Integer index) {
        if (id != null) {
            return prefix + "-" + id;
        }

        // TODO: Consider using coding.code as a fallback
        if (code != null && code.hasText()) {
            return prefix + "-" + code.getText().toLowerCase().trim().replace(" ", "-");
        }

        return prefix + "-" + Integer.toString(index + 1);
    }

    protected void buildStratifier(String groupKey, Integer stratIndex,
            MeasureGroupStratifierComponent measureStratifier, MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef, List<MeasureGroupPopulationComponent> populations) {
        reportStratifier.setCode(Collections.singletonList(measureStratifier.getCode()));
        reportStratifier.setId(measureStratifier.getId());

        if(measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                this.createStringExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/extension-description",
                        measureStratifier.getDescription()));
        }

        String stratifierKey = this.getKey("stratifier", measureStratifier.getId(), measureStratifier.getCode(),
                stratIndex);

        Map<String, Object> subjectValues = stratifierDef.getSubjectValues();
        Map<Object, List<String>> subjectsByValue = subjectValues.keySet().stream().collect(Collectors.groupingBy(x -> subjectValues.get(x)));
        for (Map.Entry<Object, List<String>> stratValue : subjectsByValue.entrySet()) {
            buildStratum(groupKey, stratifierKey, reportStratifier.addStratum(), stratValue.getKey(),
                    stratValue.getValue(), populations);
        }
    }

    protected void buildStratum(String groupKey, String stratifierKey, StratifierGroupComponent stratum, Object value,
            List<String> subjectIds, List<MeasureGroupPopulationComponent> populations) {

        if (value instanceof Iterable) {
            Iterator<?> iValue = ((Iterable<?>) value).iterator();
            if (iValue.hasNext()) {
                value = iValue.next();
            } else {
                value = null;
            }
        }

        String stratumValue = null;
        String stratumKey = null;
        if (value instanceof Coding) {
            Coding codingValue = ((Coding) value);
            stratumValue = codingValue.getCode();
            stratumKey = codingValue.getDisplay().toLowerCase().trim().replace(" ", "-");
        } else if (value instanceof CodeableConcept) {
            CodeableConcept codeableConceptValue = (CodeableConcept) value;
            codeableConceptValue.hashCode();
            stratumValue = codeableConceptValue.getCodingFirstRep().getCode();
            stratumKey = codeableConceptValue.getCodingFirstRep().getDisplay().toLowerCase().trim().replace(" ", "-");
        } else if (value instanceof Code) {
            Code codeValue = (Code) value;
            stratumValue = codeValue.getCode();
            stratifierKey = codeValue.getDisplay().toLowerCase().trim().replace(" ", "-");
        } else if (value instanceof Enum) {
            stratumValue = ((Enum<?>) value).toString();
        } else if (value instanceof IPrimitiveType<?>) {
            stratumValue = ((IPrimitiveType<?>) value).getValueAsString();
        } else if (value != null) {
            stratumValue = value.toString();
        }

        if (stratumValue != null) {
            stratum.setValue(new CodeableConcept().setText(stratumValue));
        }

        for (MeasureGroupPopulationComponent mgpc : populations) {
            buildStratumPopulation(groupKey, stratifierKey, stratumKey, stratumValue, stratum.addPopulation(),
                    subjectIds, mgpc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void buildStratumPopulation(String groupKey, String stratifierKey, String stratumKey, String stratumValue,
            StratifierGroupPopulationComponent sgpc, List<String> subjectIds,
            MeasureGroupPopulationComponent population) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(
                    this.createStringExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/extension-description",
                            population.getDescription()));
        }

        // This is a temporary resource that was carried by the population component
        Set<String> popSubjectIds = (Set<String>) population.getUserData(POPULATION_SUBJECT_SET);

        if (popSubjectIds == null) {
            sgpc.setCount(0);
            return;
        }

        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        if (intersection.size() > 0) {
            ListResource popSubjectList = this.createIdList("subject-list-" + groupKey + "-" + stratifierKey + "-"
                    + "stratum-" + (stratumKey != null ? stratumKey : stratumValue != null ? stratumValue.toLowerCase() : "null") + "-"
                    + population.getCode().getCodingFirstRep().getCode(), intersection);
            this.report.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference().setReference("#" + popSubjectList.getId()));
        }
    }

    protected void buildPopulation(String groupKey, MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation, PopulationDef populationDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());
        reportPopulation.setCount(populationDef.getSubjects().size());

        if (measurePopulation.hasDescription()) {
            reportPopulation.addExtension(
                    this.createStringExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/extension-description",
                            measurePopulation.getDescription()));
        }

        addResourceReferences(populationDef.getType(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        Set<String> populationSet = populationDef.getSubjects();
        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        switch (this.report.getType()) {
            case SUBJECTLIST:
                if (populationSet.size() > 0) {
                    ListResource subjectList = createIdList(
                            "subject-list-" + groupKey + "-" + populationDef.getType().toCode(), populationSet);
                    this.report.addContained(subjectList);
                    reportPopulation.setSubjectResults(new Reference("#" + subjectList.getId()));
                }
                break;
            default:
                break;
        }

        // Population Type behavior
        switch (populationDef.getType()) {
            case MEASUREOBSERVATION:
                buildMeasureObservations(populationDef.getCriteriaExpression(), populationDef.getResources());
                break;
            default:
                break;
        }
    }

    protected void buildMeasureObservations(String observationName, List<Object> resources) {
        int i = 1;
        for (Object resource : resources) {
            Observation observation = createMeasureObservation("measure-observation-" + observationName + "-" + i,
                    observationName);
            this.report.addContained(observation);
            i++;
        }

        // And then presumably do something with it....
    }

    protected ListResource createList(String id) {
        ListResource list = new ListResource();
        list.setId(id);
        return list;
    }

    protected ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(id, ids.stream().map(x -> new Reference(x)).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    private void addResourceReferences(MeasurePopulationType measurePopulationType, List<Object> evaluatedResources) {
        if (evaluatedResources.size() > 0) {
            for (Object object : evaluatedResources) {
                Resource resource = (Resource) object;
                String resourceId = resource.getId();
                Reference reference = this.getEvaluatedResourceReference(resourceId);
                Extension ext = createStringExtension(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE,
                        measurePopulationType.toCode());
                addExtension(reference, ext);
            }
        }
    }

    protected HashMap<String, Reference> getEvaluatedResourceReferences() {
        if (this.evaluatedResourceReferences == null) {
            this.evaluatedResourceReferences = new HashMap<>();
        }

        return this.evaluatedResourceReferences;
    }

    protected Reference getEvaluatedResourceReference(String id) {
        return this.getEvaluatedResourceReferences().computeIfAbsent(id, k -> new Reference(k));
    }

    protected Pair<String, String> getAccumulatorKeyAndDescription(Object object) {
        if (object instanceof Code) {
            Code codeObject = (Code)object;
            return Pair.of(codeObject.getCode(), codeObject.getDisplay());
        }

        if (object instanceof Coding) {
            Coding codingObject = (Coding)object;
            return Pair.of(codingObject.getCode(), codingObject.getDisplay());
        }

        if (object instanceof CodeableConcept) {
            CodeableConcept codeableConceptObject = (CodeableConcept)object;
            return Pair.of(codeableConceptObject.getCodingFirstRep().getCode(), codeableConceptObject.getText());
        }

        return Pair.of(object.toString(), object.toString());
    }

    protected void processSdes(Measure measure, MeasureDef measureDef, List<String> subjectIds) {
        // ASSUMPTION: Measure SDEs are in the same order as MeasureDef SDEs
        for (int i = 0; i < measure.getSupplementalData().size(); i++) {
            MeasureSupplementalDataComponent msdc = measure.getSupplementalData().get(i);
            SdeDef sde = measureDef.getSdes().get(i);
            Map<Pair<String, String>, Long> accumulated = sde.getValues().stream()
                    .collect(Collectors.groupingBy(x -> getAccumulatorKeyAndDescription(x), Collectors.counting()));

            String sdeKey = this.getKey("sde-observation", msdc.getId(), msdc.getCode(), i);
            String sdeCode = sde.getCode();
            for (Map.Entry<Pair<String, String>, Long> accumulator : accumulated.entrySet()) {

                String valueCode = accumulator.getKey().getLeft();
                String valueKey = accumulator.getKey().getRight();
                Long valueCount = accumulator.getValue();

                if (valueKey == null) {
                    valueKey = valueCode;
                }

                valueKey = valueKey.toLowerCase().trim().replace(" ", "-");

                Coding valueCoding = new Coding().setCode(valueCode);
                if (!sdeCode.equalsIgnoreCase("sde-sex")) {
                    // /**
                    // * Match up the category part of our SDE key (e.g. sde-race has a category of
                    // * race) with a patient extension of the same category (e.g.
                    // * http://hl7.org/fhir/us/core/StructureDefinition/us-core-race) and the same
                    // * code as sdeAccumulatorKey (aka the value extracted from the CQL expression
                    // * named in the Measure SDE metadata) and then record the coding details.
                    // *
                    // * We know that at least one patient matches the sdeAccumulatorKey or else it
                    // * wouldn't show up in the map.
                    // */

                    // String coreCategory = sdeCode
                    //         .substring(sdeCode.lastIndexOf('-') >= 0 ? sdeCode.lastIndexOf('-') + 1 : 0);
                    // for (DomainResource pt : subjects) {
                    //     valueCoding = getExtensionCoding(pt, coreCategory, valueCode);
                    //     if (valueCoding != null) {
                    //         break;
                    //     }
                    // }
                }

                DomainResource obs = null;
                switch (this.report.getType()) {
                    case INDIVIDUAL:
                        obs = createPatientObservation(sdeKey + "-" + valueKey, sdeCode, valueCoding);
                        break;
                    default:
                        obs = createPopulationObservation(sdeKey + "-" + valueKey, sdeCode, valueCoding, valueCount);
                        break;
                }

                // addEvaluatedResource(report, obs);
                // addContained(report, obs);
                // newRefList.add(new Reference("#" + obs.getId()));
                report.addContained(obs);

            }
        }
    }

    protected Period getPeriod(Interval measurementPeriod) {
        if (measurementPeriod.getStart() instanceof DateTime) {
            DateTime dtStart = (DateTime) measurementPeriod.getStart();
            DateTime dtEnd = (DateTime) measurementPeriod.getEnd();

            return new Period().setStart(dtStart.toJavaDate()).setEnd(dtEnd.toJavaDate());

        } else if (measurementPeriod.getStart() instanceof Date) {
            return new Period().setStart((Date) measurementPeriod.getStart()).setEnd((Date) measurementPeriod.getEnd());
        } else {
            throw new IllegalArgumentException(
                    "Measurement period should be an interval of CQL DateTime or Java Date objects");
        }
    }

    protected MeasureReport createMeasureReport(Measure measure, MeasureReportType type, List<String> subjectIds,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));
        ;
        if (type == MeasureReportType.INDIVIDUAL && !subjectIds.isEmpty()) {
            report.setSubject(new Reference(subjectIds.get(0)));
        }

        report.setPeriod(getPeriod(measurementPeriod));
        report.setMeasure(measure.getUrl());
        report.setDate(new Date());
        report.setImplicitRules(measure.getImplicitRules());
        report.setImprovementNotation(measure.getImprovementNotation());
        report.setLanguage(measure.getLanguage());

        if (measure.hasDescription()) {
            report.addExtension(this.createStringExtension(
                    "http://hl7.org/fhir/uv/cpg/StructureDefinition/extension-description", measure.getDescription()));
        }

        // TODO: Allow a way to pass in or set a default reporter
        // report.setReporter(value)

        return report;
    }
    // /**
    // * Retrieve the coding from an extension that that looks like the following...
    // *
    // * { "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race",
    // * "extension": [ { "url": "ombCategory", "valueCoding": { "system":
    // * "urn:oid:2.16.840.1.113883.6.238", "code": "2054-5", "display": "Black or
    // * African American" } } ] }
    // */

    protected Coding getExtensionCoding(DomainResource patient, String coreCategory, String sdeCode) {
        Coding valueCoding = new Coding();

        patient.getExtension().forEach((ptExt) -> {
            if (ptExt.getUrl().contains(coreCategory)) {
                Coding extCoding = (Coding) ptExt.getExtension().get(0).getValue();
                String extCode = extCoding.getCode();
                if (extCode.equalsIgnoreCase(sdeCode)) {
                    valueCoding.setSystem(extCoding.getSystem());
                    valueCoding.setCode(extCoding.getCode());
                    valueCoding.setDisplay(extCoding.getDisplay());
                }
            }
        });

        return valueCoding;
    }

    // protected Extension createCodingExtension(String url, String codeSystem,
    // String code) {
    // Extension ext = new Extension().setUrl(url);
    // Coding coding = new Coding().setSystem(codeSystem).setCode(code);
    // ext.setValue(coding);
    // return ext;
    // }

    protected Extension createStringExtension(String url, String value) {
        Extension ext = new Extension().setUrl(url);
        ext.setValue(new StringType(value));
        return ext;
    }

    protected void addExtension(Reference reference, Extension extension) {
        List<Extension> extensions = reference.getExtensionsByUrl(extension.getUrl());
        for (Extension e : extensions) {
            if (e.getValue().equalsShallow(extension.getValue())) {
                return;
            }
        }

        reference.addExtension(extension);
    }

    protected void setEvaluatedResources(MeasureReport report, Collection<Reference> evaluatedResources) {
        report.getEvaluatedResource().addAll(evaluatedResources);
    }

    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure = new Extension().setUrl(MeasureInfo.MEASURE)
                .setValue(new CanonicalType(measureInfo.getMeasure()));

        Extension extExtPop = new Extension().setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);
        obsExtension.addExtension(extExtPop);

        return obsExtension;
    }

    protected DomainResource createPopulationObservation(String id, String populationId, Coding valueCoding,
            Long sdeAccumulatorValue) {

        Observation obs = createObservation(id, populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));

        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(sdeAccumulatorValue));

        return obs;
    }

    protected DomainResource createPatientObservation(String id, String populationId, Coding valueCoding) {

        Observation obs = createObservation(id, populationId);

        CodeableConcept codeCodeableConcept = new CodeableConcept().setText(populationId);
        obs.setCode(codeCodeableConcept);

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    protected Observation createObservation(String id, String populationId) {
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(this.measure.hasUrl() ? measure.getUrl()
                        : (measure.hasId() ? MeasureInfo.MEASURE_PREFIX + measure.getIdElement().getIdPart() : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    protected Observation createMeasureObservation(String id, String observationName) {
        Observation obs = this.createObservation(id, observationName);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        return obs;

    }

}
