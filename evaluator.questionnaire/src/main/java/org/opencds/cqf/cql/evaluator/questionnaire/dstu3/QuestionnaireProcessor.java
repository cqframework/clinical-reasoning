package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.cql.evaluator.questionnaire.dstu3.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.helper.dstu3.PackageHelper;
import org.opencds.cqf.cql.evaluator.library.CqfExpression;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.api.Repository;

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
  public <C extends IPrimitiveType<String>> Questionnaire resolveQuestionnaire(IIdType theId,
      C theCanonical, IBaseResource theQuestionnaire) {
    var baseQuestionnaire = theQuestionnaire;
    if (baseQuestionnaire == null) {
      baseQuestionnaire = theId != null ? this.repository.read(Questionnaire.class, theId)
          : (Questionnaire) searchRepositoryByCanonical(repository, theCanonical);
    }

    return castOrThrow(baseQuestionnaire, Questionnaire.class,
        "The Questionnaire passed to repository was not a valid instance of Questionnaire.class")
            .orElse(null);
  }

  @Override
  public Questionnaire prePopulate(Questionnaire questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
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
        ? ((UriType) questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY).getValue())
            .getValue()
        : null;
    populatedQuestionnaire = questionnaire.copy();

    populatedQuestionnaire.setId(questionnaire.getIdPart() + "-" + patientId);
    populatedQuestionnaire.addExtension(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT,
        new Reference(FHIRAllTypes.PATIENT.toCode() + "/" + patientId));

    oc = new OperationOutcome();
    oc.setId("populate-outcome-" + populatedQuestionnaire.getIdPart());

    populatedQuestionnaire.setItem(processItems(questionnaire.getItem()));

    if (!oc.getIssue().isEmpty()) {
      populatedQuestionnaire.addContained(oc);
      populatedQuestionnaire.addExtension(Constants.EXT_CRMI_MESSAGES,
          new Reference("#" + oc.getIdPart()));
    }

    return populatedQuestionnaire;
  }

  private List<IBase> getExpressionResult(CqfExpression expression, String itemLinkId,
      IBase populationContext) {
    try {
      var subjectId = patientId;
      var expressionSubjectType = subjectType;
      if (populationContext != null && !populationContext.isEmpty()) {
        subjectId = ((Resource) populationContext).getIdPart();
        expressionSubjectType = ((Resource) populationContext).fhirType();
      }
      return libraryEngine.resolveExpression(subjectId, expressionSubjectType, expression,
          parameters, bundle);
    } catch (Exception ex) {
      var message =
          String.format(
              "Error encountered evaluating expression (%s) for item (%s): %s",
              expression.getExpression(), itemLinkId, ex.getMessage());
      logger.error(message);
      oc.addIssue().setCode(OperationOutcome.IssueType.EXCEPTION)
          .setSeverity(OperationOutcome.IssueSeverity.ERROR).setDiagnostics(message);
    }

    return null;

  }

  private CqfExpression getInitialExpression(QuestionnaireItemComponent item) {
    Extension expressionExtension = null;
    if (item.hasExtension(Constants.CQF_EXPRESSION)) {
      expressionExtension = item.getExtensionByUrl(Constants.CQF_EXPRESSION);
    } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
      expressionExtension = item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);;
    }

    if (expressionExtension == null) {
      return null;
    }

    var languageExtension = getExtensionByUrl(item, Constants.CQF_EXPRESSION_LANGUAGE);

    return new CqfExpression(languageExtension.getValue().toString(),
        expressionExtension.getValue().toString(), libraryUrl);
  }

  private void getInitial(QuestionnaireItemComponent item, IBase populationContext) {
    var initialExpression = getInitialExpression(item);
    if (initialExpression != null) {
      // evaluate expression and set the result as the initialAnswer on the item
      var results = getExpressionResult(initialExpression, item.getLinkId(), populationContext);

      // TODO: what to do with choice answerOptions of type valueCoding with an
      // expression that returns a valueString

      if (results != null && !results.isEmpty()) {
        for (var result : results) {
          if (result != null) {
            item.addExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR,
                new Reference(Constants.CQL_ENGINE_DEVICE));
            item.setInitial(transformValue((Type) result));
          }
        }
      }
    }
  }

  private CqfExpression getContextExpression(QuestionnaireItemComponent item) {
    var contextExpressionExt =
        item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
    if (contextExpressionExt == null) {
      return null;
    }

    var languageExtension = getExtensionByUrl(item, Constants.CQF_EXPRESSION_LANGUAGE);

    return new CqfExpression(languageExtension.getValue().toString(),
        contextExpressionExt.getValue().toString(), libraryUrl);
  }

  protected List<QuestionnaireItemComponent> processItemWithContext(
      QuestionnaireItemComponent groupItem) {
    List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
    var populationContext =
        getExpressionResult(getContextExpression(groupItem), groupItem.getLinkId(), null);
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
            item.addExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR,
                new Reference(Constants.CQL_ENGINE_DEVICE));
            item.setInitial(transformValue((Type) initialProperty.getValues().get(0)));
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
          getInitial(populatedItem, null);
        }
        populatedItems.add(populatedItem);
      }
    });

    return populatedItems;
  }

  @Override
  public IBaseResource populate(Questionnaire questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
    prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
    var response = new QuestionnaireResponse();
    response.setId(populatedQuestionnaire.getIdPart() + "-response");
    if (populatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES)
        && !oc.getIssue().isEmpty()) {
      response.addContained(oc);
      response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
    }
    response.addContained(populatedQuestionnaire);
    response.addExtension(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE,
        new Reference("#" + populatedQuestionnaire.getIdPart()));
    response.setQuestionnaire(new Reference(questionnaire.getUrl()));
    response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
    response.setSubject(new Reference(new IdType("Patient", patientId)));
    var responseItems = new ArrayList<QuestionnaireResponseItemComponent>();
    processResponseItems(populatedQuestionnaire.getItem(), responseItems);
    response.setItem(responseItems);

    return response;
  }

  protected void processResponseItems(List<QuestionnaireItemComponent> items,
      List<QuestionnaireResponseItemComponent> responseItems) {
    items.forEach(item -> {
      var responseItem =
          new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkIdElement());
      responseItem.setDefinition(item.getDefinition());
      responseItem.setTextElement(item.getTextElement());
      if (item.hasItem()) {
        var nestedResponseItems = new ArrayList<QuestionnaireResponseItemComponent>();
        processResponseItems(item.getItem(), nestedResponseItems);
        responseItem.setItem(nestedResponseItems);
      } else if (item.hasInitial()) {
        if (item.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
          responseItem
              .addExtension(item.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        }
        var answer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
            .setValue(item.getInitial());
        responseItem.addAnswer(answer);
      }
      responseItems.add(responseItem);
    });
  }

  @Override
  public Questionnaire generateQuestionnaire(String theId) {

    var questionnaire = new Questionnaire();
    questionnaire.setId(new IdType("Questionnaire", theId));

    return questionnaire;
  }

  @Override
  public Bundle packageQuestionnaire(Questionnaire theQuestionnaire, boolean theIsPut) {
    var bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);
    bundle.addEntry(PackageHelper.createEntry(theQuestionnaire, theIsPut));
    var libraryExtension = theQuestionnaire.getExtensionByUrl(Constants.CQF_LIBRARY);
    if (libraryExtension != null) {
      var libraryCanonical = (UriType) libraryExtension.getValue();
      var library = (Library) searchRepositoryByCanonical(repository, libraryCanonical);
      if (library != null) {
        bundle.addEntry(PackageHelper.createEntry(library, theIsPut));
        if (library.hasRelatedArtifact()) {
          PackageHelper.addRelatedArtifacts(bundle, library.getRelatedArtifact(), repository,
              theIsPut);
        }
      }
    }

    return bundle;
  }
}
