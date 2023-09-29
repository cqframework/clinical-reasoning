package org.opencds.cqf.fhir.cr.questionnaire.r4;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;
import org.opencds.cqf.fhir.utility.r4.SearchHelper;

public class NewQuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    protected OperationOutcome oc;
    protected Questionnaire populatedQuestionnaire;
    protected PopulateService myPopulateService;

    public NewQuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public NewQuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository, evaluationSettings);
    }

    @Override
    public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(
        IIdType id, C canonical, IBaseResource questionnaire) {
        var baseQuestionnaire = questionnaire;
        if (baseQuestionnaire == null) {
            baseQuestionnaire = id != null
                ? this.repository.read(Questionnaire.class, id)
                : (Questionnaire) SearchHelper.searchRepositoryByCanonical(repository, canonical);
        }

        return castOrThrow(
            baseQuestionnaire,
            Questionnaire.class,
            "The Questionnaire passed to repository was not a valid instance of Questionnaire.class")
            .orElse(null);
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
        final ExpressionProcessorService expressionProcessorService = new ExpressionProcessorService(questionnaire, patientId, parameters, bundle, libraryEngine);
        final PrePopulateService prePopulateService = new PrePopulateService(questionnaire, patientId, expressionProcessorService);
        return PrePopulateService.prepopulate();
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
        var bundle = new Bundle();
        bundle.setType(BundleType.TRANSACTION);
        bundle.addEntry(PackageHelper.createEntry(questionnaire, isPut));
        var libraryExtension = questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (CanonicalType) libraryExtension.getValue();
            var library = (Library) SearchHelper.searchRepositoryByCanonical(repository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    PackageHelper.addRelatedArtifacts(bundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        return bundle;
    }
}
