package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.dstu3.Measure.SelectedGroup.SelectedReference;
import org.opencds.cqf.fhir.cr.measure.dstu3.selected.def.SelectedMeasureDef;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class Measure {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/dstu3";

    @FunctionalInterface
    interface Validator<T> {
        void validate(T value);
    }

    @FunctionalInterface
    interface Selector<T, S> {
        T select(S from);
    }

    public interface ChildOf<T> {
        T up();
    }

    public interface SelectedOf<T> {
        T selected();
    }

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private IRepository repository;
        private MeasureEvaluationOptions evaluationOptions;

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

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(String repositoryPath) {
            this.repository = new IgRepository(
                    FhirContext.forDstu3Cached(),
                    Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationOptions(MeasureEvaluationOptions evaluationOptions) {
            this.evaluationOptions = evaluationOptions;
            return this;
        }

        private Dstu3MeasureProcessor buildProcessor() {
            return new Dstu3MeasureProcessor(repository, evaluationOptions, new Dstu3RepositorySubjectProvider());
        }

        public IRepository getRepository() {
            return repository;
        }

        public When when() {
            return new When(buildProcessor(), repository);
        }
    }

    public static class When {
        private final Dstu3MeasureProcessor processor;
        private final IRepository repository;

        When(Dstu3MeasureProcessor processor, IRepository repository) {
            this.processor = processor;
            this.repository = repository;
        }

        private String measureId;

        private String periodStart;
        private String periodEnd;

        private String subjectId;
        private String reportType;

        private Bundle additionalData;
        private Parameters parameters;

        private Supplier<MeasureDefAndDstu3MeasureReport> operation;

        public When measureId(String measureId) {
            this.measureId = measureId;
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
            this.subjectId = subjectId;
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

        public When evaluate() {
            this.operation = () -> processor.evaluateMeasureCaptureDefs(
                    new IdType("Measure", measureId),
                    periodStart,
                    periodEnd,
                    reportType,
                    Collections.singletonList(this.subjectId),
                    additionalData,
                    parameters);
            return this;
        }

        public Then then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            return new Then(this.operation.get());
        }
    }

    public static class Then {
        private final MeasureDefAndDstu3MeasureReport evaluation;

        Then(MeasureDefAndDstu3MeasureReport evaluation) {
            this.evaluation = evaluation;
        }

        /**
         * Access the MeasureReport hierarchy for post-scoring assertions.
         *
         * @return SelectedReport for fluent MeasureReport assertions
         */
        public SelectedReport report() {
            return new SelectedReport(evaluation.measureReport());
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
        public SelectedGroup firstGroup() {
            return report().firstGroup();
        }

        public SelectedGroup group(String id) {
            return report().group(id);
        }

        public SelectedGroup group(Selector<MeasureReportGroupComponent, MeasureReport> groupSelector) {
            return report().group(groupSelector);
        }

        public Then passes(Validator<MeasureReport> measureReportValidator) {
            report().passes(measureReportValidator);
            return this;
        }
    }

    public static class SelectedReport implements SelectedOf<MeasureReport>, ChildOf<Void> {

        private final MeasureReport report;

        public SelectedReport(MeasureReport report) {
            this.report = report;
        }

        public SelectedReport passes(Validator<MeasureReport> measureReportValidator) {
            measureReportValidator.validate(report);
            return this;
        }

        public MeasureReport report() {
            return this.report;
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
            var g = groupSelector.select(report);
            return new SelectedGroup(this, g);
        }

        public SelectedReference<SelectedReport> reference(Selector<Reference, MeasureReport> referenceSelector) {
            var r = referenceSelector.select(report);
            return new SelectedReference<>(this, r);
        }

        @Override
        public MeasureReport selected() {
            return this.report;
        }

        @Override
        public Void up() {
            return null;
        }
    }

    public static class SelectedGroup
            implements ChildOf<SelectedReport>, SelectedOf<MeasureReport.MeasureReportGroupComponent> {
        private final SelectedReport selectedReport;
        private final MeasureReport.MeasureReportGroupComponent group;

        public SelectedGroup(SelectedReport generatedReport, MeasureReport.MeasureReportGroupComponent group) {
            this.selectedReport = generatedReport;
            this.group = group;
        }

        public SelectedReport up() {
            return this.selectedReport;
        }

        public SelectedGroup hasScore(String score) {
            MeasureValidationUtils.validateGroupScore(this.group, score);
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
            var p = populationSelector.select(group);
            return new SelectedPopulation(this, p);
        }

        public SelectedPopulation firstPopulation() {
            return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
        }

        public SelectedGroup hasStratifierCount(int count) {
            assertEquals(this.group.getStratifier().size(), count);
            return this;
        }

        public SelectedStratifier firstStratifier() {
            return this.stratifier(MeasureReport.MeasureReportGroupComponent::getStratifierFirstRep);
        }

        public SelectedStratifier stratifier(
                Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent> stratifierSelector) {
            var s = stratifierSelector.select(group);
            return new SelectedStratifier(this, s);
        }

        @Override
        public MeasureReportGroupComponent selected() {
            return this.group;
        }

        public static class SelectedReference<T> implements ChildOf<T>, SelectedOf<Reference> {
            private final T parent;
            private final Reference reference;

            public SelectedReference(T parent, Reference reference) {
                this.parent = parent;
                this.reference = reference;
            }

            public T up() {
                return parent;
            }

            // Hmm.. may need to rethink this one a bit.
            public SelectedReference<T> hasPopulations(String... population) {
                var ex = this.reference.getExtensionsByUrl(MeasureConstants.EXT_CRITERIA_REFERENCE_URL);
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

            @Override
            public Reference selected() {
                return this.reference;
            }
        }

        public static class SelectedPopulation
                implements ChildOf<SelectedGroup>, SelectedOf<MeasureReport.MeasureReportGroupPopulationComponent> {
            private final SelectedGroup selectedGroup;
            private final MeasureReport.MeasureReportGroupPopulationComponent population;

            public SelectedPopulation(
                    SelectedGroup selectedGroup, MeasureReport.MeasureReportGroupPopulationComponent population) {
                this.selectedGroup = selectedGroup;
                this.population = population;
            }

            public SelectedGroup up() {
                return this.selectedGroup;
            }

            public SelectedPopulation hasCount(int count) {
                MeasureValidationUtils.validatePopulation(population, count);
                return this;
            }

            public SelectedPopulation passes(
                    Validator<MeasureReport.MeasureReportGroupPopulationComponent> populationValidator) {
                populationValidator.validate(this.population);
                return this;
            }

            @Override
            public MeasureReportGroupPopulationComponent selected() {
                return this.population;
            }
        }
    }

    public static class SelectedStratifier
            implements ChildOf<SelectedGroup>, SelectedOf<MeasureReport.MeasureReportGroupStratifierComponent> {

        private final SelectedGroup selectedGroup;
        private final MeasureReport.MeasureReportGroupStratifierComponent stratifier;

        public SelectedStratifier(
                SelectedGroup selectedGroup, MeasureReport.MeasureReportGroupStratifierComponent stratifier) {
            this.selectedGroup = selectedGroup;
            this.stratifier = stratifier;
        }

        @Override
        public MeasureReportGroupStratifierComponent selected() {
            return stratifier;
        }

        @Override
        public SelectedGroup up() {
            return selectedGroup;
        }
    }
}
