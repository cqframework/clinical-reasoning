package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_DAVINCI_DEQM_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_REFERENCE_EXT_URL;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Extension;
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
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selector;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Then;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Validator;
import org.opencds.cqf.fhir.utility.r4.ContainedHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectedMeasureReport extends Selected<MeasureReport, Then> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Logger logger = LoggerFactory.getLogger(SelectedMeasureReport.class);

    private final IRepository repository;
    private final FhirContext fhirContext;
    private final IParser jsonParser;

    public SelectedMeasureReport(MeasureReport report, Then parent, IRepository repository) {
        super(report, parent);
        this.repository = repository;
        this.fhirContext = repository.fhirContext();
        this.jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    }

    // Legacy constructor for backward compatibility (when parent is null/Void)
    @Deprecated
    public SelectedMeasureReport(MeasureReport report) {
        this(report, null, null);
    }

    public SelectedMeasureReport passes(Validator<MeasureReport> measureReportValidator) {
        measureReportValidator.validate(value());
        return this;
    }

    public MeasureReport report() {
        return this.value();
    }

    public SelectedMeasureReport hasGroupCount(int count) {
        assertEquals(count, report().getGroup().size());
        return this;
    }

    public SelectedMeasureReportGroup firstGroup() {
        return this.group(MeasureReport::getGroupFirstRep);
    }

    public SelectedMeasureReportGroup group(String id) {
        return this.group(x -> x.getGroup().stream()
                .filter(g -> g.getId().equals(id))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReportGroup group(int index) {
        return new SelectedMeasureReportGroup(value().getGroup().get(index), this);
    }

    public SelectedMeasureReportGroup group(Selector<MeasureReportGroupComponent, MeasureReport> groupSelector) {
        var g = groupSelector.select(value());
        return new SelectedMeasureReportGroup(g, this);
    }

    public SelectedMeasureReportReference reference(Selector<Reference, MeasureReport> referenceSelector) {
        var r = referenceSelector.select(value());
        return new SelectedMeasureReportReference(r, this);
    }

    public SelectedMeasureReportReference evaluatedResource(String name) {
        return this.reference(x -> x.getEvaluatedResource().stream()
                .filter(y -> y.getReference().equals(name))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReport hasEvaluatedResourceCount(int count) {
        assertEquals(count, report().getEvaluatedResource().size());
        return this;
    }

    public SelectedMeasureReport evaluatedResourceHasNoDuplicateReferences() {
        var rawRefs = report().getEvaluatedResource().size();
        var distinctRefs =
                (int) report().getEvaluatedResource().stream().distinct().count();
        assertEquals(rawRefs, distinctRefs, "duplicate reference found in evaluatedResources");
        return this;
    }

    public SelectedMeasureReport hasMeasureUrl(String url) {
        assertEquals(url, report().getMeasure());
        return this;
    }

    public SelectedMeasureReport hasMeasureReportDate() {
        assertNotNull(report().getDate());
        return this;
    }

    public SelectedMeasureReport hasStatus(MeasureReportStatus status) {
        assertEquals(status, report().getStatus());
        return this;
    }

    public SelectedMeasureReport hasEmptySubject() {
        assertNull(report().getSubject().getReference());
        return this;
    }

    public SelectedMeasureReport hasMeasureReportPeriod() {
        assertNotNull(report().getPeriod());
        return this;
    }

    public SelectedMeasureReport hasMeasureVersion(String version) {
        assertEquals(
                version, report().getMeasure().substring(report().getMeasure().indexOf('|') + 1));
        return this;
    }

    public SelectedMeasureReport hasContainedResourceCount(int count) {
        assertEquals(count, report().getContained().size());
        return this;
    }

    public SelectedMeasureReport hasContainedResource(Predicate<Resource> criteria) {
        var contained = this.report().getContained().stream();
        assertTrue(contained.anyMatch(criteria), "Did not find a resource matching this criteria ");
        return this;
    }

    public SelectedMeasureReport hasNoReportLevelImprovementNotation() {
        assertFalse(this.value().hasImprovementNotation());

        return this;
    }

    public SelectedMeasureReport hasReportLevelImprovementNotation() {
        assertTrue(this.value().hasImprovementNotation());

        return this;
    }

    public SelectedMeasureReport improvementNotationCode(String code) {
        assertEquals(
                code, this.value().getImprovementNotation().getCodingFirstRep().getCode());

        return this;
    }

    public SelectedMeasureReport hasSubjectReference(String subjectReference) {
        var ref = this.report().getSubject();
        assertEquals(ref.getReference(), subjectReference);
        return this;
    }

    public SelectedMeasureReport hasPatientReference(String patientReference) {
        var ref = this.report().getSubject();
        assertEquals(ref.getReference(), patientReference);
        return this;
    }

    public SelectedMeasureReport hasImprovementNotation(String code) {
        assertEquals(
                code, this.value().getImprovementNotation().getCodingFirstRep().getCode());
        return this;
    }

    public SelectedMeasureReport hasReportType(String reportType) {
        var ref = this.report().getType();
        assertEquals(reportType, ref.getDisplay());
        return this;
    }

    public SelectedMeasureReport hasPeriodStart(Date periodStart) {
        var period = this.report().getPeriod();
        assertEquals(
                periodStart,
                period.getStart(),
                "Expected period start of %s but was: %s"
                        .formatted(formatDate(periodStart), formatDate(period.getStart())));
        return this;
    }

    public SelectedMeasureReport hasPeriodEnd(Date periodEnd) {
        var period = this.report().getPeriod();
        assertEquals(
                periodEnd,
                period.getEnd(),
                "Expected period start of %s but was: %s"
                        .formatted(formatDate(periodEnd), formatDate(period.getEnd())));
        return this;
    }

    public SelectedMeasureReportExtension extension(Selector<Extension, MeasureReport> extensionSelector) {
        var e = extensionSelector.select(value());
        return new SelectedMeasureReportExtension(e, this);
    }

    public SelectedMeasureReportExtension extension(String supplementalDataId) {
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
     * @return SelectedMeasureReportExtension
     */
    public SelectedMeasureReportExtension extensionByValueReference(String resourceReference) {
        return this.extension(t -> getExtensions(t).stream()
                .filter(y -> ((Reference) y.getValue()).getReference().equals(resourceReference))
                .findFirst()
                .orElseThrow());
    }

    public List<Extension> getExtensions(MeasureReport measureReport) {
        return measureReport.getExtension();
    }

    public SelectedMeasureReport hasExtension(String url, int count) {
        var ex = this.value().getExtensionsByUrl(url);
        assertEquals(ex.size(), count);

        return this;
    }

    public SelectedMeasureReportContained contained(Selector<Resource, MeasureReport> containedSelector) {
        var c = containedSelector.select(value());
        return new SelectedMeasureReportContained(c, this);
    }

    public SelectedMeasureReportContained containedByValue(String codeValue) {
        /*
         * SelectedMeasureReportContained will only be useful for Observation resources
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

    public SelectedMeasureReportContained containedByCoding(String codeCoding) {
        /*
         * SelectedMeasureReportContained will only be useful for Observation resources
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

    public SelectedMeasureReport containedObservationsHaveMatchingExtension() {
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

    public SelectedMeasureReport containedListHasCorrectResourceType(String resourceType) {
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
    public SelectedMeasureReport subjectResultsHaveResourceType(String resourceType) {
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
     * <p/>
     * This gets all contained Lists and checks for a matching reference on a report population
     * It then checks that each population.count matches the size of the List (ex population.count=10, subjectResult list has 10 items)
     * @return report containing more chained methods
     */
    public SelectedMeasureReport subjectResultsValidation() {
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

    public SelectedMeasureReport hasContainedOperationOutcome() {
        assertTrue(report().hasContained()
                && report().getContained().stream()
                        .anyMatch(t -> t.getResourceType().equals(ResourceType.OperationOutcome)));
        return this;
    }

    public SelectedMeasureReport hasContainedOperationOutcomeMsg(String expectedMsg) {
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

    // Backward compatibility accessors - delegate to report()
    public List<MeasureReportGroupComponent> getGroup() {
        return report().getGroup();
    }

    public MeasureReportGroupComponent getGroupFirstRep() {
        return report().getGroupFirstRep();
    }

    public Period getPeriod() {
        return report().getPeriod();
    }

    public Reference getSubject() {
        return report().getSubject();
    }

    public String getMeasure() {
        return report().getMeasure();
    }

    public Date getDate() {
        return report().getDate();
    }

    public MeasureReport.MeasureReportType getType() {
        return report().getType();
    }

    public List<Extension> getExtension() {
        return report().getExtension();
    }

    // Log the JSON corresponding to the report at this point:
    public SelectedMeasureReport logReportJson() {
        logger.info(jsonParser.encodeResourceToString(report()));

        return this;
    }
}
