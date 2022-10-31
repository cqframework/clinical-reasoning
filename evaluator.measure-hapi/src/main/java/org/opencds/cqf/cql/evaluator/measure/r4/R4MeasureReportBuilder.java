package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
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
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.measure.common.CodeDef;
import org.opencds.cqf.cql.evaluator.measure.common.ConceptDef;
import org.opencds.cqf.cql.evaluator.measure.common.CriteriaResult;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureInfo;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportScorer;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.common.PopulationDef;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

public class R4MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static final String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    protected static final String MISSING_ID_NO_CRITERIA_REF_EXT = String.join("Id for a Measure element is null.",
            "Unable to create criteriaReference extensions.",
            "Ensure all groups, populations, SDEs, and stratifiers",
            "in your Measure resource have ids set.", " ");

    protected MeasureReportScorer<MeasureReport> measureReportScorer;

    public R4MeasureReportBuilder() {
        this.measureReportScorer = new R4MeasureReportScorer();
    }

    private static class BuilderContext {
        private final Measure measure;
        private final MeasureDef measureDef;
        private final MeasureReport measureReport;

        private final HashMap<String, Reference> evaluatedResourceReferences = new HashMap<>();
        private final Set<String> issues = new HashSet<>();

        public BuilderContext(Measure measure, MeasureDef measureDef, MeasureReport measureReport) {
            this.measure = measure;
            this.measureDef = measureDef;
            this.measureReport = measureReport;
        }

        public Measure measure() {
            return this.measure;
        }

        public MeasureReport report() {
            return this.measureReport;
        }

        public MeasureDef measureDef() {
            return this.measureDef;
        }

        public Map<String, Reference> evaluatedResourceReferences() {
            return this.evaluatedResourceReferences;
        }

        public boolean hasEvaluatedResourceReference(String id) {
            return this.evaluatedResourceReferences().containsKey(id);
        }

        public Reference createEvaluatedResourceReference(String id) {
            return this.evaluatedResourceReferences().computeIfAbsent(id, Reference::new);
        }

        public void addIssue(String issue) {
            this.issues.add(issue);
        }

        public Set<String> issues() {
            return this.issues;
        }

    }

    @Override
    public MeasureReport build(Measure measure, MeasureDef measureDef, MeasureReportType measureReportType,
            Interval measurementPeriod, List<String> subjectIds) {

        var report = this.createMeasureReport(measure, measureReportType, subjectIds, measurementPeriod);

        var bc = new BuilderContext(measure, measureDef, report);

        // buildGroups must be run first to set up the builder context to be able to use
        // the evaluatedResource references for SDE processing
        buildGroups(bc);

        buildSDEs(bc);

        addEvaluatedResource(bc);
        // addIssues(bc);

        this.measureReportScorer.score(measureDef.scoring(), bc.report());

        return bc.report();
    }

    protected void addEvaluatedResource(BuilderContext bc) {
        var report = bc.report();
        // Only add evaluated resources to individual reports
        if (report.getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.INDIVIDUAL) {
            for (Reference r : bc.evaluatedResourceReferences().values()) {
                report.addEvaluatedResource(r);
            }
        }
    }

    protected void addIssues(BuilderContext bc) {
        if (bc.issues().isEmpty()) {
            return;
        }

        var report = bc.report();
        OperationOutcome oc = new OperationOutcome();
        oc.setId(UUID.randomUUID().toString());

        for (String issue : bc.issues()) {
            oc.addIssue().setSeverity(IssueSeverity.WARNING).setDiagnostics(issue);
        }

        report.addContained(oc);
        report.addExtension(MeasureConstants.EXT_OPERATION_OUTCOME_REFERENCE_URL, new Reference("#" + oc.getId()));
    }

    protected void buildGroups(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        var report = bc.report();

        if (measure.getGroup().size() != measureDef.groups().size()) {
            throw new IllegalArgumentException(
                    "The Measure has a different number of groups defined than the MeasureDef");
        }

        // ASSUMPTION: The groups are in the same order in both the Measure and the
        // MeasureDef
        for (int i = 0; i < measure.getGroup().size(); i++) {
            var measureGroup = measure.getGroup().get(i);
            var defGroup = measureDef.groups().get(i);
            var reportGroup = report.addGroup();
            buildGroup(bc, measureGroup, reportGroup, defGroup);
        }
    }

    protected void buildGroup(BuilderContext bc, MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        if (measureGroup.getPopulation().size() != groupDef.populations().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef");
        }

        if (measureGroup.getStratifier().size() != groupDef.stratifiers().size()) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef");
        }

        reportGroup.setCode(measureGroup.getCode());
        reportGroup.setId(measureGroup.getId());

        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureGroup.getDescription()));
        }

        for (int i = 0; i < measureGroup.getPopulation().size(); i++) {
            var measurePop = measureGroup.getPopulation().get(i);
            var defPop = groupDef.populations().get(i);
            var reportPop = reportGroup.addPopulation();
            buildPopulation(bc, measurePop, reportPop, defPop);
        }

        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            var groupStrat = measureGroup.getStratifier().get(i);
            var reportStrat = reportGroup.addStratifier();
            var defStrat = groupDef.stratifiers().get(i);
            buildStratifier(bc, groupStrat, reportStrat, defStrat, measureGroup.getPopulation());
        }
    }

    protected void buildStratifier(BuilderContext bc,
            MeasureGroupStratifierComponent measureStratifier, MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef, List<MeasureGroupPopulationComponent> populations) {
        reportStratifier.setCode(Collections.singletonList(measureStratifier.getCode()));
        reportStratifier.setId(measureStratifier.getId());

        if (measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureStratifier.getDescription()));
        }

        Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        // Because most of the types we're dealing with don't implement hashCode or
        // equals
        // the ValueWrapper does it for them.
        Map<ValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(x -> new ValueWrapper(subjectValues.get(x).value())));

        for (Map.Entry<ValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            var reportStratum = reportStratifier.addStratum();
            buildStratum(bc, reportStratum, stratValue.getKey(),
                    stratValue.getValue(), populations);
        }
    }

    protected void buildStratum(BuilderContext bc,
            StratifierGroupComponent stratum,
            ValueWrapper value, List<String> subjectIds, List<MeasureGroupPopulationComponent> populations) {

        if (value.getValueClass().equals(CodeableConcept.class)) {
            stratum.setValue((CodeableConcept) value.getValue());
        } else {
            stratum.setValue(new CodeableConcept().setText(value.getValueAsString()));
        }

        for (MeasureGroupPopulationComponent mgpc : populations) {
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(bc, stratumPopulation, subjectIds, mgpc);
        }
    }

    protected void buildStratumPopulation(BuilderContext bc,
            StratifierGroupPopulationComponent sgpc, List<String> subjectIds,
            MeasureGroupPopulationComponent population) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(population.getDescription()));
        }

        // This is a temporary resource that was carried by the population component
        @SuppressWarnings("unchecked")
        Set<String> popSubjectIds = (Set<String>) population.getUserData(POPULATION_SUBJECT_SET);

        if (popSubjectIds == null) {
            sgpc.setCount(0);
            return;
        }

        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        if (!intersection.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList = this.createIdList(UUID.randomUUID().toString(), intersection);
            bc.report().addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference().setReference("#" + popSubjectList.getId()));
        }
    }

    protected void buildPopulation(BuilderContext bc,
            MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation, PopulationDef populationDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());

        if (ifIsBooleanBasis(bc.measure())) {
            reportPopulation.setCount(populationDef.getResources().size());
        } else {
            reportPopulation.setCount(populationDef.getSubjects().size());
        }

        if (measurePopulation.hasDescription()) {
            reportPopulation.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measurePopulation.getDescription()));
        }

        addResourceReferences(bc, populationDef.id(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        Set<String> populationSet = populationDef.getSubjects();
        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        switch (bc.report().getType()) {
            case SUBJECTLIST:
                if (!populationSet.isEmpty()) {
                    ListResource subjectList = createIdList(
                            UUID.randomUUID().toString(), populationSet);
                    bc.report().addContained(subjectList);
                    reportPopulation.setSubjectResults(new Reference("#" + subjectList.getId()));
                }
                break;
            default:
                break;
        }

        // Population Type behavior
        switch (populationDef.type()) {
            case MEASUREOBSERVATION:
                buildMeasureObservations(bc, populationDef.expression(), populationDef.getResources());
                break;
            default:
                break;
        }
    }

    protected void buildMeasureObservations(BuilderContext bc, String observationName, List<Object> resources) {
        for (int i = 0; i < resources.size(); i++) {
            // TODO: Do something with the resource...
            Observation observation = createMeasureObservation(bc,
                    "measure-observation-" + observationName + "-" + (i + 1),
                    observationName);
            bc.report().addContained(observation);
        }
    }

    protected ListResource createList(String id) {
        ListResource list = new ListResource();
        list.setId(id);
        return list;
    }

    protected ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(id, ids.stream().map(Reference::new).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    protected void addResourceReferences(BuilderContext bc, String id,
            List<Object> evaluatedResources) {
        if (evaluatedResources == null || evaluatedResources.isEmpty()) {
            return;
        }

        if (id == null) {
            bc.addIssue(MISSING_ID_NO_CRITERIA_REF_EXT);
        }

        for (Object object : evaluatedResources) {
            Resource resource = (Resource) object;
            String resourceId = resource.getId();
            Reference reference = bc.createEvaluatedResourceReference(resourceId);
            if (id != null) {
                reference.addExtension(MeasureConstants.EXT_CRITERIA_REFERENCE_URL, new StringType(id));
            }
        }
    }

    protected void buildSDE(BuilderContext bc, MeasureSupplementalDataComponent msdc, SdeDef sde) {
        var report = bc.report();


        createSdeCriteriaReferenceExt(sde.id())

        // This processes the SDEs for a given report.
        // Case 1: individual - primitive types (ints, codes, etc)
        // convert to observation, add observation as contained, add sde reference with
        // criteria reference extension
        // Case 2: individual - resource types
        // add sde reference with criteria reference extension for each resource
        // if not an evaluated resource, add to contained
        // Case 3: population - primitive types, non aggregatable
        // convert to observation, add observation as contained, add sde reference with
        // criteria reference extension
        // Case 4: population - primitive type, aggregatable
        // aggregate by value, convert to observation, add observation as contained, add
        // sde reference with criteria reference extension
        // Case 5: population - resource types
        // add sde reference with criteria reference extension for each resource
        // if not an evaluated resource, add to contained

        CodeableConcept concept = conceptDefToConcept(sde.code());

        Map<ValueWrapper, Long> accumulated = sde.getResults().values().stream().map(x -> new ValueWrapper(x.value()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        processSdeEvaluatedResourceExt(bc, sde);

        for (Map.Entry<ValueWrapper, Long> accumulator : accumulated.entrySet()) {

            DomainResource obs = null;
            if (!(accumulator.getKey().getValue() instanceof DomainResource)) {
                String valueCode = accumulator.getKey().getValueAsString();
                String valueKey = accumulator.getKey().getKey();
                Long valueCount = accumulator.getValue();

                if (valueKey == null) {
                    valueKey = valueCode;
                }

                valueKey = this.escapeForFhirId(valueKey);

                Coding valueCoding = new Coding().setCode(valueCode);
                // if (!sdeCode.equalsIgnoreCase("sde-sex")) {
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
                // }

                switch (report.getType()) {
                    case INDIVIDUAL:
                        obs = createPatientObservation(bc, UUID.randomUUID().toString(), sde.id(), valueCoding,
                                concept);
                        break;
                    default:
                        obs = createPopulationObservation(bc, UUID.randomUUID().toString(), sde.id(), valueCoding,
                                valueCount, concept);
                        break;
                }
                report.addContained(obs);
                processCoreSdeEvaluatedResourceExt(bc, createSdeCriteriaReferenceExt(sde.id()),
                        "#" + obs.getId(), sde.id());
            }
        }

    }

    protected void buildSDEs(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        // ASSUMPTION: Measure SDEs are in the same order as MeasureDef SDEs
        for (int i = 0; i < measure.getSupplementalData().size(); i++) {
            var msdc = measure.getSupplementalData().get(i);
            var sde = measureDef.sdes().get(i);
            buildSDE(bc, msdc, sde);
        }
    }

    private CodeableConcept conceptDefToConcept(ConceptDef c) {
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(codeDefToCoding(cd));
        }

        return cc;
    }

    private Coding codeDefToCoding(CodeDef c) {
        var cd = new Coding();
        cd.setSystem(c.system());
        cd.setCode(c.code());
        cd.setVersion(c.version());
        cd.setDisplay(c.display());

        return cd;
    }

    private Map<String, HashSet<String>> extensionSet;

    private Map<String, HashSet<String>> getExtKeySetMap() {
        if (extensionSet == null) {
            extensionSet = new HashMap<>();
        }
        return extensionSet;
    }

    private void updateKeySetMap(Map<String, HashSet<String>> keySetMap, String key, String value) {
        if (!keySetMap.containsKey(key)) {
            HashSet<String> set = new HashSet<>();
            set.add(value);
            keySetMap.put(key, set);
        } else {
            keySetMap.get(key).add(value);
        }
    }

    private void processSdeEvaluatedResourceExt(BuilderContext bc, SdeDef sdeDef) {
        for (Object obj : sdeDef.getValues()) {
            if (obj instanceof IBaseResource) {
                IBaseResource res = (IBaseResource) obj;
                Extension sdeCriteriaReferenceExt = createSdeCriteriaReferenceExt(sdeDef.id());
                String referenceValue = createResourceReference(res.fhirType(), res.getIdElement().getIdPart());
                processCoreSdeEvaluatedResourceExt(bc, sdeCriteriaReferenceExt, referenceValue, sdeDef.id());
            }
        }
    }

    private void processCoreSdeEvaluatedResourceExt(BuilderContext bc, Extension criteriaRefExt, String referenceVal,
            String populationRef) {
        var report = bc.report();
        if (!getExtKeySetMap().containsKey(referenceVal)) {
            Extension ext = new Extension(MeasureConstants.EXT_SDE_URL);
            Reference ref = new Reference(referenceVal);
            ref.addExtension(criteriaRefExt);
            ext.setValue(ref);
            report.getExtension().add(ext);
            updateKeySetMap(getExtKeySetMap(), referenceVal, populationRef);
        } else if (!(getExtKeySetMap().get(referenceVal).contains(populationRef))) {
            updateExtInExistingList(report.getExtensionsByUrl(MeasureConstants.EXT_SDE_URL), criteriaRefExt,
                    referenceVal);
            updateKeySetMap(getExtKeySetMap(), referenceVal, populationRef);
        }
    }

    private Extension createSdeCriteriaReferenceExt(String value) {
        return new Extension(MeasureConstants.EXT_CRITERIA_REFERENCE_URL)
                .setValue(new StringType(value));
    }

    private String createResourceReference(String resourceType, String id) {
        return new StringBuilder(resourceType).append("/").append(id).toString();
    }

    private void updateExtInExistingList(List<Extension> list, Extension criteriaReferenceExtension, String value) {
        list.stream().filter(extension -> ((Reference) extension.getValue()).getReference().equals(value))
                .collect(Collectors.toList()).stream().findFirst().get().getValue()
                .addExtension(criteriaReferenceExtension);
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
            throw new IllegalArgumentException(
                    "Measurement period should be an interval of CQL DateTime or Date");
        }
    }

    protected MeasureReport createMeasureReport(Measure measure, MeasureReportType type, List<String> subjectIds,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));

        if (type == MeasureReportType.INDIVIDUAL && !subjectIds.isEmpty()) {
            report.setSubject(new Reference(subjectIds.get(0)));
        }

        report.setPeriod(getPeriod(measurementPeriod));
        report.setMeasure(measure.getUrl());
        report.setDate(new java.util.Date());
        report.setImplicitRules(measure.getImplicitRules());
        report.setImprovementNotation(measure.getImprovementNotation());
        report.setLanguage(measure.getLanguage());

        if (measure.hasDescription()) {
            report.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measure.getDescription()));
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

        Extension extExtMeasure = new Extension().setUrl(MeasureInfo.MEASURE)
                .setValue(new CanonicalType(measureInfo.getMeasure()));

        Extension extExtPop = new Extension().setUrl(MeasureInfo.POPULATION_ID)
                .setValue(new StringType(measureInfo.getPopulationId()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);

        return obsExtension;
    }

    protected boolean ifIsBooleanBasis(Measure measure) {
        if (measure.hasExtension()) {
            return measure.getExtension().stream().anyMatch(this::isBooleanBasisExtension);
        }
        return false;
    }

    private boolean isBooleanBasisExtension(Extension item) {
        return (item.getUrl() != null &&
                StringUtils.equalsIgnoreCase(item.getUrl(), MeasureConstants.POPULATION_BASIS_URL) &&
                StringUtils.equalsIgnoreCase(item.getValue().toString(), "boolean"));
    }

    private Coding supplementalDataCoding;

    private Coding geSupplementalDataCoding() {
        if (supplementalDataCoding == null) {
            supplementalDataCoding = new Coding().setCode("supplemental-data")
                    .setSystem("http://terminology.hl7.org/CodeSystem/measure-data-usage");
        }
        return supplementalDataCoding;
    }

    private CodeableConcept getMeasureUsageConcept(CodeableConcept originalConcept) {
        CodeableConcept measureUsageConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(geSupplementalDataCoding());
        measureUsageConcept.setCoding(list);

        if (originalConcept != null) {
            if (originalConcept.hasText() && StringUtils.isNotBlank(originalConcept.getText())) {
                measureUsageConcept.setText(originalConcept.getText());
            }
            if (originalConcept.hasCoding()) {
                measureUsageConcept.getCoding().add(originalConcept.getCodingFirstRep());
            }
        }
        return measureUsageConcept;
    }

    protected DomainResource createPopulationObservation(BuilderContext bc, String id, String populationId,
            Coding valueCoding,
            Long sdeAccumulatorValue, CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(valueCoding);
        if (originalConcept != null && originalConcept.hasCoding()) {
            list.add(originalConcept.getCodingFirstRep());
        }
        obsCodeableConcept.setCoding(list);

        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(sdeAccumulatorValue));

        return obs;
    }

    protected DomainResource createPatientObservation(BuilderContext bc, String id, String populationId,
            Coding valueCoding,
            CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        obs.setCode(getMeasureUsageConcept(originalConcept));

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    protected Observation createObservation(BuilderContext bc, String id, String populationId) {
        var measure = bc.measure();
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(measure.hasUrl() ? measure.getUrl()
                        : (measure.hasId() ? MeasureInfo.MEASURE_PREFIX + measure.getIdElement().getIdPart() : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    protected Observation createMeasureObservation(BuilderContext bc, String id, String observationName) {
        Observation obs = this.createObservation(bc, id, observationName);
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
            if (this == o)
                return true;
            if (o == null)
                return false;
            if (this.getClass() != o.getClass())
                return false;

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
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                // ASSUMPTION: We won't have different systems with the same code
                // within a given stratifier / sde
                return joinValues("coding", c.getCode());
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return joinValues("codeable-concept", c.getCodingFirstRep().getCode());
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return joinValues("code", c.getCode());
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return joinValues("enum", e.toString());
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return joinValues("primitive", p.getValueAsString());
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return null;
            }
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
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return null;
            }
        }

        public String getDescription() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.hasDisplay() ? c.getDisplay() : c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().hasDisplay() ? c.getCodingFirstRep().getDisplay()
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
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
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
