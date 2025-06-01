package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class DataRequirements {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";

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

            this.evaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .setQueryBatchThreshold(1000);
            this.evaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .setMaxCodesPerQuery(100);
            this.evaluationOptions
                    .getEvaluationSettings()
                    .getCqlOptions()
                    .getCqlEngineOptions()
                    .setPageSize(50);
        }

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forR4Cached(),
                    Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        private R4DataRequirementsService buildR4DataRequirementsService() {
            return new R4DataRequirementsService(repository, evaluationOptions);
        }

        public When when() {
            return new When(buildR4DataRequirementsService());
        }
    }

    public static class When {
        private final R4DataRequirementsService service;

        When(R4DataRequirementsService service) {
            this.service = service;
        }

        private IdType measureId;
        private String periodStart;
        private String periodEnd;
        private Supplier<Library> operation;

        public When measureId(String measureId) {
            this.measureId = new IdType(measureId);
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

        public When DataRequirements() {
            this.operation = () -> service.dataRequirements(measureId, periodStart, periodEnd);
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

    public static class SelectedReport extends Selected<Library, Void> {
        public SelectedReport(Library report) {
            super(report, null);
        }

        public SelectedReport hasParameterDefCount(int count) {
            assertEquals(count, report().getParameter().size());
            return this;
        }

        public SelectedReport hasDataRequirementCount(int count) {
            assertEquals(count, report().getDataRequirement().size());
            return this;
        }

        public SelectedReport hasRelatedArtifactCount(int count) {
            assertEquals(count, report().getRelatedArtifact().size());
            return this;
        }

        public Library report() {
            return this.value();
        }
    }
}
