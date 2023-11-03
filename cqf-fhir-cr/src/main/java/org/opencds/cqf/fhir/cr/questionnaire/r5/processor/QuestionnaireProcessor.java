package org.opencds.cqf.fhir.cr.questionnaire.r5.processor;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.*;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Questionnaire;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r5.processor.prepopulate.PrePopulateProcessor;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    private PopulateProcessor populateProcessor;
    private ResolveProcessor resolveProcessor;
    private PackageProcessor packageProcessor;
    private PrePopulateProcessor prePopulateProcessor;
    private GenerateProcessor generateProcessor;

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(
                repository,
                evaluationSettings,
                new ResolveProcessor(repository),
                new PackageProcessor(repository),
                new PopulateProcessor(),
                new PrePopulateProcessor(),
                new GenerateProcessor());
    }

    public QuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    QuestionnaireProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            ResolveProcessor resolveProcessor,
            PackageProcessor packageProcessor,
            PopulateProcessor populateProcessor,
            PrePopulateProcessor prePopulateProcessor,
            GenerateProcessor generateProcessor) {
        super(repository, evaluationSettings);
        this.resolveProcessor = resolveProcessor;
        this.packageProcessor = packageProcessor;
        this.populateProcessor = populateProcessor;
        this.prePopulateProcessor = prePopulateProcessor;
        this.generateProcessor = generateProcessor;
    }

    @Override
    public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
            IIdType id, C canonical, IBaseResource questionnaire) {
        return resolveProcessor.resolve(id, canonical, questionnaire);
    }

    @Override
    public Questionnaire prePopulate(
            Questionnaire questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        requireNonNull(questionnaire);
        requireNonNull(libraryEngine);
        final PrePopulateRequest prePopulateRequest =
                new PrePopulateRequest(patientId, parameters, bundle, libraryEngine);
        return prePopulateProcessor.prePopulate(questionnaire, prePopulateRequest);
    }

    @Override
    public IBaseResource populate(
            Questionnaire questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        final Questionnaire prePopulatedQuestionnaire =
                prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        return populateProcessor.populate(questionnaire, prePopulatedQuestionnaire, patientId);
    }

    @Override
    public Questionnaire generateQuestionnaire(String id) {
        return generateProcessor.generate(id);
    }

    @Override
    public Bundle packageQuestionnaire(Questionnaire questionnaire, boolean isPut) {
        return packageProcessor.packageQuestionnaire(questionnaire, isPut);
    }
}
