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

public class NewQuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    protected PopulateService myPopulateService;
    protected ResolveService myResolveService;
    protected PackageService myPackageService;
    protected PrePopulateService myPrePopulateService;

    public static NewQuestionnaireProcessor of(Repository theRepository, EvaluationSettings theEvaluationSettings) {
        final ResolveService resolveService = ResolveService.of(theRepository);
        final PackageService packageService = PackageService.of(theRepository);
        final PopulateService populateService = PopulateService.of();
        final PrePopulateService prePopulateService = PrePopulateService.of();
        return new NewQuestionnaireProcessor(
                theRepository,
                theEvaluationSettings,
                resolveService,
                packageService,
                populateService,
                prePopulateService);
    }

    public static NewQuestionnaireProcessor of(Repository repository) {
        return of(repository, EvaluationSettings.getDefault());
    }

    private NewQuestionnaireProcessor(
            Repository theRepository,
            EvaluationSettings theEvaluationSettings,
            ResolveService theResolveService,
            PackageService thePackageService,
            PopulateService thePopulateService,
            PrePopulateService thePrePopulateService) {
        super(theRepository, theEvaluationSettings);
        myResolveService = theResolveService;
        myPackageService = thePackageService;
        myPopulateService = thePopulateService;
        myPrePopulateService = thePrePopulateService;
    }

    @Override
    public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
            IIdType theId, C theCanonical, IBaseResource theQuestionnaire) {
        return myResolveService.resolve(theId, theCanonical, theQuestionnaire);
    }

    @Override
    public Questionnaire prePopulate(
            Questionnaire theQuestionnaire,
            String thePatientId,
            IBaseParameters theParameters,
            IBaseBundle theBundle,
            LibraryEngine theLibraryEngine) {
        requireNonNull(theQuestionnaire);
        requireNonNull(theLibraryEngine);
        final PrePopulateRequest prePopulateRequest =
                new PrePopulateRequest(theQuestionnaire, thePatientId, theParameters, theBundle, theLibraryEngine);
        return myPrePopulateService.prePopulate(prePopulateRequest);
    }

    @Override
    public IBaseResource populate(
            Questionnaire theQuestionnaire,
            String thePatientId,
            IBaseParameters theParameters,
            IBaseBundle theBundle,
            LibraryEngine theLibraryEngine) {
        final Questionnaire prePopulatedQuestionnaire =
                prePopulate(theQuestionnaire, thePatientId, theParameters, theBundle, theLibraryEngine);
        return myPopulateService.populate(theQuestionnaire, prePopulatedQuestionnaire, thePatientId);
    }

    @Override
    public Questionnaire generateQuestionnaire(String theId) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", theId));
        return questionnaire;
    }

    @Override
    public Bundle packageQuestionnaire(Questionnaire theQuestionnaire, boolean isPut) {
        return myPackageService.packageQuestionnaire(theQuestionnaire, isPut);
    }
}
