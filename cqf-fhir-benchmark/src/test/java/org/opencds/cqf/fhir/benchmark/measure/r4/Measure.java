package org.opencds.cqf.fhir.benchmark.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class Measure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

    interface SelectedOf<T> {
        T value();
    }

    protected static class Selected<T> implements SelectedOf<T> {
        private final T value;

        public Selected(T value) {
            this.value = value;
        }

        @Override
        public T value() {
            return value;
        }
    }

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private IRepository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private String serverBase;
        private final MeasurePeriodValidator measurePeriodValidator;
        private final R4MeasureServiceUtils measureServiceUtils;

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

            this.measureServiceUtils = new R4MeasureServiceUtils(repository);
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
        private final R4MultiMeasureService service;

        When(R4MultiMeasureService service) {
            this.service = service;
        }

        private String measureId;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private String reportType;
        private Bundle additionalData;

        private Supplier<MeasureReport> operation;

        public When measureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        public When periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
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
                    null,
                    null,
                    null);
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

    public static class SelectedReport extends Selected<MeasureReport> {
        public SelectedReport(MeasureReport report) {
            super(report);
        }

        public MeasureReport report() {
            return this.value();
        }
    }
}
