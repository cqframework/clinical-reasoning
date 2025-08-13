package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.common.MeasureInfo.EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_DAVINCI_DEQM_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_REFERENCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_SYSTEM_URL;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedGroup.SelectedReference;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.r4.ContainedHelper;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings({"squid:S2699", "squid:S5960", "squid:S1135"})
public class Measure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FunctionalInterface
    interface Validator<T> {
        void validate(T value);
    }

    @FunctionalInterface
    public interface Selector<T, S> {
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

    public static Given given(@Nullable Boolean applyScoringSetMembership) {
        return new Given(applyScoringSetMembership);
    }

    public static Given given() {
        return new Given(true);
    }

    public static class Given {
        private IRepository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private final MeasurePeriodValidator measurePeriodValidator;
        private final R4MeasureServiceUtils measureServiceUtils;

        public Given(@Nullable Boolean applyScoringSetMembership) {
            this.evaluationOptions = MeasureEvaluationOptions.defaultOptions();
            if (applyScoringSetMembership != null && !applyScoringSetMembership) {
                MeasureEvaluationOptions options = MeasureEvaluationOptions.defaultOptions();
                options.setApplyScoringSetMembership(false);
                this.evaluationOptions = options;
            } else {
                this.evaluationOptions = MeasureEvaluationOptions.defaultOptions();
            }

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

            this.measureServiceUtils = new R4MeasureServiceUtils(repository);
        }

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));

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

        public SelectedReference reference(Selector<Reference, MeasureReport> referenceSelector) {
            var r = referenceSelector.select(value());
            return new SelectedReference(r, this);
        }

        public SelectedReference evaluatedResource(String name) {
            return this.reference(x -> x.getEvaluatedResource().stream()
                    .filter(y -> y.getReference().equals(name))
                    .findFirst()
                    .get());
        }

        public SelectedReport hasEvaluatedResourceCount(int count) {
            assertEquals(count, report().getEvaluatedResource().size());
            return this;
        }

        public SelectedReport evaluatedResourceHasNoDuplicateReferences() {
            var rawRefs = report().getEvaluatedResource().size();
            var distinctRefs =
                    (int) report().getEvaluatedResource().stream().distinct().count();
            assertEquals(rawRefs, distinctRefs, "duplicate reference found in evaluatedResources");
            return this;
        }

        public SelectedReport hasMeasureUrl(String url) {
            assertEquals(url, report().getMeasure());
            return this;
        }

        public SelectedReport hasMeasureReportDate() {
            assertNotNull(report().getDate());
            return this;
        }

        public SelectedReport hasStatus(MeasureReportStatus status) {
            assertEquals(status, report().getStatus());
            return this;
        }

        public SelectedReport hasEmptySubject() {
            assertNull(report().getSubject().getReference());
            return this;
        }

        public SelectedReport hasMeasureReportPeriod() {
            assertNotNull(report().getPeriod());
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

        public SelectedReport hasContainedResource(Predicate<Resource> criteria) {
            var contained = this.report().getContained().stream();
            assertTrue(contained.anyMatch(criteria), "Did not find a resource matching this criteria ");
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

        public SelectedReport improvementNotationCode(String code) {
            assertEquals(
                    code,
                    this.value().getImprovementNotation().getCodingFirstRep().getCode());

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
                    "Expected period start of %s but was: %s"
                            .formatted(formatDate(periodStart), formatDate(period.getStart())));
            return this;
        }

        public SelectedReport hasPeriodEnd(Date periodEnd) {
            var period = this.report().getPeriod();
            assertEquals(
                    periodEnd,
                    period.getEnd(),
                    "Expected period start of %s but was: %s"
                            .formatted(formatDate(periodEnd), formatDate(period.getEnd())));
            return this;
        }

        public SelectedExtension extension(Selector<Extension, MeasureReport> extensionSelector) {
            var e = extensionSelector.select(value());
            return new SelectedExtension(e, this);
        }

        public SelectedExtension extension(String supplementalDataId) {
            return this.extension(t -> getExtensions(t).stream()
                    .filter(x -> x.getValue()
                            .getExtensionByUrl(SDE_DAVINCI_DEQM_EXT_URL)
                            .getValue()
                            .toString()
                            .equals(supplementalDataId))
                    .findFirst()
                    .orElseThrow());
        }

        /**
         * for looking up resource reference values for List SDE
         * @param resourceReference example Encounter/{id}
         * @return SelectedExtension
         */
        public SelectedExtension extensionByValueReference(String resourceReference) {
            return this.extension(t -> getExtensions(t).stream()
                    .filter(y -> ((Reference) y.getValue()).getReference().equals(resourceReference))
                    .findFirst()
                    .orElseThrow());
        }

        public List<Extension> getExtensions(MeasureReport measureReport) {
            return measureReport.getExtension();
        }

        public SelectedReport hasExtension(String url, int count) {
            var ex = this.value().getExtensionsByUrl(url);
            assertEquals(ex.size(), count);

            return this;
        }

        public SelectedContained contained(Selector<Resource, MeasureReport> containedSelector) {
            var c = containedSelector.select(value());
            return new SelectedContained(c, this);
        }

        public SelectedContained containedByValue(String codeValue) {
            /*
             * SelectedContained will only be useful for Observation resources
             * Explanation: This will retrieve the CodeableConcept value for individual Measure Reports
             * Example: "M" for 'Male' gender code
             */
            return this.contained(t -> getContainedResources(t).stream()
                    .filter(Observation.class::isInstance)
                    .filter(y -> ((Observation) y)
                            .getValueCodeableConcept()
                            .getCodingFirstRep()
                            .getCode()
                            .equals(codeValue))
                    .findFirst()
                    .orElseThrow());
        }

        public SelectedContained containedByCoding(String codeCoding) {
            /*
             * SelectedContained will only be useful for Observation resources
             * Explanation: This will retrieve the Observation.Coding.code for Summary Measure Reports, as value then becomes a count
             * Example: "M" for 'Male' gender code
             */
            return this.contained(t -> getContainedResources(t).stream()
                    .filter(Observation.class::isInstance)
                    .filter(y -> ((Observation) y)
                            .getCode()
                            .getCodingFirstRep()
                            .getCode()
                            .equals(codeCoding))
                    .findFirst()
                    .orElseThrow());
        }

        private List<Resource> getContainedResources(MeasureReport measureReport) {
            return ContainedHelper.getAllContainedResources(measureReport);
        }

        public SelectedReport containedObservationsHaveMatchingExtension() {
            Set<String> contained = value().getContained().stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.Observation))
                    .map(Resource::getIdPart)
                    .collect(Collectors.toSet());

            Set<String> extIds = value().getExtensionsByUrl(SDE_REFERENCE_EXT_URL).stream()
                    .map(x -> (Reference) x.getValue())
                    .map(t -> t.getReference().replace("#", ""))
                    .collect(Collectors.toSet());

            assertEquals(
                    contained.size(),
                    extIds.size(),
                    "Qty of SDE Observation resources don't match qty of SDE Extension references");
            // contained Observations have a matching extension reference
            Set<String> intersection = new HashSet<>(contained);
            intersection.retainAll(extIds);
            assertEquals(intersection.size(), contained.size());

            return this;
        }

        public SelectedReport containedListHasCorrectResourceType(String resourceType) {
            var resourceTypeToUse = ContainedHelper.getAllContainedResources(value()).stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.List))
                    .map(x -> (ListResource) x)
                    .findFirst()
                    .orElseThrow()
                    .getEntryFirstRep()
                    .getItem()
                    .getReference();
            assertTrue(resourceTypeToUse.contains(resourceType));
            return this;
        }

        private List<String> getContainedIdsPerResourceType(ResourceType resourceType) {
            List<String> containedIds = new ArrayList<>();
            List<Resource> resources = ContainedHelper.getAllContainedResources(value());
            for (Resource resource : resources) {
                if (resource.getResourceType().equals(resourceType)) {
                    containedIds.add(resource.getId());
                }
            }
            return containedIds;
        }

        /**
         * Subject-List will contain Lists of resource references to represent population. This test validates that correct resourceType is present.
         * TODO: if group specifies basis instead of Measure then this will need to be updated.
         * @param resourceType resource basis of population (encounter or patient)
         * @return value allowing chaining of more methods
         */
        public SelectedReport subjectResultsHaveResourceType(String resourceType) {
            var lists = value().getContained().stream()
                    .filter(ListResource.class::isInstance)
                    .map(x -> (ListResource) x)
                    .toList();
            for (ListResource list : lists) {
                // all contained lists have correct ResourceType
                var size = list.getEntry().size();
                var matchSize = (int) list.getEntry().stream()
                        .filter(x -> x.getItem().getReference().startsWith(resourceType))
                        .count();
                assertEquals(size, matchSize, "SubjectResult List does not have correct ResourceType");
            }
            return this;
        }

        /**
         * This method is a top level validation that all subjectResult lists accurately represent population counts
         *
         * This gets all contained Lists and checks for a matching reference on a report population
         * It then checks that each population.count matches the size of the List (ex population.count=10, subjectResult list has 10 items)
         * @return
         */
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

        private int getPopulationCount(MeasureReport measureReport, String subjectResultId) {
            // find population with reference to contained List resource
            var groups = measureReport.getGroup();
            for (MeasureReportGroupComponent group : groups) {
                final Integer stratumPopulation = getPopulationCount(subjectResultId, group);
                if (stratumPopulation != null) {
                    return stratumPopulation;
                }
            }
            // if reached then no match found
            return 0;
        }

        @Nullable
        private Integer getPopulationCount(String subjectResultId, MeasureReportGroupComponent group) {
            var population = group.getPopulation().stream()
                    .filter(MeasureReportGroupPopulationComponent::hasSubjectResults)
                    .filter(x -> x.getSubjectResults().getReference().contains(subjectResultId))
                    .findFirst()
                    .orElse(null);
            if (population == null && group.getStratifier() != null) {
                final Integer stratumPopulation = getStratumCount(subjectResultId, group);
                if (stratumPopulation != null) {
                    return stratumPopulation;
                }
            } else if (population != null) {
                return population.getCount();
            }
            return null;
        }

        @Nullable
        private Integer getStratumCount(String subjectResultId, MeasureReportGroupComponent group) {
            var stratifiers = group.getStratifier();
            for (MeasureReportGroupStratifierComponent strat : stratifiers) {
                var stratifierGroups = strat.getStratum();
                for (StratifierGroupComponent stratGroup : stratifierGroups) {
                    var stratumPops = stratGroup.getPopulation();
                    for (StratifierGroupPopulationComponent stratumPopulation : stratumPops) {
                        // empty results could omit subjectResult reference
                        if (stratumPopulation.getSubjectResults().hasReference()
                                && stratumPopulation
                                        .getSubjectResults()
                                        .getReference()
                                        .contains(subjectResultId)) {
                            return stratumPopulation.getCount();
                        }
                    }
                }
            }
            return null;
        }

        private int getListEntrySize(MeasureReport measureReport, String resourceId) {
            var entry = (ListResource) ContainedHelper.getAllContainedResources(measureReport).stream()
                    .filter(t -> t.getResourceType().equals(ResourceType.List))
                    .filter(x -> x.getId().equals(resourceId))
                    .findAny()
                    .orElseThrow();
            return entry.getEntry().size();
        }

        private List<String> subjectResultReferences() {
            List<String> refs = new ArrayList<>();
            // loop through all groups and populations
            var groupPops = value().getGroup();
            for (MeasureReportGroupComponent groupPop : groupPops) {
                // standard population elements
                subjectResultReference(groupPop, refs);
            }
            return refs;
        }

        private void subjectResultReference(MeasureReportGroupComponent groupPop, List<String> refs) {
            var pops = groupPop.getPopulation();
            for (MeasureReportGroupPopulationComponent pop : pops) {
                if (pop.getSubjectResults().hasReference()) {
                    refs.add(pop.getSubjectResults().getReference());
                }
            }
            // stratifier results have references too
            if (groupPop.getStratifier() != null) {
                subjectResultReferenceStratifier(groupPop, refs);
            }
        }

        private void subjectResultReferenceStratifier(MeasureReportGroupComponent groupPop, List<String> refs) {
            var stratifiers = groupPop.getStratifier();
            for (MeasureReportGroupStratifierComponent strat : stratifiers) {
                var stratifierGroups = strat.getStratum();
                for (StratifierGroupComponent stratGroup : stratifierGroups) {
                    var stratumPops = stratGroup.getPopulation();
                    for (StratifierGroupPopulationComponent stratumPopulation : stratumPops) {
                        // empty results could omit subjectResult reference
                        if (stratumPopulation.getSubjectResults().hasReference()) {
                            refs.add(stratumPopulation.getSubjectResults().getReference());
                        }
                    }
                }
            }
        }

        public SelectedReport hasContainedOperationOutcome() {
            assertTrue(report().hasContained()
                    && report().getContained().stream()
                            .anyMatch(t -> t.getResourceType().equals(ResourceType.OperationOutcome)));
            return this;
        }

        public SelectedReport hasContainedOperationOutcomeMsg(String expectedMsg) {
            assertNotNull(expectedMsg);
            assertTrue(expectedMsg.length() > 1);

            final List<String> actualDiagnostics = report().getContained().stream()
                    .filter(OperationOutcome.class::isInstance)
                    .map(OperationOutcome.class::cast)
                    .map(OperationOutcome::getIssueFirstRep)
                    .map(OperationOutcomeIssueComponent::getDiagnostics)
                    .toList();

            assertTrue(
                    actualDiagnostics.stream().anyMatch(actualMsg -> actualMsg.contains(expectedMsg)),
                    "Expected: %n%s was not found in actual:%n%s".formatted(expectedMsg, actualDiagnostics));

            return this;
        }

        private static String formatDate(Date javaUtilDate) {
            return javaUtilDate
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(FORMATTER);
        }
    }

    static class SelectedExtension extends Selected<Extension, SelectedReport> {

        public SelectedExtension(Extension value, SelectedReport parent) {
            super(value, parent);
        }

        public SelectedExtension extensionHasSDEUrl() {
            assertEquals(SDE_REFERENCE_EXT_URL, value().getUrl());
            return this;
        }

        public SelectedExtension extensionHasSDEId(String id) {
            assertEquals(
                    id,
                    value().getValue()
                            .getExtensionByUrl(EXT_CRITERIA_REFERENCE_URL)
                            .getValue()
                            .toString());
            return this;
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

        /**
         * only applicable to individual reports
         * @return
         */
        public SelectedContained observationHasSDECoding() {
            assert value() instanceof Observation;
            var obs = (Observation) value();
            assertEquals(SDE_SYSTEM_URL, obs.getCode().getCodingFirstRep().getSystem());
            assertEquals("supplemental-data", obs.getCode().getCodingFirstRep().getCode());
            return this;
        }

        public SelectedContained observationHasCode(String code) {
            var obs = (Observation) value();
            assertEquals(code, obs.getCode().getCoding().get(0).getCode());
            return this;
        }

        public SelectedContained observationCount(int count) {
            var obs = (Observation) value();
            assertEquals(count, obs.getValueIntegerType().getValue());
            return this;
        }
    }

    public static class SelectedGroup extends Selected<MeasureReport.MeasureReportGroupComponent, SelectedReport> {

        public SelectedGroup(MeasureReportGroupComponent value, SelectedReport parent) {
            super(value, parent);
        }

        public SelectedGroup hasScore(String score) {
            MeasureValidationUtils.validateGroupScore(this.value(), score);
            return this;
        }

        public SelectedGroup hasMeasureScore(boolean hasScore) {
            assertEquals(hasScore, this.value().hasMeasureScore());
            return this;
        }

        public SelectedGroup hasImprovementNotationExt(String code) {
            var improvementNotationExt = value().getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
            assertNotNull(improvementNotationExt);
            var codeConcept = (CodeableConcept) improvementNotationExt.getValue();
            assertTrue(codeConcept.hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, code));

            return this;
        }

        public SelectedGroup hasNoImprovementNotationExt() {
            var improvementNotationExt = value().getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
            assertNull(improvementNotationExt);
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

        public SelectedStratifier stratifierById(String stratId) {
            return this.stratifier(g -> g.getStratifier().stream()
                    .filter(t -> t.getId().equals(stratId))
                    .findFirst()
                    .get());
        }

        public SelectedStratifier stratifier(
                Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent> stratifierSelector) {
            var s = stratifierSelector.select(value());
            return new SelectedStratifier(s, this);
        }

        static class SelectedReference extends Selected<Reference, SelectedReport> {

            public SelectedReference(Reference value, SelectedReport parent) {
                super(value, parent);
            }

            public SelectedReference hasNoDuplicateExtensions() {
                var exts = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
                var extsCount = exts.size();
                var distinctExtCount = (int) exts.stream()
                        .map(t -> ((StringType) t.getValue()).getValue())
                        .distinct()
                        .count();
                assertEquals(extsCount, distinctExtCount, "extension contain duplicate values");
                return this;
            }

            public SelectedReference referenceHasExtension(String extValueRef) {
                var ex = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
                if (ex.isEmpty()) {
                    throw new IllegalStateException(
                            "no evaluated resource extensions were found, and expected %s".formatted(extValueRef));
                }
                String foundRef = null;
                for (Extension extension : ex) {
                    assert extension.getValue() instanceof StringType;
                    StringType extValue = (StringType) extension.getValue();
                    if (extValue.getValue().equals(extValueRef)) {
                        foundRef = extValue.getValue();
                        break;
                    }
                }
                assertNotNull(foundRef);
                return this;
            }

            public SelectedReference hasEvaluatedResourceReferenceCount(int count) {
                assertEquals(count, this.value().getExtension().size());
                return this;
            }

            // Hmm.. may need to rethink this one a bit.
            public SelectedReference hasPopulations(String... population) {
                var ex = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
                if (ex.isEmpty()) {
                    throw new IllegalStateException("no evaluated resource extensions were found, and expected %s"
                            .formatted(population.length));
                }

                @SuppressWarnings("unchecked")
                var set = ex.stream()
                        .map(x -> ((IPrimitiveType<String>) x.getValue()).getValue())
                        .collect(Collectors.toSet());

                for (var p : population) {
                    assertTrue(
                            set.contains(p),
                            "population: %s was not found in the evaluated resources criteria reference extension list"
                                    .formatted(p));
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

        public SelectedStratifier stratumCount(int stratumCount) {
            assertEquals(stratumCount, value().getStratum().size());
            return this;
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
                    .orElse(null));
        }

        public SelectedStratum stratumByComponentValueText(String textValue) {
            return stratum(s -> s.getStratum().stream()
                    .filter(x -> x.getComponent().stream()
                            .anyMatch(t -> t.getValue().getText().equals(textValue)))
                    .findFirst()
                    .get());
        }

        public SelectedStratum stratumByComponentCodeText(String textValue) {
            return stratum(s -> s.getStratum().stream()
                    .filter(x -> x.getComponent().stream()
                            .anyMatch(t -> t.getCode().getText().equals(textValue)))
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

        public SelectedStratum hasComponentStratifierCount(int count) {
            assertEquals(count, value().getComponent().size());
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

        /**
         * if population has a count>0 and mode= subject-list, then population should have a subjectResult reference
         * @return assertNotNull
         */
        public SelectedStratumPopulation hasStratumPopulationSubjectResults() {
            assertNotNull(value().getSubjectResults().getReference());
            return this;
        }
        /**
         * if population has a count=0 and mode= subject-list, then population should NOT have a subjectResult reference
         * @return assertNull
         */
        public SelectedStratumPopulation hasNoStratumPopulationSubjectResults() {
            assertNull(value().getSubjectResults().getReference());
            return this;
        }
    }
}
