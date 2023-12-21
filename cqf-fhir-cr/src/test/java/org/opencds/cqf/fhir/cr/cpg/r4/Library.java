package org.opencds.cqf.fhir.cr.cpg.r4;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

public class Library {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/cpg/r4";

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

    protected static class Selected<T, P> implements Library.SelectedOf<T>, Library.ChildOf<P> {
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

    public static Library.Given given() {
        return new Library.Given();
    }

    public static class Given {
        private Repository repository;
        private EvaluationSettings evaluationSettings;

        public Given() {
            this.evaluationSettings = EvaluationSettings.getDefault();
            this.evaluationSettings
                    .getRetrieveSettings()
                    .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                    .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

            this.evaluationSettings
                    .getTerminologySettings()
                    .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        }

        public Library.Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Library.Given repositoryFor(String repositoryPath) {
            this.repository = TestRepositoryFactory.createRepository(
                    FhirContext.forR4Cached(),
                    this.getClass(),
                    CLASS_PATH + "/" + repositoryPath,
                    IGLayoutMode.DIRECTORY);
            return this;
        }

        public Library.Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        private R4CqlExecutionService buildCqlService() {
            return new R4CqlExecutionService(repository, evaluationSettings);
        }

        private R4LibraryEvaluationService buildLibraryEvaluationService() {
            return new R4LibraryEvaluationService(repository, evaluationSettings);
        }

        public Library.When when() {
            return new Library.When(buildCqlService(), buildLibraryEvaluationService());
        }
    }

    public static class When {
        // private final R4MeasureProcessor processor;
        private final R4CqlExecutionService cqlService;
        private final R4LibraryEvaluationService libraryEvalService;

        When(R4CqlExecutionService cqlService, R4LibraryEvaluationService libraryEvalService) {
            this.cqlService = cqlService;
            this.libraryEvalService = libraryEvalService;
        }
        // Library Eval Service params
        private IdType theId;
        private String subject;
        private List<String> expressionList;
        private Parameters parameters;
        private Bundle data;
        private List<Parameters> prefetchData;
        private Endpoint dataEndpoint;
        private Endpoint contentEndpoint;
        private Endpoint terminologyEndpoint;

        // CQL Service specific params
        private String expression;
        private List<Parameters> library;
        private String content;

        private Supplier<Parameters> operation;

        public Library.When id(IdType theId) {
            this.theId = theId;
            return this;
        }

        public Library.When subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Library.When expressionList(List<String> expressionList) {
            this.expressionList = expressionList;
            return this;
        }

        public Library.When parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Library.When data(Bundle data) {
            this.data = data;
            return this;
        }

        public Library.When prefetchData(List<Parameters> prefetchData) {
            this.prefetchData = prefetchData;
            return this;
        }

        public Library.When dataEndpoint(Endpoint dataEndpoint) {
            this.dataEndpoint = dataEndpoint;
            return this;
        }

        public Library.When contentEndpoint(Endpoint contentEndpoint) {
            this.contentEndpoint = contentEndpoint;
            return this;
        }

        public Library.When terminologyEndpoint(Endpoint terminologyEndpoint) {
            this.terminologyEndpoint = terminologyEndpoint;
            return this;
        }

        public Library.When expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Library.When library(List<Parameters> library) {
            this.library = library;
            return this;
        }

        public Library.When content(String content) {
            this.content = content;
            return this;
        }

        public Library.When evaluateCql() {
            this.operation = () -> cqlService.evaluate(
                    subject,
                    expression,
                    parameters,
                    library,
                    null,
                    data,
                    prefetchData,
                    dataEndpoint,
                    contentEndpoint,
                    terminologyEndpoint,
                    content);
            return this;
        }

        public Library.When evaluateLibrary() {
            this.operation = () -> libraryEvalService.evaluate(
                    theId,
                    subject,
                    expressionList,
                    parameters,
                    data,
                    prefetchData,
                    dataEndpoint,
                    contentEndpoint,
                    terminologyEndpoint);
            return this;
        }

        public Library.SelectedParameters then() {
            if (this.operation == null) {
                throw new IllegalStateException(
                        "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
            }

            Parameters parameters;
            try {
                parameters = this.operation.get();
            } catch (Exception e) {
                throw new IllegalStateException("error when running 'then' and invoking the chosen operation", e);
            }

            return new Library.SelectedParameters(parameters);
        }
    }

    public static class SelectedParameters extends Library.Selected<Parameters, Void> {
        public SelectedParameters(Parameters parameters) {
            super(parameters, null);
        }

        public Library.SelectedParameters passes(Library.Validator<Parameters> parametersValidator) {
            parametersValidator.validate(value());
            return this;
        }

        public Parameters parameters() {
            return this.value();
        }
    }
}
