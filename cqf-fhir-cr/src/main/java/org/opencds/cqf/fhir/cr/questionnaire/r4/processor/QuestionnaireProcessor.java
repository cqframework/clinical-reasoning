package org.opencds.cqf.fhir.cr.questionnaire.r4.processor;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.packager.PackageService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate.PopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.resolve.ResolveService;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    protected PopulateService populateService;
    protected ResolveService resolveService;
    protected PackageService packageService;
    protected PrePopulateService prePopulateService;

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository,
            evaluationSettings,
            new ResolveService(repository),
            new PackageService(repository),
            new PopulateService(),
            new PrePopulateService()
        );
    }

    public QuestionnaireProcessor(Repository repository) {
       this(repository, EvaluationSettings.getDefault());
    }

    private QuestionnaireProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            ResolveService resolveService,
            PackageService packageService,
            PopulateService populateService,
            PrePopulateService prePopulateService
    ) {
        super(repository, evaluationSettings);
        this.resolveService = resolveService;
        this.packageService = packageService;
        this.populateService = populateService;
        this.prePopulateService = prePopulateService;
    }

    @Override
    public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
            IIdType id, C canonical, IBaseResource questionnaire) {
        return resolveService.resolve(id, canonical, questionnaire);
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
                new PrePopulateRequest(questionnaire, patientId, parameters, bundle, libraryEngine);
        return prePopulateService.prePopulate(prePopulateRequest);
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
        return populateService.populate(questionnaire, prePopulatedQuestionnaire, patientId);
    }

    @Override
    public Questionnaire generateQuestionnaire(String id) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", id));
        return questionnaire;
    }

    @Override
    public Bundle packageQuestionnaire(Questionnaire questionnaire, boolean isPut) {
        return packageService.packageQuestionnaire(questionnaire, isPut);
    }
}
