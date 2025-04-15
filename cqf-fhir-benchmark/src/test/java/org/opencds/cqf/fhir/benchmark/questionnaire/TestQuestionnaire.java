package org.opencds.cqf.fhir.benchmark.questionnaire;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.questionnaire.QuestionnaireProcessor;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class TestQuestionnaire {
    public static final String CLASS_PATH = "shared";

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private Repository repository;
        private EvaluationSettings evaluationSettings;

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public QuestionnaireProcessor buildProcessor(Repository repository) {
            if (evaluationSettings == null) {
                evaluationSettings = EvaluationSettings.getDefault();
                evaluationSettings
                        .getRetrieveSettings()
                        .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                        .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

                evaluationSettings
                        .getTerminologySettings()
                        .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
            }
            return new QuestionnaireProcessor(repository, evaluationSettings, null, null, null, null);
        }

        public When when() {
            return new When(buildProcessor(repository));
        }
    }

    public static class When {

        private final QuestionnaireProcessor processor;
        private IPrimitiveType<String> questionnaireUrl;
        private IIdType questionnaireId;
        private IBaseResource questionnaire;
        private String subjectId;
        private List<IBaseBackboneElement> context;
        private IBaseExtension<?, ?> launchContext;
        private final boolean useServerData;
        private IBaseBundle data;
        private IBaseParameters parameters;

        When(QuestionnaireProcessor processor) {
            this.processor = processor;
            useServerData = true;
        }

        public When questionnaireId(IIdType id) {
            questionnaireId = id;
            return this;
        }

        public When subjectId(String id) {
            subjectId = id;
            return this;
        }

        public When context(List<IBaseBackboneElement> context) {
            this.context = context;
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public IBaseResource runPopulate() {
            return processor.populate(
                    Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
                    subjectId,
                    context,
                    launchContext,
                    parameters,
                    data,
                    useServerData,
                    (IBaseResource) null,
                    null,
                    null);
        }
    }
}
