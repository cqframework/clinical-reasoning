package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
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

public class Dstu3MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    protected MeasureReportScorer<MeasureReport> measureReportScorer;

    public Dstu3MeasureReportBuilder() {
        this.measureReportScorer = new Dstu3MeasureReportScorer();
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
            Interval measurementPeriod, List<DomainResource> subjects) {
        this.reset();

        this.measure = measure;
        this.report = this.createMeasureReport(measure, measureReportType, subjects, measurementPeriod);

        buildGroups(measure, measureDef);
        processSdes(measure, measureDef, subjects);

        this.measureReportScorer.score(measureDef.getMeasureScoring(), this.report);

        ListResource references = this.createReferenceList(this.getEvaluatedResourceReferences().values());
        this.report.addContained(references);
        this.report.setEvaluatedResources(new Reference("#" + references.getId()));

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
            buildGroup(measure.getGroup().get(i), this.report.addGroup(), measureDef.getGroups().get(i));
        }
    }

    protected void buildGroup(MeasureGroupComponent measureGroup, MeasureReportGroupComponent reportGroup,
            GroupDef groupDef) {
        if (measureGroup.getPopulation().size() != groupDef.values().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef");
        }

        if (measureGroup.getStratifier().size() != groupDef.getStratifiers().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef");
        }

        reportGroup.setId(measureGroup.getId());

        for (MeasureGroupPopulationComponent mgpc : measureGroup.getPopulation()) {
            buildPopulation(mgpc, reportGroup.addPopulation(),
                    groupDef.get(MeasurePopulationType.fromCode(mgpc.getCode().getCodingFirstRep().getCode())));
        }

        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            buildStratifier(measureGroup.getStratifier().get(0), reportGroup.addStratifier(),
                    groupDef.getStratifiers().get(0), measureGroup.getPopulation());
        }
    }

    protected void buildStratifier(MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier, StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations) {
        reportStratifier.setId(measureStratifier.getId());

        Map<Object, Set<String>> values = stratifierDef.getValues();
        for (Map.Entry<Object, Set<String>> stratValue : values.entrySet()) {
            buildStratum(reportStratifier.addStratum(), stratValue.getKey(), stratValue.getValue(), populations);
        }
    }

    protected void buildStratum(StratifierGroupComponent stratum, Object value, Set<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations) {

        String stratumValue = null;
        if (value instanceof Coding) {
            stratumValue = ((Coding) value).getCode();
        } else if (value instanceof CodeableConcept) {
            stratumValue = ((CodeableConcept) value).getCodingFirstRep().getCode();
        } else if (value instanceof Code) {
            stratumValue = ((Code) value).getCode();
        } else if (value instanceof Enum) {
            stratumValue = ((Enum<?>) value).toString();
        } else if (value instanceof IPrimitiveType<?>) {
            stratumValue = ((IPrimitiveType<?>) value).getValueAsString();
        } else if (value != null) {
            stratumValue = value.toString();
        }

        stratum.setValue(stratumValue);

        for (MeasureGroupPopulationComponent mgpc : populations) {
            buildStratumPopulation(stratum.addPopulation(), subjectIds, mgpc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void buildStratumPopulation(StratifierGroupPopulationComponent sgpc, Set<String> subjectIds,
            MeasureGroupPopulationComponent population) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        // This is a temporary resource that was carried by the population component
        Set<String> popSubjectIds = (Set<String>) population.getUserData(POPULATION_SUBJECT_SET);

        if (popSubjectIds == null) {
            return;
        }

        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        ListResource popSubjectList = this.createIdList(intersection);
        this.report.addContained(popSubjectList);
        sgpc.setPatients(new Reference().setReference("#" + popSubjectList.getId()));
    }

    protected void buildPopulation(MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation, PopulationDef populationDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());
        reportPopulation.setCount(populationDef.getSubjects().size());

        addResourceReferences(populationDef.getType(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        Set<String> populationSet = populationDef.getSubjects().stream().map(x -> ((Resource) x).getId())
                .collect(Collectors.toSet());
        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        switch (this.report.getType()) {
            case PATIENTLIST:
                ListResource subjectList = createIdList(populationSet);
                this.report.addContained(subjectList);
                reportPopulation.setPatients(new Reference("#" + subjectList.getId()));
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

    protected void buildMeasureObservations(String observationName, Set<Object> resources) {
        for (Object resource : resources) {
            Observation observation = createMeasureObservation(observationName);
            this.report.addContained(observation);
        }

        // And then presumably do something with it....
    }

    protected ListResource createList() {
        ListResource list = new ListResource();
        list.setId(UUID.randomUUID().toString());
        return list;
    }

    protected ListResource createSubjectList(Set<Object> subjects) {
        return this.createIdList(subjects.stream().map(x -> ((DomainResource) x).getId()).collect(Collectors.toList()));
    }

    protected ListResource createIdList(Collection<String> ids) {
        return this.createReferenceList(ids.stream().map(x -> new Reference(x)).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(Collection<Reference> references) {
        ListResource referenceList = createList();
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    private void addResourceReferences(MeasurePopulationType measurePopulationType, Set<Object> evaluatedResources) {
        if (evaluatedResources.size() > 0) {
            ListResource list = createList();
            for (Object object : evaluatedResources) {
                Resource resource = (Resource) object;
                String resourceId = resource.getId();
                Reference reference = this.getEvaluatedResourceReference(resourceId);
                Extension ext = createStringExtension(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE,
                        measurePopulationType.toCode());
                addExtension(reference, ext);
                list.addEntry().setItem(reference);
            }

            // TODO: What to do with the evaluated resource list?
            evaluatedResources.add(list);
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

    protected String getAccumulatorKey(Object object) {
        if (object instanceof Code) {
            return ((Code) object).getCode();
        }

        if (object instanceof Coding) {
            return ((Coding) object).getCode();
        }

        return object.toString();
    }

    protected void processSdes(Measure measure, MeasureDef measureDef, List<DomainResource> subjects) {
        for (SdeDef sde : measureDef.getSdes()) {
            Map<String, Long> accumulated = sde.getValues().stream()
                    .collect(Collectors.groupingBy(x -> getAccumulatorKey(x), Collectors.counting()));

            for (Map.Entry<String, Long> accumulator : accumulated.entrySet()) {
                String accumulatorCode = accumulator.getKey();
                Long accumulatorValue = accumulator.getValue();
                Coding valueCoding = new Coding().setCode(accumulatorCode);
                String sdeKey = sde.getCode();

                if (sdeKey != null && !sdeKey.equalsIgnoreCase("sde-sex")) {

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

                    String coreCategory = sdeKey
                            .substring(sdeKey.lastIndexOf('-') >= 0 ? sdeKey.lastIndexOf('-') + 1 : 0);
                    for (DomainResource pt : subjects) {
                        valueCoding = getExtensionCoding(pt, coreCategory, accumulatorCode);

                        // TODO - Is there any reason to continue looking at additional patients? The
                        // original
                        // cqf-ruler implementation would use the last matching patient's data vs.
                        // the
                        // first.
                        if (valueCoding != null) {
                            break;
                        }
                    }
                }

                DomainResource obs = null;
                switch (this.report.getType()) {
                    case INDIVIDUAL:
                        obs = createPatientObservation(sdeKey, valueCoding);
                        break;
                    default:
                        obs = createPopulationObservation(sdeKey, valueCoding, accumulatorValue);
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

    protected MeasureReport createMeasureReport(Measure measure, MeasureReportType type, List<DomainResource> subjects,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
        report.setType(org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));
        ;
        if (type == MeasureReportType.INDIVIDUAL && !subjects.isEmpty()) {
            report.setPatient(new Reference(subjects.get(0).getId()));
        }

        report.setPeriod(getPeriod(measurementPeriod));
        report.setMeasure(new Reference(measure.getId()));
        report.setDate(new Date());
        report.setImplicitRules(measure.getImplicitRules());
        report.setLanguage(measure.getLanguage());

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

    protected ListResource createListResource(Collection<ListEntryComponent> entries) {
        ListResource resource = new ListResource();
        resource.setId(UUID.randomUUID().toString());
        resource.getEntry().addAll(entries);
        return resource;
    }

    protected ListEntryComponent createListEntry(Reference reference) {
        return new ListEntryComponent().setItem(reference);
    }

    protected void addExtension(Reference reference, Extension extension) {
        reference.addExtension(extension);
    }

    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure = new Extension().setUrl(MeasureInfo.MEASURE)
                .setValue(new IdType(measureInfo.getMeasure()));

        Extension extExtPop = new Extension().setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);
        obsExtension.addExtension(extExtPop);

        return obsExtension;
    }

    protected DomainResource createPopulationObservation(String populationId, Coding valueCoding,
            Long sdeAccumulatorValue) {

        Observation obs = createObservation(populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));

        obs.setCode(obsCodeableConcept);
        obs.setValue(new Quantity().setValue(sdeAccumulatorValue));

        return obs;
    }

    protected DomainResource createPatientObservation(String populationId, Coding valueCoding) {

        Observation obs = createObservation(populationId);

        CodeableConcept codeCodeableConcept = new CodeableConcept().setText(populationId);
        obs.setCode(codeCodeableConcept);

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    protected Observation createObservation(String populationId) {
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(this.measure.hasUrl() ? measure.getUrl()
                        : (measure.hasId() ? MeasureInfo.MEASURE_PREFIX + measure.getIdElement().getIdPart() : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(UUID.randomUUID().toString());
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    protected Observation createMeasureObservation(String observationName) {
        Observation obs = this.createObservation(observationName);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        return obs;

    }

}
