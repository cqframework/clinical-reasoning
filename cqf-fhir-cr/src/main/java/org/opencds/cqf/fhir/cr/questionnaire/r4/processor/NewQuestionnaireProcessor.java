package org.opencds.cqf.fhir.cr.questionnaire.r4.processor;

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

public class NewQuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    protected PopulateService myPopulateService;
    protected ResolveService myResolveService;
    protected PackageService myPackageService;
    protected PrePopulateService myPrePopulateService;

    public NewQuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public NewQuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository, evaluationSettings);
        myResolveService = new ResolveService(repository);
        myPackageService = new PackageService(repository);
        myPopulateService = new PopulateService();
        myPrePopulateService = new PrePopulateService();
    }

    @Override
    public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
        IIdType id,
        C canonical,
        IBaseResource questionnaire
    ) {
        return myResolveService.resolve(id, canonical, questionnaire);
    }

    @Override
    public Questionnaire prePopulate(
        Questionnaire questionnaire,
        String patientId,
        IBaseParameters parameters,
        IBaseBundle bundle,
        LibraryEngine libraryEngine
    ) {
        if (questionnaire == null) {
            throw new IllegalArgumentException("No questionnaire passed in");
        }
        if (libraryEngine == null) {
            throw new IllegalArgumentException("No engine passed in");
        }
        final PrePopulateRequest prePopulateRequest = new PrePopulateRequest(questionnaire, patientId, parameters, bundle, libraryEngine);
        return myPrePopulateService.prePopulate(prePopulateRequest);
    }

    @Override
    public IBaseResource populate(
        Questionnaire questionnaire,
        String patientId,
        IBaseParameters parameters,
        IBaseBundle bundle,
        LibraryEngine libraryEngine
    ) {
        final Questionnaire prePopulatedQuestionnaire = prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        return myPopulateService.populate(questionnaire, prePopulatedQuestionnaire, oc, patientId);
    }

    @Override
    public Questionnaire generateQuestionnaire(String id) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", id));
        return questionnaire;
    }

    @Override
    public Bundle packageQuestionnaire(Questionnaire questionnaire, boolean isPut) {
        return myPackageService.packageQuestionnaire(questionnaire, isPut);
    }
}
