package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
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
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.selected.def.SelectedMeasureDefCollection;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("squid:S1135")
class MultiMeasure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    @FunctionalInterface
    public interface Validator<T> {
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
        private IRepository repository;
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

        public MultiMeasure.Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public MultiMeasure.Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
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

        // Exposed for unit tests that only want to use part of this framework when passing in
        // an IgRepository
        public IRepository getRepository() {
            if (this.repository == null) {
                throw new IllegalStateException(
                        "Repository has not been set. Use 'repository' or 'repositoryFor' to set it.");
            }
            return this.repository;
        }

        private R4MultiMeasureService buildMeasureService() {
            return new R4MultiMeasureService(repository, evaluationOptions, serverBase, measurePeriodValidator);
        }

        public MultiMeasure.When when() {
            return new MultiMeasure.When(buildMeasureService(), this.repository);
        }
    }

    public static class When {
        private final R4MultiMeasureService service;
        private final IRepository repository;

        When(R4MultiMeasureService service, IRepository repository) {
            this.service = service;
            this.repository = repository;
        }

        private List<IdType> measureId = new ArrayList<>();
        private List<String> measureUrl = new ArrayList<>();
        private List<String> measureIdentifier = new ArrayList<>();
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private String reportType;
        private Bundle additionalData;
        private Parameters parameters;

        private Supplier<MeasureDefAndR4ParametersWithMeasureReports> operation;
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

        public MultiMeasure.When periodEnd(String periodEnd) {
            this.periodEnd =
                    LocalDate.parse(periodEnd, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        public MultiMeasure.When periodStart(String periodStart) {
            this.periodStart = LocalDate.parse(periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        public MultiMeasure.When subject(String subjectId) {
            this.subject = subjectId;
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
            this.operation = () -> service.evaluateWithDefs(
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

        public MultiMeasure.Then then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new MultiMeasure.Then(this.operation.get(), this.repository);
        }
    }

    public static class Then {
        private final MeasureDefAndR4ParametersWithMeasureReports evaluation;
        private final IRepository repository;

        Then(MeasureDefAndR4ParametersWithMeasureReports evaluation, IRepository repository) {
            this.evaluation = evaluation;
            this.repository = repository;
        }

        /**
         * Access the Parameters with bundled MeasureReports for post-scoring assertions.
         *
         * @return SelectedReport for fluent MeasureReport assertions on Parameters
         */
        public MultiMeasure.SelectedReport report() {
            return new MultiMeasure.SelectedReport(evaluation.parameters());
        }

        /**
         * Access the List<MeasureDef> collection for pre-scoring assertions.
         *
         * @return SelectedMeasureDefCollection for fluent MeasureDef collection assertions
         */
        public SelectedMeasureDefCollection<Then> defs() {
            return new SelectedMeasureDefCollection<>(evaluation.measureDefs(), this);
        }

        // Backward compatibility - delegate to report()
        public MultiMeasure.Then hasBundleCount(int count) {
            report().hasBundleCount(count);
            return this;
        }

        public MultiMeasure.Then hasMeasureReportCount(int count) {
            report().hasMeasureReportCount(count);
            return this;
        }

        public MultiMeasure.Then hasMeasureReportCountPerUrl(int count, String measureUrl) {
            report().hasMeasureReportCountPerUrl(count, measureUrl);
            return this;
        }

        public MultiMeasure.SelectedMeasureReport measureReport(String measureUrl) {
            return report().measureReport(measureUrl);
        }

        public MultiMeasure.SelectedMeasureReport measureReport(String measureUrl, String subject) {
            return report().measureReport(measureUrl, subject);
        }

        public MultiMeasure.SelectedMeasureReport getFirstMeasureReport() {
            return report().getFirstMeasureReport();
        }
    }

    public static class SelectedReport extends MultiMeasure.Selected<Parameters, Void> {

        public SelectedReport(Parameters report) {
            super(report, null);
        }

        public Parameters report() {
            return this.value();
        }

        public MultiMeasure.SelectedReport hasBundleCount(int count) {
            assertEquals(
                    count,
                    report().getParameter().stream()
                            .map(ParametersParameterComponent::getResource)
                            .filter(Bundle.class::isInstance)
                            .toList()
                            .size());
            return this;
        }

        public MultiMeasure.SelectedReport hasMeasureReportCount(int count) {
            assertEquals(
                    count,
                    report().getParameter().stream()
                            .map(ParametersParameterComponent::getResource)
                            .filter(Bundle.class::isInstance)
                            .map(Bundle.class::cast)
                            .flatMap(b -> BundleHelper.getEntryResources(b).stream())
                            .filter(MeasureReport.class::isInstance)
                            .toList()
                            .size());
            return this;
        }

        public MultiMeasure.SelectedReport hasMeasureReportCountPerUrl(int count, String measureUrl) {
            var reports = report().getParameter().stream()
                    .map(ParametersParameterComponent::getResource)
                    .filter(Bundle.class::isInstance)
                    .map(Bundle.class::cast)
                    .flatMap(b -> BundleHelper.getEntryResources(b).stream())
                    .filter(MeasureReport.class::isInstance)
                    .map(MeasureReport.class::cast)
                    .filter(x -> x.getMeasure().equals(measureUrl))
                    .toList();
            var msg =
                    "measureReport count: %s, does not match for measure url: %s".formatted(reports.size(), measureUrl);
            assertEquals(count, reports.size(), msg);

            return this;
        }

        public SelectedMeasureReport measureReport(MultiMeasure.Selector<MeasureReport, Parameters> bundleSelector) {
            var p = bundleSelector.select(value());
            return new SelectedMeasureReport(p, this);
        }

        public SelectedMeasureReport measureReport(String measureUrl) {
            return this.measureReport(g -> resourceToMeasureReport(
                    g.getParameter().stream()
                            .flatMap(p -> ((Bundle) p.getResource()).getEntry().stream())
                            .filter(x ->
                                    x.getResource().getResourceType().toString().equals("MeasureReport"))
                            .toList(),
                    measureUrl));
        }

        public SelectedMeasureReport measureReport(String measureUrl, String subject) {
            return this.measureReport(g -> resourceToMeasureReport(
                    g.getParameter().stream()
                            .flatMap(p -> ((Bundle) p.getResource()).getEntry().stream())
                            .filter(x ->
                                    x.getResource().getResourceType().toString().equals("MeasureReport"))
                            .toList(),
                    measureUrl,
                    subject));
        }

        public SelectedMeasureReport getFirstMeasureReport() {
            var mr = (MeasureReport) ((Bundle) report().getParameterFirstRep().getResource())
                    .getEntryFirstRep()
                    .getResource();
            return this.measureReport(g -> mr);
        }

        public SelectedMeasureReport getSecondMeasureReport() {
            var entries = report().getParameter().stream()
                    .flatMap(p -> ((Bundle) p.getResource()).getEntry().stream())
                    .toList();
            if (entries.size() < 2) {
                fail("There are not enough entries in the report to get the second one.");
            }
            var secondEntryResource = entries.get(1).getResource();
            if (!(secondEntryResource instanceof MeasureReport measureReport)) {
                fail("The second entry in the report is not a MeasureReport resource.");
                return null;
            }
            return this.measureReport(g -> measureReport);
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

        public MeasureReport resourceToMeasureReport(
                List<BundleEntryComponent> entries, String measureUrl, String subject) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            MeasureReport matchedReport = null;
            for (int i = 0; i < entries.size(); i++) {
                MeasureReport report = (MeasureReport) parser.parseResource(
                        parser.encodeResourceToString(entries.get(i).getResource()));
                if (report.getMeasure().equals(measureUrl)
                        && report.getSubject().getReference().equals(subject)) {
                    matchedReport = report;
                    break;
                }
            }
            return matchedReport;
        }
    }

    public static class SelectedMeasureReport extends MultiMeasure.Selected<MeasureReport, SelectedReport> {
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

        public SelectedMeasureReport hasMeasure(String measureUrl) {
            assertEquals(measureUrl, report().getMeasure());
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

        public SelectedMeasureReport hasMeasureReportStatus(MeasureReportStatus status) {
            assertEquals(status, report().getStatus());
            return this;
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

        public SelectedMeasureReport hasPeriodStart(Date periodStart) {
            var period = this.report().getPeriod();
            assertEquals(periodStart, period.getStart());
            return this;
        }

        public SelectedMeasureReport hasPeriodEnd(Date periodEnd) {
            var period = this.report().getPeriod();
            assertEquals(periodEnd, period.getEnd());
            return this;
        }
    }

    public static class SelectedGroup
            extends MultiMeasure.Selected<MeasureReportGroupComponent, SelectedMeasureReport> {

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
            assertEquals(count, this.value().getStratifier().size());
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

    public static class SelectedReference<P> extends MultiMeasure.Selected<Reference, P> {

        public SelectedReference(Reference value, P parent) {
            super(value, parent);
        }

        // Hmm.. may need to rethink this one a bit.
        public SelectedReference<P> hasPopulations(String... population) {
            var ex = this.value().getExtensionsByUrl(MeasureConstants.EXT_CRITERIA_REFERENCE_URL);
            if (ex.isEmpty()) {
                throw new IllegalStateException(
                        "no evaluated resource extensions were found, and expected %s".formatted(population.length));
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

    public static class SelectedPopulation
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

    public static class SelectedStratifier
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

    public static class SelectedStratum extends MultiMeasure.Selected<StratifierGroupComponent, SelectedStratifier> {

        public SelectedStratum(MeasureReport.StratifierGroupComponent value, SelectedStratifier parent) {
            super(value, parent);
        }

        public SelectedStratum hasScore(String score) {
            MeasureValidationUtils.validateStratumScore(value(), score);
            return this;
        }

        public SelectedStratum hasValue(String text) {
            assertEquals(text, value().getValue().getText());
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
