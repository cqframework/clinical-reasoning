package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class CollectData {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

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
        private final MeasureEvaluationOptions evaluationOptions;

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

        private R4CollectDataService buildR4CollectDataService() {
            return new R4CollectDataService(repository, evaluationOptions);
        }

        public When when() {
            return new When(buildR4CollectDataService());
        }
    }

    public static class When {
        private final R4CollectDataService service;

        When(R4CollectDataService service) {
            this.service = service;
        }

        private IdType theId;
        private String periodStart;
        private String periodEnd;
        private String subject;
        private String practitioner;
        private Supplier<Parameters> operation;

        public When measureId(String theId) {
            this.theId = new IdType(theId);
            return this;
        }

        public When periodEnd(String periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public When periodStart(String periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public When subject(String subjectId) {
            this.subject = subjectId;
            return this;
        }

        public When practitioner(String practitioner) {
            this.practitioner = practitioner;
            return this;
        }

        public When collectData() {
            this.operation = () -> service.collectData(theId, periodStart, periodEnd, subject, practitioner);
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

    public static class SelectedReport extends Selected<Parameters, Void> {
        public SelectedReport(Parameters report) {
            super(report, null);
        }

        public SelectedMeasureReport measureReport() {
            return this.measureReport(g -> resourceToMeasureReport(g.getParameter().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("MeasureReport"))
                    .findFirst()
                    .get()
                    .getResource()));
        }

        public SelectedMeasureReport measureReport(CollectData.Selector<MeasureReport, Parameters> paramSelector) {
            var p = paramSelector.select(value());
            return new SelectedMeasureReport(p, this);
        }

        public MeasureReport resourceToMeasureReport(Resource theResource) {
            IParser parser = FhirContext.forR4Cached().newJsonParser();
            return (MeasureReport) parser.parseResource(parser.encodeResourceToString(theResource));
        }

        public SelectedReport hasParameterCount(int count) {
            assertEquals(count, report().getParameter().size());
            return this;
        }

        public SelectedReport hasMeasureReportCount(int count) {
            assertEquals(count, (int) report().getParameter().stream()
                    .filter(x -> x.getResource().getResourceType().toString().equals("MeasureReport"))
                    .count());
            return this;
        }

        public Parameters report() {
            return this.value();
        }
    }

    static class SelectedMeasureReport extends CollectData.Selected<MeasureReport, SelectedReport> {

        public SelectedMeasureReport(MeasureReport value, SelectedReport parent) {
            super(value, parent);
        }

        public MeasureReport measureReport() {
            return this.value();
        }

        public CollectData.SelectedMeasureReport hasEvaluatedResourceCount(int count) {
            assertEquals(count, measureReport().getEvaluatedResource().size());
            return this;
        }

        public CollectData.SelectedMeasureReport hasDataCollectionReportType() {
            assertEquals(
                    MeasureReport.MeasureReportType.DATACOLLECTION,
                    measureReport().getType());
            return this;
        }
    }
}
