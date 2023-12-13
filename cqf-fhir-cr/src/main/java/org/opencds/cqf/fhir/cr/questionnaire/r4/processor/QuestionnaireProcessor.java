package org.opencds.cqf.fhir.cr.questionnaire.r4.processor;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateProcessor;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    private ResourceResolver resourceResolver;
    private PopulateProcessor populateProcessor;
    private PackageProcessor packageProcessor;
    private PrePopulateProcessor prePopulateProcessor;
    private GenerateProcessor generateProcessor;

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(
                repository,
                evaluationSettings,
                new ResourceResolver("Questionnaire", repository),
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
            ResourceResolver resourceResolver,
            PackageProcessor packageProcessor,
            PopulateProcessor populateProcessor,
            PrePopulateProcessor prePopulateProcessor,
            GenerateProcessor generateProcessor) {
        super(repository, evaluationSettings);
        this.resourceResolver = resourceResolver;
        this.packageProcessor = packageProcessor;
        this.populateProcessor = populateProcessor;
        this.prePopulateProcessor = prePopulateProcessor;
        this.generateProcessor = generateProcessor;
    }

    @Override
    public <CanonicalType extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
            IIdType id, CanonicalType canonical, IBaseResource questionnaire) {
        return resolveQuestionnaire(Eithers.for3(canonical, id, questionnaire));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> Questionnaire resolveQuestionnaire(
            Either3<C, IIdType, R> questionnaire) {
        return (Questionnaire) resourceResolver.resolve(questionnaire);
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
