package org.opencds.cqf.fhir.cr.measure.dstu3;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureInfo;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;

public class Dstu3MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static final String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";
    protected static final String EXT_POPULATION_DESCRIPTION_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description";
    protected static final String EXT_SDE_REFERENCE_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference";
    protected static final String POPULATION_BASIS_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";

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
    public MeasureReport build(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds) {
        this.reset();

        this.measure = measure;
        this.report = this.createMeasureReport(measure, measureDef, measureReportType, subjectIds, measurementPeriod);

        buildGroups(measure, measureDef);
        processSdes(measure, measureDef, subjectIds);

        this.measureReportScorer.score(measureDef.scoring(), this.report);

        // Only add evaluated resources to individual reports
        if (measureReportType == MeasureReportType.INDIVIDUAL) {
            ListResource references = this.createReferenceList(
                    "evaluated-resources-references",
                    this.getEvaluatedResourceReferences().values());
            this.report.addContained(references);
            this.report.setEvaluatedResources(new Reference("#" + references.getId()));
        }

        return this.report;
    }

    protected void buildGroups(Measure measure, MeasureDef measureDef) {
        if (measure.getGroup().size() != measureDef.groups().size()) {
            throw new IllegalArgumentException(
                    "The Measure has a different number of groups defined than the MeasureDef");
        }

        // ASSUMPTION: The groups are in the same order in both the Measure and the
        // MeasureDef
        for (int i = 0; i < measure.getGroup().size(); i++) {
            MeasureGroupComponent mgc = measure.getGroup().get(i);
            String groupKey = this.getKey("group", mgc.getId(), null, i);
            buildGroup(
                    measureDef,
                    groupKey,
                    mgc,
                    this.report.addGroup(),
                    measureDef.groups().get(i));
        }
    }

    protected void buildGroup(
            MeasureDef measureDef,
            String groupKey,
            MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup,
            GroupDef groupDef) {
        // groupDef contains populations/stratifier components not defined in measureGroup (TOTAL-NUMERATOR &
        // TOTAL-DENOMINATOR), and will not be added to group populations.
        // Subtracting '2' from groupDef to balance with Measure defined Groups
        if (measureGroup.getPopulation().size() != (groupDef.populations().size() - 2)) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef");
        }

        if (measureGroup.getStratifier().size() != groupDef.stratifiers().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef");
        }

        reportGroup.setId(measureGroup.getId());

        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    this.createStringExtension(EXT_POPULATION_DESCRIPTION_URL, measureGroup.getDescription()));
        }

        for (MeasureGroupPopulationComponent mgpc : measureGroup.getPopulation()) {
            buildPopulation(
                    measureDef,
                    groupKey,
                    mgpc,
                    reportGroup.addPopulation(),
                    groupDef.getSingle(MeasurePopulationType.fromCode(
                            mgpc.getCode().getCodingFirstRep().getCode())));
        }

        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            buildStratifier(
                    groupKey,
                    i,
                    measureGroup.getStratifier().get(i),
                    reportGroup.addStratifier(),
                    groupDef.stratifiers().get(i),
                    measureGroup.getPopulation());
        }
    }

    protected String getKey(String prefix, String id, CodeableConcept code, Integer index) {
        if (id != null) {
            return prefix + "-" + id;
        }

        // TODO: Consider using coding.code as a fallback
        if (code != null && code.hasText()) {
            return prefix + "-" + this.escapeForFhirId(code.getText());
        }

        return prefix + "-" + Integer.toString(index + 1);
    }

    protected void buildStratifier(
            String groupKey,
            Integer stratIndex,
            MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations) {
        reportStratifier.setId(measureStratifier.getId());

        // NOTE: DSTU3 does not support descriptions

        String stratifierKey = this.getKey("stratifier", measureStratifier.getId(), null, stratIndex);

        Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        // Because most of the types we're dealing with don't implement hashCode or
        // equals
        // the ValueWrapper does it for them.
        Map<ValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(
                        x -> new ValueWrapper(subjectValues.get(x).rawValue())));

        for (Map.Entry<ValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            buildStratum(
                    groupKey,
                    stratifierKey,
                    reportStratifier.addStratum(),
                    stratValue.getKey(),
                    stratValue.getValue(),
                    populations);
        }
    }

    protected void buildStratum(
            String groupKey,
            String stratifierKey,
            StratifierGroupComponent stratum,
            ValueWrapper value,
            List<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations) {

        String stratumKey = this.escapeForFhirId(value.getKey());

        stratum.setValue(value.getValueAsString());

        for (MeasureGroupPopulationComponent mgpc : populations) {
            buildStratumPopulation(groupKey, stratifierKey, stratumKey, stratum.addPopulation(), subjectIds, mgpc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void buildStratumPopulation(
            String groupKey,
            String stratifierKey,
            String stratumKey,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            MeasureGroupPopulationComponent population) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(this.createStringExtension(EXT_POPULATION_DESCRIPTION_URL, population.getDescription()));
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

        if (intersection.size() > 0
                && this.report.getType() == org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType.PATIENTLIST) {
            ListResource popSubjectList = this.createIdList(
                    "subject-list-" + groupKey + "-" + stratifierKey + "-" + "stratum-" + stratumKey + "-"
                            + population.getCode().getCodingFirstRep().getCode(),
                    intersection);
            this.report.addContained(popSubjectList);
            sgpc.setPatients(new Reference().setReference("#" + popSubjectList.getId()));
        }
    }

    protected void buildPopulation(
            MeasureDef measureDef,
            String groupKey,
            MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation,
            PopulationDef populationDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());

        if (measureDef.isBooleanBasis()) {
            reportPopulation.setCount(populationDef.getResources().size());
        } else {
            reportPopulation.setCount(populationDef.getSubjects().size());
        }

        if (measurePopulation.hasDescription()) {
            reportPopulation.addExtension(
                    this.createStringExtension(EXT_POPULATION_DESCRIPTION_URL, measurePopulation.getDescription()));
        }

        addResourceReferences(populationDef.type(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        Set<String> populationSet = populationDef.getSubjects();
        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        switch (this.report.getType()) {
            case PATIENTLIST:
                if (populationSet.size() > 0) {
                    ListResource subjectList = createIdList(
                            "subject-list-" + groupKey + "-"
                                    + populationDef.type().toCode(),
                            populationSet);
                    this.report.addContained(subjectList);
                    reportPopulation.setPatients(new Reference("#" + subjectList.getId()));
                }

                break;
            default:
                break;
        }

        // Population Type behavior
        switch (populationDef.type()) {
            case MEASUREOBSERVATION:
                buildMeasureObservations(populationDef.expression(), populationDef.getResources());
                break;
            default:
                break;
        }
    }

    protected void buildMeasureObservations(String observationName, Set<Object> resources) {
        for (int i = 0; i < resources.size(); i++) {
            // TODO: Do something with the resource...
            Observation observation =
                    createMeasureObservation("measure-observation-" + observationName + "-" + (i + 1), observationName);
            this.report.addContained(observation);
        }
    }

    protected ListResource createList(String id) {
        ListResource list = new ListResource();
        list.setId(id);
        return list;
    }

    protected ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(
                id, ids.stream().map(x -> new Reference(x)).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    private void addResourceReferences(MeasurePopulationType measurePopulationType, Set<Object> evaluatedResources) {
        if (!evaluatedResources.isEmpty()) {
            for (Object object : evaluatedResources) {
                Resource resource = (Resource) object;
                String resourceId = resource.getId();
                Reference reference = this.getEvaluatedResourceReference(resourceId);
                Extension ext = createStringExtension(
                        MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE, measurePopulationType.toCode());
                addExtensionToReference(reference, ext);
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
        return this.getEvaluatedResourceReferences().computeIfAbsent(id, x -> new Reference(id));
    }

    protected void processSdes(Measure measure, MeasureDef measureDef, List<String> subjectIds) {
        // ASSUMPTION: Measure SDEs are in the same order as MeasureDef SDEs
        for (int i = 0; i < measure.getSupplementalData().size(); i++) {
            MeasureSupplementalDataComponent msdc =
                    measure.getSupplementalData().get(i);
            SdeDef sde = measureDef.sdes().get(i);

            processSdeEvaluatedResourceExtension(sde);

            Map<ValueWrapper, Long> accumulated = sde.getResults().values().stream()
                    .flatMap(x -> ((List<Object>) Lists.newArrayList(x.iterableValue())).stream())
                    .map(ValueWrapper::new)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            String sdeKey = this.getKey("sde-observation", msdc.getId(), null, i);
            String sdeId = sde.id();
            for (Map.Entry<ValueWrapper, Long> accumulator : accumulated.entrySet()) {

                String valueCode = accumulator.getKey().getValueAsString();
                String valueKey = accumulator.getKey().getKey();
                Long valueCount = accumulator.getValue();

                if (valueKey == null) {
                    valueKey = valueCode;
                }

                valueKey = this.escapeForFhirId(valueKey);

                Coding valueCoding = new Coding().setCode(valueCode);
                if (!sdeId.equalsIgnoreCase("sde-sex")) {
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
                    // .substring(sdeCode.lastIndexOf('-') >= 0 ? sdeCode.lastIndexOf('-') + 1 : 0);
                    // for (DomainResource pt : subjectIds) {
                    // valueCoding = getExtensionCoding(pt, coreCategory, valueCode);
                    // if (valueCoding != null) {
                    // break;
                    // }
                    // }
                }

                DomainResource obs = null;
                switch (this.report.getType()) {
                    case INDIVIDUAL:
                        obs = createPatientObservation(sdeKey + "-" + valueKey, sdeId, valueCoding);
                        break;
                    default:
                        obs = createPopulationObservation(sdeKey + "-" + valueKey, sdeId, valueCoding, valueCount);
                        break;
                }

                report.addExtension(this.createReferenceExtension(EXT_SDE_REFERENCE_URL, "#" + obs.getId()));
                report.addContained(obs);
            }
        }
    }

    private void processSdeEvaluatedResourceExtension(SdeDef sdeDef) {
        for (CriteriaResult r : sdeDef.getResults().values()) {
            for (Object o : r.evaluatedResources()) {
                if (o instanceof IBaseResource) {
                    // extension item
                    Extension extension = new Extension(MeasureConstants.EXT_SDE_URL);
                    IBaseResource iBaseResource = (IBaseResource) o;

                    // adding value to extension
                    extension.setValue(new StringType(
                            new StringBuilder(iBaseResource.getIdElement().getResourceType())
                                    .append("/")
                                    .append(iBaseResource.getIdElement().getIdPart())
                                    .toString()));

                    // adding item extension to MR extension list
                    report.getExtension().add(extension);
                }
            }
        }
    }

    protected Period getPeriod(Interval measurementPeriod) {
        if (measurementPeriod.getStart() instanceof DateTime) {
            DateTime dtStart = (DateTime) measurementPeriod.getStart();
            DateTime dtEnd = (DateTime) measurementPeriod.getEnd();

            return new Period().setStart(dtStart.toJavaDate()).setEnd(dtEnd.toJavaDate());

        } else if (measurementPeriod.getStart() instanceof Date) {
            Date dStart = (Date) measurementPeriod.getStart();
            Date dEnd = (Date) measurementPeriod.getEnd();
            return new Period().setStart(dStart.toJavaDate()).setEnd(dEnd.toJavaDate());
        } else {
            throw new IllegalArgumentException("Measurement period should be an interval of CQL DateTime or Date");
        }
    }

    protected MeasureReport createMeasureReport(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType type,
            List<String> subjectIds,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.fromCode("complete"));
        report.setType(org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));

        if (type == MeasureReportType.INDIVIDUAL && !subjectIds.isEmpty()) {
            report.setPatient(new Reference(subjectIds.get(0)));
        }

        if (measurementPeriod != null) {
            report.setPeriod(getPeriod(measurementPeriod));
        } else if (measureDef.getDefaultMeasurementPeriod() != null) {
            report.setPeriod(getPeriod(measureDef.getDefaultMeasurementPeriod()));
        }

        report.setMeasure(new Reference(measure.getId()));
        report.setDate(new java.util.Date());
        report.setImplicitRules(measure.getImplicitRules());
        report.setLanguage(measure.getLanguage());

        if (measure.hasDescription()) {
            report.addExtension(this.createStringExtension(EXT_POPULATION_DESCRIPTION_URL, measure.getDescription()));
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

    protected Extension createReferenceExtension(String url, String reference) {
        Extension ext = new Extension().setUrl(url);
        ext.setValue(new Reference(reference));

        return ext;
    }

    protected void addExtensionToReference(Reference reference, Extension extension) {
        List<Extension> extensions = reference.getExtensionsByUrl(extension.getUrl());
        for (Extension e : extensions) {
            if (e.getValue().equalsShallow(extension.getValue())) {
                return;
            }
        }

        reference.addExtension(extension);
    }

    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure =
                new Extension().setUrl(MeasureInfo.MEASURE).setValue(new IdType(measureInfo.getMeasure()));

        Extension extExtPop = new Extension()
                .setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);
        obsExtension.addExtension(extExtPop);

        return obsExtension;
    }

    protected DomainResource createPopulationObservation(
            String id, String populationId, Coding valueCoding, Long sdeAccumulatorValue) {

        Observation obs = createObservation(id, populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        obsCodeableConcept.setCoding(Collections.singletonList(valueCoding));

        obs.setCode(obsCodeableConcept);
        obs.setValue(new Quantity().setValue(sdeAccumulatorValue));

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
                .withMeasure(
                        this.measure.hasUrl()
                                ? measure.getUrl()
                                : (measure.hasId()
                                        ? MeasureInfo.MEASURE_PREFIX
                                                + measure.getIdElement().getIdPart()
                                        : ""))
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

    protected String escapeForFhirId(String value) {
        if (value == null) {
            return null;
        }

        return value.toLowerCase().trim().replace(" ", "-").replace("_", "-");
    }

    // This is some hackery because most of these objects don't implement
    // hashCode or equals, meaning it's hard to detect distinct values;
    class ValueWrapper {
        protected Object value;

        public ValueWrapper(Object value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.getKey().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;

            ValueWrapper other = (ValueWrapper) o;

            if (other.getValue() == null ^ this.getValue() == null) {
                return false;
            }

            if (other.getValue() == null && this.getValue() == null) {
                return true;
            }

            return this.getKey().equals(other.getKey());
        }

        public String getKey() {
            String key = null;
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                // ASSUMPTION: We won't have different systems with the same code
                // within a given stratifier / sde
                key = joinValues("coding", c.getCode());
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                key = joinValues("codeable-concept", c.getCodingFirstRep().getCode());
            } else if (value instanceof Code) {
                Code c = (Code) value;
                key = joinValues("code", c.getCode());
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                key = joinValues("enum", e.toString());
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                key = joinValues("primitive", p.getValueAsString());
            } else if (value instanceof Identifier) {
                key = ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                key = ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                key = value.toString();
            }

            if (key == null) {
                throw new IllegalArgumentException(String.format("found a null key for the wrapped value: %s", value));
            }

            return key;
        }

        public String getValueAsString() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value != null) {
                return value.toString();
            } else {
                return "<null>";
            }
        }

        public String getDescription() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.hasDisplay() ? c.getDisplay() : c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().hasDisplay()
                        ? c.getCodingFirstRep().getDisplay()
                        : c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getDisplay() != null ? c.getDisplay() : c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value != null) {
                return value.toString();
            } else {
                return null;
            }
        }

        public Object getValue() {
            return this.value;
        }

        public Class<?> getValueClass() {
            return this.value.getClass();
        }

        private String joinValues(String... elements) {
            return String.join("-", elements);
        }
    }
}
