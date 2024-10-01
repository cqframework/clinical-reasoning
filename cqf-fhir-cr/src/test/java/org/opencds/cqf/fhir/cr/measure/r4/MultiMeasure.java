package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class MultiMeasure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

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

    protected static class Selected<T, P> implements MultiMeasure.SelectedOf<T>, MultiMeasure.ChildOf<P> {
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

    public static MultiMeasure.Given given() {
        return new MultiMeasure.Given();
    }

    public static class Given {
        private Repository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private String serverBase;
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

            this.serverBase = "http://localhost";

            this.measurePeriodValidator = new MeasurePeriodValidator();
        }

        public MultiMeasure.Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public MultiMeasure.Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public MultiMeasure.Given evaluationOptions(MeasureEvaluationOptions evaluationOptions) {
            this.evaluationOptions = evaluationOptions;
            return this;
        }

        public MultiMeasure.Given serverBase(String serverBase) {
            this.serverBase = serverBase;
            return this;
        }

        private R4MeasureProcessor buildProcessor() {
            return new R4MeasureProcessor(repository, evaluationOptions, new R4RepositorySubjectProvider());
        }

        private R4MultiMeasureService buildMeasureService() {
            return new R4MultiMeasureService(repository, evaluationOptions, serverBase, measurePeriodValidator);
        }

        public MultiMeasure.When when() {
            return new MultiMeasure.When(buildMeasureService());
        }
    }

    public static class When {
        // private final R4MeasureProcessor processor;
        private final R4MultiMeasureService service;

        When(R4MultiMeasureService service) {
            this.service = service;
        }

        private List<IdType> measureId = new ArrayList<>();
        private List<String> measureUrl = new ArrayList<>();
        private List<String> measureIdentifier = new ArrayList<>();
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private List<String> subjectIds;
        private String subject;
        private String reportType;
        private Bundle additionalData;
        private Parameters parameters;

        private Supplier<Bundle> operation;
        private String productLine;
        private String reporter;

        public MultiMeasure.When measureId(String measureId) {
            this.measureId.add(new IdType("Measure", measureId));
            return this;
        }

        public MultiMeasure.When measureIdentifier(String measureIdentifier) {
            this.measureIdentifier.add(measureIdentifier);
            return this;
        }

        public MultiMeasure.When measureUrl(String measureUrl) {
            this.measureUrl.add(measureUrl);
            return this;
        }

        public MultiMeasure.When periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public MultiMeasure.When periodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public MultiMeasure.When subject(String subjectId) {
            this.subject = subjectId;
            return this;
        }

        public MultiMeasure.When subjects(List<String> subjectIds) {
            this.subjectIds = subjectIds;
            return this;
        }

        public MultiMeasure.When reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        public MultiMeasure.When additionalData(Bundle additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public MultiMeasure.When parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public MultiMeasure.When productLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        public MultiMeasure.When reporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        public MultiMeasure.When evaluate() {
            this.operation = () -> service.evaluate(
                    measureId,
                    measureUrl,
                    measureIdentifier,
                    periodStart,
                    periodEnd,
                    reportType,
                    subject,
                    null,
                    null,
                    null,
                    additionalData,
                    parameters,
                    productLine,
                    reporter);
            return this;
        }

        public MultiMeasure.SelectedReport then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new MultiMeasure.SelectedReport(this.operation.get());
        }
    }

    public static class SelectedReport extends MultiMeasure.Selected<Bundle, Void> {

        public SelectedReport(Bundle report) {
            super(report, null);
        }

        public Bundle report() {
            return this.value();
        }

        public MultiMeasure.SelectedReport hasMeasureReportCount(int count) {
            assertEquals(count, report().getEntry().size());
            return this;
        }

        public MultiMeasure.SelectedReport hasMeasureReportCountPerUrl(int count, String measureUrl) {
            var reports = report().getEntry().stream()
                    .map(t -> (MeasureReport) t.getResource())
                    .filter(x -> x.getMeasure().equals(measureUrl))
                    .collect(Collectors.toList());
            var msg = String.format(
                    "measureReport count: %s, does not match for measure url: %s", reports.size(), measureUrl);
            assertEquals(count, reports.size(), msg);

            return this;
        }

        public SelectedMeasureReport measureReport(MultiMeasure.Selector<MeasureReport, Bundle> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedMeasureReport(p, this);
        }

        public SelectedMeasureReport measureReport(String measureUrl) {
            return this.measureReport(g -> resourceToMeasureReport(
                    g.getEntry().stream()
                            .filter(x ->
                                    x.getResource().getResourceType().toString().equals("MeasureReport"))
                            .collect(Collectors.toList()),
                    measureUrl));
        }

        public SelectedMeasureReport getFirstMeasureReport() {
            var mr = (MeasureReport) report().getEntryFirstRep().getResource();
            return this.measureReport(g -> mr);
        }

        public MeasureReport resourceToMeasureReport(List<BundleEntryComponent> entries, String measureUrl) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            MeasureReport matchedReport = null;
            for (int i = 0; i < entries.size(); i++) {
                MeasureReport report = (MeasureReport) parser.parseResource(
                        parser.encodeResourceToString(entries.get(i).getResource()));
                if (report.getMeasure().equals(measureUrl)) {
                    matchedReport = report;
                    break;
                }
            }
            return matchedReport;
        }
    }

    static class SelectedMeasureReport extends MultiMeasure.Selected<MeasureReport, SelectedReport> {
        public MeasureReport report() {
            return this.value();
        }

        public SelectedMeasureReport(MeasureReport value, SelectedReport parent) {
            super(value, parent);
        }

        public MeasureReport measureReport() {
            return this.value();
        }
        // measure url found
        public SelectedMeasureReport measureReportMatches(String measureReportUrl) {
            assertEquals(measureReportUrl, measureReport().getMeasure());
            return this;
        }
        // subject ref found
        public SelectedMeasureReport measureReportSubjectMatches(String subjectReference) {
            assertEquals(subjectReference, measureReport().getSubject().getReference());
            return this;
        }
        // reportType individual
        public SelectedMeasureReport measureReportTypeIndividual() {
            assertEquals(MeasureReportType.INDIVIDUAL, measureReport().getType());
            return this;
        }

        public SelectedReference<SelectedMeasureReport> reference(
                org.opencds.cqf.fhir.cr.measure.r4.Measure.Selector<Reference, MeasureReport> referenceSelector) {
            var r = referenceSelector.select(value());
            return new SelectedReference<>(r, this);
        }

        public SelectedReference<SelectedMeasureReport> evaluatedResource(String name) {
            return this.reference(x -> x.getEvaluatedResource().stream()
                    .filter(y -> y.getReference().equals(name))
                    .findFirst()
                    .get());
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

        public SelectedMeasureReport hasEvaluatedResourceCount(int count) {
            assertEquals(count, report().getEvaluatedResource().size());
            return this;
        }

        public SelectedMeasureReport hasMeasureVersion(String version) {
            assertEquals(
                    version,
                    report().getMeasure().substring(report().getMeasure().indexOf('|') + 1));
            return this;
        }

        public SelectedMeasureReport hasContainedResourceCount(int count) {
            assertEquals(count, report().getContained().size());
            return this;
        }

        // TODO: SelectedContained resource class?
        public SelectedMeasureReport hasContainedResource(Predicate<Resource> criteria) {
            var contained = this.report().getContained().stream();
            assertTrue(contained.anyMatch(criteria), "Did not find a resource matching this criteria ");
            return this;
        }

        // TODO: SelectedExtension class?
        public SelectedMeasureReport hasExtension(String url, int count) {
            var ex = this.value().getExtensionsByUrl(url);
            assertEquals(ex.size(), count);

            return this;
        }

        public SelectedMeasureReport hasSubjectReference(String subjectReference) {
            var ref = this.report().getSubject();
            assertEquals(ref.getReference(), subjectReference);
            return this;
        }

        public SelectedMeasureReport hasReportType(String reportType) {
            var ref = this.report().getType();
            assertEquals(reportType, ref.getDisplay());
            return this;
        }

        public SelectedMeasureReport hasReporter(String reporter) {
            var ref = this.report().getReporter().getReference();
            assertEquals(reporter, ref);
            return this;
        }
    }

    static class SelectedGroup extends MultiMeasure.Selected<MeasureReportGroupComponent, SelectedMeasureReport> {

        public SelectedGroup(MeasureReportGroupComponent value, SelectedMeasureReport parent) {
            super(value, parent);
        }

        public SelectedGroup hasScore(String score) {
            MeasureValidationUtils.validateGroupScore(this.value(), score);
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
                MultiMeasure.Selector<MeasureReportGroupPopulationComponent, MeasureReportGroupComponent>
                        populationSelector) {
            var p = populationSelector.select(value());
            return new SelectedPopulation(p, this);
        }

        public SelectedPopulation firstPopulation() {
            return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
        }

        public SelectedGroup hasStratifierCount(int count) {
            assertEquals(this.value().getStratifier().size(), count);
            return this;
        }

        public SelectedStratifier firstStratifier() {
            return this.stratifier(MeasureReport.MeasureReportGroupComponent::getStratifierFirstRep);
        }

        public SelectedStratifier stratifier(
                MultiMeasure.Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent>
                        stratifierSelector) {
            var s = stratifierSelector.select(value());
            return new SelectedStratifier(s, this);
        }
    }

    static class SelectedReference<P> extends MultiMeasure.Selected<Reference, P> {

        public SelectedReference(Reference value, P parent) {
            super(value, parent);
        }

        // Hmm.. may need to rethink this one a bit.
        public SelectedReference<P> hasPopulations(String... population) {
            var ex = this.value().getExtensionsByUrl(MeasureConstants.EXT_CRITERIA_REFERENCE_URL);
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
            extends MultiMeasure.Selected<MeasureReportGroupPopulationComponent, SelectedGroup> {

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
                MultiMeasure.Validator<MeasureReportGroupPopulationComponent> populationValidator) {
            populationValidator.validate(value());
            return this;
        }
    }

    static class SelectedStratifier
            extends MultiMeasure.Selected<MeasureReportGroupStratifierComponent, SelectedGroup> {

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
                MultiMeasure.Selector<StratifierGroupComponent, MeasureReportGroupStratifierComponent>
                        stratumSelector) {
            var s = stratumSelector.select(value());
            return new SelectedStratum(s, this);
        }
    }

    static class SelectedStratum extends MultiMeasure.Selected<StratifierGroupComponent, SelectedStratifier> {

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
                MultiMeasure.Selector<StratifierGroupPopulationComponent, StratifierGroupComponent>
                        populationSelector) {
            var p = populationSelector.select(value());
            return new SelectedStratumPopulation(p, this);
        }
    }

    static class SelectedStratumPopulation
            extends MultiMeasure.Selected<StratifierGroupPopulationComponent, SelectedStratum> {

        public SelectedStratumPopulation(
                MeasureReport.StratifierGroupPopulationComponent value, SelectedStratum parent) {
            super(value, parent);
        }

        public SelectedStratumPopulation hasCount(int count) {
            assertEquals(this.value().getCount(), count);
            return this;
        }
    }
}
