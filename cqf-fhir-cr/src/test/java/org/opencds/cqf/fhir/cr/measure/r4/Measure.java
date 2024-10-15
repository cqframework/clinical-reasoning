package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.common.MeasureInfo.EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedGroup.SelectedReference;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.r4.ContainedHelper;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class Measure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @FunctionalInterface
    interface Validator<T> {
        void validate(T value);
    }

    @FunctionalInterface
    interface Selector<T, S> {
        T select(S from);
    }

    interface ChildOf<T> {
        T up();
    }

    interface SelectedOf<T> {
        T value();
    }

    protected static class Selected<T, P> implements SelectedOf<T>, ChildOf<P> {
        private final P parent;
        private final T value;

        public Selected(T value, P parent) {
            this.parent = parent;
            this.value = value;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public P up() {
            return parent;
        }
    }

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private Repository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private MeasurePeriodValidator measurePeriodValidator;

        public Given() {
            this.evaluationOptions = MeasureEvaluationOptions.defaultOptions();
            this.evaluationOptions
                    .getEvaluationSettings()
                    .getRetrieveSettings()
                    .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                    .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

            this.evaluationOptions
                    .getEvaluationSettings()
                    .getTerminologySettings()
                    .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

            this.measurePeriodValidator = new MeasurePeriodValidator();
        }

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));

            return this;
        }

        public Given evaluationOptions(MeasureEvaluationOptions evaluationOptions) {
            this.evaluationOptions = evaluationOptions;
            return this;
        }

        private R4MeasureService buildMeasureService() {
            return new R4MeasureService(repository, evaluationOptions, measurePeriodValidator);
        }

        public When when() {
            return new When(buildMeasureService());
        }
    }

    public static class When {
        // private final R4MeasureProcessor processor;
        private final R4MeasureService service;

        When(R4MeasureService service) {
            this.service = service;
        }

        private String measureId;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private String reportType;
        private Bundle additionalData;
        private Parameters parameters;

        private Supplier<MeasureReport> operation;
        private String practitioner;
        private String productLine;

        public When measureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        public When periodEnd(String periodEnd) {
            this.periodEnd =
                    LocalDate.parse(periodEnd, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        public When periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public When periodStart(String periodStart) {
            this.periodStart = LocalDate.parse(periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        public When periodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public When subject(String subjectId) {
            this.subject = subjectId;
            return this;
        }

        public When reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        public When additionalData(Bundle additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public When parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public When practitioner(String practitioner) {
            this.practitioner = practitioner;
            return this;
        }

        public When productLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        public When evaluate() {
            this.operation = () -> service.evaluate(
                    Eithers.forMiddle3(new IdType("Measure", measureId)),
                    periodStart,
                    periodEnd,
                    reportType,
                    subject,
                    null,
                    null,
                    null,
                    null,
                    additionalData,
                    parameters,
                    productLine,
                    practitioner);
            return this;
        }

        public SelectedReport then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new SelectedReport(this.operation.get());
        }
    }

    public static class SelectedReport extends Selected<MeasureReport, Void> {
        public SelectedReport(MeasureReport report) {
            super(report, null);
        }

        public SelectedReport passes(Validator<MeasureReport> measureReportValidator) {
            measureReportValidator.validate(value());
            return this;
        }

        public MeasureReport report() {
            return this.value();
        }

        public SelectedGroup firstGroup() {
            return this.group(MeasureReport::getGroupFirstRep);
        }

        public SelectedGroup group(String id) {
            return this.group(x -> x.getGroup().stream()
                    .filter(g -> g.getId().equals(id))
                    .findFirst()
                    .get());
        }

        public SelectedGroup group(Selector<MeasureReportGroupComponent, MeasureReport> groupSelector) {
            var g = groupSelector.select(value());
            return new SelectedGroup(g, this);
        }

        public SelectedReference<SelectedReport> reference(Selector<Reference, MeasureReport> referenceSelector) {
            var r = referenceSelector.select(value());
            return new SelectedReference<>(r, this);
        }

        public SelectedReference<SelectedReport> evaluatedResource(String name) {
            return this.reference(x -> x.getEvaluatedResource().stream()
                    .filter(y -> y.getReference().equals(name))
                    .findFirst()
                    .get());
        }

        public SelectedReport hasEvaluatedResourceCount(int count) {
            assertEquals(count, report().getEvaluatedResource().size());
            return this;
        }

        public SelectedReport hasMeasureVersion(String version) {
            assertEquals(
                    version,
                    report().getMeasure().substring(report().getMeasure().indexOf('|') + 1));
            return this;
        }

        public SelectedReport hasContainedResourceCount(int count) {
            assertEquals(count, report().getContained().size());
            return this;
        }

        // TODO: SelectedContained resource class?
        public SelectedReport hasContainedResource(Predicate<Resource> criteria) {
            var contained = this.report().getContained().stream();
            assertTrue(contained.anyMatch(criteria), "Did not find a resource matching this criteria ");
            return this;
        }

        // TODO: SelectedExtension class?
        public SelectedReport hasExtension(String url, int count) {
            var ex = this.value().getExtensionsByUrl(url);
            assertEquals(ex.size(), count);

            return this;
        }

        public SelectedReport hasNoReportLevelImprovementNotation() {
            assertFalse(this.value().hasImprovementNotation());

            return this;
        }

        public SelectedReport hasReportLevelImprovementNotation() {
            assertTrue(this.value().hasImprovementNotation());

            return this;
        }

        public SelectedReport hasSubjectReference(String subjectReference) {
            var ref = this.report().getSubject();
            assertEquals(ref.getReference(), subjectReference);
            return this;
        }

        public SelectedReport hasReportType(String reportType) {
            var ref = this.report().getType();
            assertEquals(reportType, ref.getDisplay());
            return this;
        }

        public SelectedReport hasPeriodStart(Date periodStart) {
            var period = this.report().getPeriod();
            assertEquals(
                    periodStart,
                    period.getStart(),
                    String.format(
                            "Expected period start of %s but was: %s",
                            DATE_FORMAT.format(periodStart), DATE_FORMAT.format(period.getStart())));
            return this;
        }

        public SelectedReport hasPeriodEnd(Date periodEnd) {
            var period = this.report().getPeriod();
            assertEquals(
                    periodEnd,
                    period.getEnd(),
                    String.format(
                            "Expected period start of %s but was: %s",
                            DATE_FORMAT.format(periodEnd), DATE_FORMAT.format(period.getEnd())));
            return this;
        }

        public SelectedContained contained(Selector<Resource, MeasureReport> containedSelector) {
            var c = containedSelector.select(value());
            return new SelectedContained(c, this);
        }

        public SelectedContained contained(ResourceType theResourceType, String theId) {
            /*
             * SelectedContained will only be useful for Observation resources
             * Explanation: Contained resourceIds are randomly generated at runtime and are not searchable or known before testing.
             * List resources can only be verified from population reference id, which is generated at runtime.
             * There are no other unique references on List to Measure Report population, so it is not reverse searchable.
             * Observation resources will leverage sde-code reference to isolate contained resources.
             */
            return this.contained(t -> getContainedResources(t).stream()
                    .filter(x -> x.getResourceType().equals(theResourceType))
                    .filter(y -> y.getId().equals(theId))
                    .findFirst()
                    .orElseThrow());
        }

        private List<Resource> getContainedResources(MeasureReport theMeasureReport) {
            return ContainedHelper.getAllContainedResources(theMeasureReport);
        }

        public SelectedReport containedObservationsHaveMatchingExtension() {
            List<String> contained = getContainedIdsPerResourceType(ResourceType.Observation);
            List<String> extIds = getExtensionIds();

            assertEquals(
                    contained.size(),
                    extIds.size(),
                    "Qty of SDE Observation resources don't match qty of Extension references");
            // contained Observations have a matching extension reference
            for (String s : contained) {
                assertTrue(extIds.stream().anyMatch(t -> t.replace("#", "").equals(s)));
            }
            // extension references have a matching contained Observation resource
            for (String extId : extIds) {
                // inline resource references concat prefix '#' to indicate they are not persisted
                assertTrue(contained.contains(extId.replace("#", "")));
            }
            return this;
        }

        public SelectedReport containedListHasCorrectResourceType(String theResourceType) {
            var resourceType = ContainedHelper.getAllContainedResources(value()).stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.List))
                    .map(x -> (ListResource) x)
                    .findFirst()
                    .orElseThrow()
                    .getEntryFirstRep()
                    .getItem()
                    .getReference();
            assertTrue(resourceType.contains(theResourceType));
            return this;
        }

        private List<String> getContainedIdsPerResourceType(ResourceType theResourceType) {
            List<String> containedIds = new ArrayList<>();
            List<Resource> resources = ContainedHelper.getAllContainedResources(value());
            for (Resource resource : resources) {
                if (resource.getResourceType().equals(theResourceType)) {
                    containedIds.add(resource.getId());
                }
            }
            return containedIds;
        }

        private List<String> getExtensionIds() {
            List<String> extReferences = new ArrayList<>();
            List<Extension> exts = value().getExtensionsByUrl(EXT_SDE_REFERENCE_URL);
            for (Extension ext : exts) {
                extReferences.add(ext.getValue().toString());
            }
            return extReferences;
        }

        public SelectedReport subjectResultsValidation() {
            List<String> contained = getContainedIdsPerResourceType(ResourceType.List);
            List<String> subjectRefs = subjectResultReferences();
            // where lists are contained resources
            if (!contained.isEmpty()) {
                // all contained List resources have a matching population referencing it
                for (String s : contained) {
                    assertTrue(subjectRefs.stream().anyMatch(t -> t.contains(s)));
                    // validate matching counts for resource and MeasureReport population count
                    var listEntryCount = getListEntrySize(value(), s);
                    var populationCount = getPopulationCount(value(), s);
                    assertEquals(populationCount, listEntryCount);
                }
                // all referenced resources are found in contained array
                for (String subjectRef : subjectRefs) {
                    // inline resource references concat prefix '#' to indicate they are not persisted
                    assertTrue(contained.contains(subjectRef.replace("#", "")));
                }
            }

            return this;
        }

        private int getPopulationCount(MeasureReport theMeasureReport, String theSubjectResultId) {
            return theMeasureReport.getGroup().stream()
                    .map(t -> t.getPopulation().stream()
                            .filter(MeasureReportGroupPopulationComponent::hasSubjectResults)
                            .filter(x -> x.getSubjectResults().getReference().contains(theSubjectResultId))
                            .findFirst()
                            .orElseThrow())
                    .findFirst()
                    .orElseThrow()
                    .getCount();
        }

        private int getListEntrySize(MeasureReport theMeasureReport, String theResourceId) {
            var entry = (ListResource) ContainedHelper.getAllContainedResources(theMeasureReport).stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.List))
                    .filter(x -> x.getId().equals(theResourceId))
                    .findAny()
                    .orElseThrow();
            return entry.getEntry().size();
        }

        private List<String> subjectResultReferences() {
            List<String> refs = new ArrayList<>();
            // loop through all groups and populations
            var groupPops = value().getGroup();
            for (MeasureReportGroupComponent groupPop : groupPops) {
                var pops = groupPop.getPopulation();
                for (MeasureReportGroupPopulationComponent pop : pops) {
                    if (pop.getSubjectResults().hasReference()) {
                        refs.add(pop.getSubjectResults().getReference());
                    }
                }
            }
            return refs;
        }
    }

    static class SelectedContained extends Selected<Resource, SelectedReport> {

        public SelectedContained(Resource value, SelectedReport parent) {
            super(value, parent);
        }

        public SelectedContained observationHasExtensionUrl() {
            var obs = (Observation) value();
            assertEquals(EXT_URL, obs.getExtension().get(0).getUrl());
            return this;
        }

        public SelectedContained observationHasCode(String theCode) {
            var obs = (Observation) value();
            assertEquals(theCode, obs.getCode().getCoding().get(0).getCode());
            return this;
        }

        public SelectedContained observationCount(int theCount) {
            var obs = (Observation) value();
            assertEquals(theCount, obs.getValueIntegerType().getValue());
            return this;
        }
    }

    static class SelectedGroup extends Selected<MeasureReport.MeasureReportGroupComponent, SelectedReport> {

        public SelectedGroup(MeasureReportGroupComponent value, SelectedReport parent) {
            super(value, parent);
        }

        public SelectedGroup hasScore(String score) {
            MeasureValidationUtils.validateGroupScore(this.value(), score);
            return this;
        }

        public SelectedGroup hasImprovementNotationExt(String code) {
            var improvementNotationExt = value().getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
            assertNotNull(improvementNotationExt);
            var codeConcept = (CodeableConcept) improvementNotationExt.getValue();
            assertTrue(codeConcept.hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, code));

            return this;
        }

        public SelectedGroup hasDateOfCompliance() {
            assertEquals(
                    CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL,
                    this.value()
                            .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                            .get(0)
                            .getUrl());
            assertFalse(this.value()
                    .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                    .get(0)
                    .getValue()
                    .isEmpty());
            assertTrue(
                    this.value()
                                    .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                                    .get(0)
                                    .getValue()
                            instanceof Period);
            return this;
        }

        public SelectedPopulation population(String name) {
            return this.population(g -> g.getPopulation().stream()
                    .filter(x -> x.hasCode()
                            && x.getCode().hasCoding()
                            && x.getCode().getCoding().get(0).getCode().equals(name))
                    .findFirst()
                    .get());
        }

        public SelectedPopulation population(
                Selector<MeasureReportGroupPopulationComponent, MeasureReportGroupComponent> populationSelector) {
            var p = populationSelector.select(value());
            return new SelectedPopulation(p, this);
        }

        public SelectedPopulation firstPopulation() {
            return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
        }

        public SelectedGroup hasStratifierCount(int count) {
            assertEquals(count, this.value().getStratifier().size());
            return this;
        }

        public SelectedStratifier firstStratifier() {
            return this.stratifier(MeasureReport.MeasureReportGroupComponent::getStratifierFirstRep);
        }

        public SelectedStratifier stratifier(
                Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent> stratifierSelector) {
            var s = stratifierSelector.select(value());
            return new SelectedStratifier(s, this);
        }

        static class SelectedReference<P> extends Selected<Reference, P> {

            public SelectedReference(Reference value, P parent) {
                super(value, parent);
            }

            // Hmm.. may need to rethink this one a bit.
            public SelectedReference<P> hasPopulations(String... population) {
                var ex = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
                if (ex.isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "no evaluated resource extensions were found, and expected %s", population.length));
                }

                @SuppressWarnings("unchecked")
                var set = ex.stream()
                        .map(x -> ((IPrimitiveType<String>) x.getValue()).getValue())
                        .collect(Collectors.toSet());

                for (var p : population) {
                    assertTrue(
                            set.contains(p),
                            String.format(
                                    "population: %s was not found in the evaluated resources criteria reference extension list",
                                    p));
                }

                return this;
            }
        }

        static class SelectedPopulation
                extends Selected<MeasureReport.MeasureReportGroupPopulationComponent, SelectedGroup> {

            public SelectedPopulation(MeasureReportGroupPopulationComponent value, SelectedGroup parent) {
                super(value, parent);
            }

            public SelectedPopulation hasCount(int count) {
                MeasureValidationUtils.validatePopulation(value(), count);
                return this;
            }

            public SelectedPopulation hasSubjectResults() {
                assertNotNull(value().getSubjectResults().getReference());
                return this;
            }

            public SelectedPopulation passes(
                    Validator<MeasureReport.MeasureReportGroupPopulationComponent> populationValidator) {
                populationValidator.validate(value());
                return this;
            }
        }
    }

    static class SelectedStratifier
            extends Selected<MeasureReport.MeasureReportGroupStratifierComponent, SelectedGroup> {

        public SelectedStratifier(MeasureReportGroupStratifierComponent value, SelectedGroup parent) {
            super(value, parent);
        }

        public SelectedStratum firstStratum() {
            return stratum(MeasureReport.MeasureReportGroupStratifierComponent::getStratumFirstRep);
        }

        public SelectedStratum stratum(CodeableConcept value) {
            return stratum(s -> s.getStratum().stream()
                    .filter(x -> x.hasValue() && x.getValue().equalsDeep(value))
                    .findFirst()
                    .get());
        }

        public SelectedStratum stratum(String textValue) {
            return stratum(s -> s.getStratum().stream()
                    .filter(x -> x.hasValue() && x.getValue().hasText())
                    .filter(x -> x.getValue().getText().equals(textValue))
                    .findFirst()
                    .get());
        }

        public SelectedStratum stratum(
                Selector<MeasureReport.StratifierGroupComponent, MeasureReport.MeasureReportGroupStratifierComponent>
                        stratumSelector) {
            var s = stratumSelector.select(value());
            return new SelectedStratum(s, this);
        }
    }

    static class SelectedStratum extends Selected<MeasureReport.StratifierGroupComponent, SelectedStratifier> {

        public SelectedStratum(MeasureReport.StratifierGroupComponent value, SelectedStratifier parent) {
            super(value, parent);
        }

        public SelectedStratum hasScore(String score) {
            MeasureValidationUtils.validateStratumScore(value(), score);
            return this;
        }

        public SelectedStratumPopulation firstPopulation() {
            return population(MeasureReport.StratifierGroupComponent::getPopulationFirstRep);
        }

        public SelectedStratumPopulation population(String name) {
            return population(s -> s.getPopulation().stream()
                    .filter(x -> x.hasCode()
                            && x.getCode().hasCoding()
                            && x.getCode().getCoding().get(0).getCode().equals(name))
                    .findFirst()
                    .get());
        }

        public SelectedStratumPopulation population(
                Selector<MeasureReport.StratifierGroupPopulationComponent, MeasureReport.StratifierGroupComponent>
                        populationSelector) {
            var p = populationSelector.select(value());
            return new SelectedStratumPopulation(p, this);
        }
    }

    static class SelectedStratumPopulation
            extends Selected<MeasureReport.StratifierGroupPopulationComponent, SelectedStratum> {

        public SelectedStratumPopulation(
                MeasureReport.StratifierGroupPopulationComponent value, SelectedStratum parent) {
            super(value, parent);
        }

        public SelectedStratumPopulation hasCount(int count) {
            assertEquals(count, this.value().getCount());
            return this;
        }
    }
}
