package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.selected.def.SelectedMeasureDef;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReport;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReportContained;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReportExtension;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReportGroup;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReportReference;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

// consider rolling this entire thing into MultiMeasure with "single measure" assertions
@SuppressWarnings({"squid:S2699", "squid:S5960", "squid:S1135"})
public class Measure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    @FunctionalInterface
    public interface Validator<T> {
        void validate(T value);
    }

    @FunctionalInterface
    public interface Selector<T, S> {
        T select(S from);
    }

    public interface ChildOf<T> {
        T up();
    }

    public interface SelectedOf<T> {
        T value();
    }

    public static class Selected<T, P> implements SelectedOf<T>, ChildOf<P> {
        protected final P parent;
        protected final T value;

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
        private String serverBase;
        private final MeasurePeriodValidator measurePeriodValidator;

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

            this.serverBase = "http://localhost";

            this.measurePeriodValidator = new MeasurePeriodValidator();
        }

        public IRepository getRepository() {
            return repository;
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

        private R4MultiMeasureService buildMultiMeasureService() {
            return new R4MultiMeasureService(repository, evaluationOptions, serverBase, measurePeriodValidator);
        }

        public When when() {
            return new When(buildMultiMeasureService());
        }
    }

    public static class When {
        private final R4MultiMeasureService multiMeasureService;

        When(R4MultiMeasureService multiMeasureService) {
            this.multiMeasureService = multiMeasureService;
        }

        private String measureId;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private String reportType;
        private Bundle additionalData;
        private Parameters parameters;

        private Supplier<MeasureDefAndR4MeasureReport> operation;
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
            this.operation = () -> multiMeasureService.evaluateSingleMeasureCaptureDef(
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

        public Then then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new Then(this.operation.get(), this.multiMeasureService.getRepository());
        }
    }

    public static class Then {
        private final MeasureDefAndR4MeasureReport evaluation;
        private final IRepository repository;

        Then(MeasureDefAndR4MeasureReport evaluation, IRepository repository) {
            this.evaluation = evaluation;
            this.repository = repository;
        }

        /**
         * Access the MeasureReport hierarchy for post-scoring assertions.
         *
         * @return SelectedMeasureReport for fluent MeasureReport assertions
         */
        public SelectedMeasureReport report() {
            return new SelectedMeasureReport(evaluation.measureReport(), this, repository);
        }

        /**
         * Get the raw MeasureReport object.
         *
         * @return raw MeasureReport
         */
        public MeasureReport measureReport() {
            return evaluation.measureReport();
        }

        /**
         * Access the MeasureDef hierarchy for pre-scoring assertions.
         *
         * @return SelectedMeasureDef for fluent MeasureDef assertions
         */
        public SelectedMeasureDef<Then> def() {
            return new SelectedMeasureDef<>(evaluation.measureDef(), this);
        }

        // Backward compatibility - delegate to report()
        public SelectedMeasureReportGroup firstGroup() {
            return report().firstGroup();
        }

        public SelectedMeasureReportGroup group(String id) {
            return report().group(id);
        }

        public SelectedMeasureReportGroup group(int index) {
            return report().group(index);
        }

        public Then hasGroupCount(int count) {
            report().hasGroupCount(count);
            return this;
        }

        public Then hasContainedResourceCount(int count) {
            report().hasContainedResourceCount(count);
            return this;
        }

        public Then hasMeasureVersion(String version) {
            report().hasMeasureVersion(version);
            return this;
        }

        public Then hasMeasureUrl(String url) {
            report().hasMeasureUrl(url);
            return this;
        }

        public Then hasEvaluatedResourceCount(int count) {
            report().hasEvaluatedResourceCount(count);
            return this;
        }

        public Then hasReportType(String reportType) {
            report().hasReportType(reportType);
            return this;
        }

        public Then hasSubjectReference(String reference) {
            report().hasSubjectReference(reference);
            return this;
        }

        public Then hasPatientReference(String reference) {
            report().hasPatientReference(reference);
            return this;
        }

        public Then hasImprovementNotation(String code) {
            report().hasImprovementNotation(code);
            return this;
        }

        public Then passes(Validator<MeasureReport> validator) {
            report().passes(validator);
            return this;
        }

        public Then hasPeriodStart(Date periodStart) {
            report().hasPeriodStart(periodStart);
            return this;
        }

        public Then hasPeriodEnd(Date periodEnd) {
            report().hasPeriodEnd(periodEnd);
            return this;
        }

        public Then hasStatus(MeasureReportStatus status) {
            report().hasStatus(status);
            return this;
        }

        public Then hasContainedResource(Predicate<Resource> criteria) {
            report().hasContainedResource(criteria);
            return this;
        }

        public Then hasContainedOperationOutcome() {
            report().hasContainedOperationOutcome();
            return this;
        }

        public Then hasContainedOperationOutcomeMsg(String expectedMsg) {
            report().hasContainedOperationOutcomeMsg(expectedMsg);
            return this;
        }

        public Then hasExtension(String url, int count) {
            report().hasExtension(url, count);
            return this;
        }

        public Then evaluatedResourceHasNoDuplicateReferences() {
            report().evaluatedResourceHasNoDuplicateReferences();
            return this;
        }

        public SelectedMeasureReportReference evaluatedResource(String name) {
            return report().evaluatedResource(name);
        }

        public Then hasMeasureReportDate() {
            report().hasMeasureReportDate();
            return this;
        }

        public Then hasEmptySubject() {
            report().hasEmptySubject();
            return this;
        }

        public Then hasMeasureReportPeriod() {
            report().hasMeasureReportPeriod();
            return this;
        }

        public Then hasNoReportLevelImprovementNotation() {
            report().hasNoReportLevelImprovementNotation();
            return this;
        }

        public Then hasReportLevelImprovementNotation() {
            report().hasReportLevelImprovementNotation();
            return this;
        }

        public Then improvementNotationCode(String code) {
            report().improvementNotationCode(code);
            return this;
        }

        public Then containedObservationsHaveMatchingExtension() {
            report().containedObservationsHaveMatchingExtension();
            return this;
        }

        public Then subjectResultsValidation() {
            report().subjectResultsValidation();
            return this;
        }

        public Then subjectResultsHaveResourceType(String resourceType) {
            report().subjectResultsHaveResourceType(resourceType);
            return this;
        }

        public Then containedListHasCorrectResourceType(String resourceType) {
            report().containedListHasCorrectResourceType(resourceType);
            return this;
        }

        public SelectedMeasureReportContained containedByValue(String codeValue) {
            return report().containedByValue(codeValue);
        }

        public SelectedMeasureReportContained containedByCoding(String codeCoding) {
            return report().containedByCoding(codeCoding);
        }

        public SelectedMeasureReportExtension extensionByValueReference(String resourceReference) {
            return report().extensionByValueReference(resourceReference);
        }

        public SelectedMeasureReportExtension extension(String supplementalDataId) {
            return report().extension(supplementalDataId);
        }
    }
}
