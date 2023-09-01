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

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    protected OperationOutcome oc;
    protected Questionnaire populatedQuestionnaire;

    public QuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
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
            LibraryEngine libraryEngine) {
        if (questionnaire == null) {
            throw new IllegalArgumentException("No questionnaire passed in");
        }
        if (libraryEngine == null) {
            throw new IllegalArgumentException("No engine passed in");
        }
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        libraryUrl = questionnaire.hasExtension(Constants.CQF_LIBRARY)
                ? ((CanonicalType) questionnaire
                                .getExtensionByUrl(Constants.CQF_LIBRARY)
                                .getValue())
                        .getValue()
                : null;
        populatedQuestionnaire = questionnaire.copy();

        populatedQuestionnaire.setId(questionnaire.getIdPart() + "-" + patientId);
        populatedQuestionnaire.addExtension(
                Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT,
                new Reference(FHIRAllTypes.PATIENT.toCode() + "/" + patientId));

        oc = new OperationOutcome();
        oc.setId("populate-outcome-" + populatedQuestionnaire.getIdPart());

        populatedQuestionnaire.setItem(processItems(questionnaire.getItem()));

        if (!oc.getIssue().isEmpty()) {
            populatedQuestionnaire.addContained(oc);
            populatedQuestionnaire.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
        }

        return populatedQuestionnaire;
    }

    private List<IBase> getExpressionResult(Expression expression, String itemLinkId) {
        if (expression == null || !expression.hasExpression()) {
            return null;
        }
        try {
            return libraryEngine.resolveExpression(
                    patientId, new CqfExpression(expression, libraryUrl, null), parameters, bundle);
        } catch (Exception ex) {
            var message = String.format(
                    "Error encountered evaluating expression (%s) for item (%s): %s",
                    expression.getExpression(), itemLinkId, ex.getMessage());
            logger.error(message);
            oc.addIssue()
                    .setCode(OperationOutcome.IssueType.EXCEPTION)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics(message);
        }

        return null;
    }

    private Expression getInitialExpression(QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)
                    .getValue();
        }

        return null;
    }

    private void getInitial(QuestionnaireItemComponent item) {
        var initialExpression = getInitialExpression(item);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            var results = getExpressionResult(initialExpression, item.getLinkId());

            // TODO: what to do with choice answerOptions of type valueCoding with an
            // expression that returns a valueString

            if (results != null && !results.isEmpty()) {
                for (var result : results) {
                    if (result != null) {
                        item.addExtension(
                                Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new Reference(Constants.CQL_ENGINE_DEVICE));
                        item.addInitial(new Questionnaire.QuestionnaireItemInitialComponent()
                                .setValue(transformValue((Type) result)));
                    }
                }
            }
        }
    }

    protected List<QuestionnaireItemComponent> processItemWithContext(QuestionnaireItemComponent groupItem) {
        List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        var contextExpression = (Expression) groupItem
                .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)
                .getValue();
        var populationContext = getExpressionResult(contextExpression, groupItem.getLinkId());
        if (populationContext == null || populationContext.isEmpty()) {
            return Collections.singletonList(groupItem.copy());
        }
        for (var context : populationContext) {
            var contextItem = groupItem.copy();
            for (var item : contextItem.getItem()) {
                var path = item.getDefinition().split("#")[1].split("\\.")[1];
                var initialProperty = ((Base) context).getNamedProperty(path);
                if (initialProperty.hasValues()) {
                    if (initialProperty.isList()) {
                        // TODO: handle lists
                    } else {
                        item.addExtension(
                                Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new Reference(Constants.CQL_ENGINE_DEVICE));
                        item.addInitial(
                                new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type)
                                        initialProperty.getValues().get(0))));
                    }
                }
            }
            populatedItems.add(contextItem);
        }

        return populatedItems;
    }

    protected List<QuestionnaireItemComponent> processItems(List<QuestionnaireItemComponent> items) {
        List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        items.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                populatedItems.addAll(processItemWithContext(item));
            } else {
                var populatedItem = item.copy();
                if (item.hasItem()) {
                    populatedItem.setItem(processItems(item.getItem()));
                } else {
                    getInitial(populatedItem);
                }
                populatedItems.add(populatedItem);
            }
        });

        return populatedItems;
    }

    @Override
    public IBaseResource populate(
            Questionnaire questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        var response = new QuestionnaireResponse();
        response.setId(populatedQuestionnaire.getIdPart() + "-response");
        if (populatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES)
                && !oc.getIssue().isEmpty()) {
            response.addContained(oc);
            response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
        }
        response.addContained(populatedQuestionnaire);
        response.addExtension(
                Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE,
                new Reference("#" + populatedQuestionnaire.getIdPart()));
        response.setQuestionnaire(questionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        var responseItems = new ArrayList<QuestionnaireResponseItemComponent>();
        processResponseItems(populatedQuestionnaire.getItem(), responseItems);
        response.setItem(responseItems);

        return response;
    }

    protected void processResponseItems(
            List<QuestionnaireItemComponent> items, List<QuestionnaireResponseItemComponent> responseItems) {
        items.forEach(item -> {
            var responseItem = new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkIdElement());
            responseItem.setDefinition(item.getDefinition());
            responseItem.setTextElement(item.getTextElement());
            if (item.hasItem()) {
                var nestedResponseItems = new ArrayList<QuestionnaireResponseItemComponent>();
                processResponseItems(item.getItem(), nestedResponseItems);
                responseItem.setItem(nestedResponseItems);
            } else if (item.hasInitial()) {
                if (item.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
                    responseItem.addExtension(item.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
                }
                item.getInitial().forEach(initial -> {
                    var answer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                            .setValue(initial.getValue());
                    responseItem.addAnswer(answer);
                });
            }
            responseItems.add(responseItem);
        });
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
