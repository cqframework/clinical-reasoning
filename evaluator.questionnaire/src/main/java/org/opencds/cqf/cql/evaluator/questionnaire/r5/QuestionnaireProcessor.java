package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import static org.opencds.cqf.cql.evaluator.fhir.util.r5.SearchHelper.searchRepositoryByCanonical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;
import org.opencds.cqf.fhir.api.Repository;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
  public QuestionnaireProcessor(Repository repository) {
    super(repository);
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


    var libraryUrl =
        (CanonicalType) questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY).getValue();
    var oc = new OperationOutcome();
    oc.setId("prepopulate-outcome-" + questionnaire.getIdPart());

    processItems(questionnaire.getItem(), libraryUrl.getValue(), oc);

    if (!oc.getIssue().isEmpty()) {
      questionnaire.addContained(oc);
      questionnaire.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
    }

    return questionnaire;
  }

  protected void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary,
      OperationOutcome oc) {
    items.forEach(item -> {
      if (item.hasItem()) {
        processItems(item.getItem(), defaultLibrary, oc);
      } else {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
          // evaluate expression and set the result as the initialAnswer on the item
          var expression = (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
          var libraryUrl = expression.hasReference() ? expression.getReference() : defaultLibrary;
          try {
            var result = this.libraryEngine.getExpressionResult(this.patientId, "Patient",
                expression.getExpression(), expression.getLanguage(), libraryUrl, this.parameters,
                this.bundle);
            item.addInitial(
                new Questionnaire.QuestionnaireItemInitialComponent().setValue((DataType) result));
          } catch (Exception ex) {
            var message =
                String.format("Error encountered evaluating expression (%s) for item (%s): %s",
                    expression.getExpression(), item.getLinkId(), ex.getMessage());
            logger.error(message);
            oc.addIssue().setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR).setDiagnostics(message);
          }
        }
      }
    });
  }

  @Override
  public IBaseResource populate(Questionnaire questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
    var populatedQuestionnaire =
        prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
    var response = new QuestionnaireResponse();
    response.setId(populatedQuestionnaire.getIdPart() + "-response");
    if (questionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES)) {
      var ocExt = questionnaire.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES);
      var ocId = ((Reference) ocExt.getValue()).getReference().replaceFirst("#", "");
      var ocList = questionnaire.getContained().stream()
          .filter(resource -> resource.getIdPart().equals(ocId)).collect(Collectors.toList());
      var oc = ocList == null || ocList.isEmpty() ? null : ocList.get(0);
      if (oc != null) {
        oc.setId("populate-outcome-" + populatedQuestionnaire.getIdPart());
        response.addContained(oc);
        response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
      }
    }
    response.setQuestionnaire(populatedQuestionnaire.getUrl());
    response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
    response.setSubject(new Reference(new IdType("Patient", patientId)));
    var responseItems = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
    processResponseItems(populatedQuestionnaire.getItem(), responseItems);
    response.setItem(responseItems);

    return response;
  }

  protected void processResponseItems(List<QuestionnaireItemComponent> items,
      List<QuestionnaireResponseItemComponent> responseItems) {
    items.forEach(item -> {
      var responseItem =
          new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkId());
      responseItem.setDefinition(item.getDefinition());
      responseItem.setTextElement(item.getTextElement());
      if (item.hasItem()) {
        var nestedResponseItems =
            new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
        processResponseItems(item.getItem(), nestedResponseItems);
        responseItem.setItem(nestedResponseItems);
      } else if (item.hasInitial()) {
        item.getInitial()
            .forEach(answer -> responseItem
                .addAnswer(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                    .setValue(answer.getValue())));
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

  private static List<String> packableResources = Arrays.asList(FHIRTypes.LIBRARY.toCode(),
      FHIRTypes.CODESYSTEM.toCode(), FHIRTypes.VALUESET.toCode());

  private void addRelatedArtifacts(Bundle theBundle, List<RelatedArtifact> theArtifacts) {
    for (var artifact : theArtifacts) {
      if (artifact.getType().equals(RelatedArtifactType.DEPENDSON)
          && artifact.hasResourceElement()) {
        var resource = searchRepositoryByCanonical(repository, artifact.getResourceElement());
        if (resource != null && packableResources.contains(resource.fhirType())
            && theBundle.getEntry().stream()
                .noneMatch(e -> e.getResource().getIdElement().equals(resource.getIdElement()))) {
          theBundle.addEntry(new BundleEntryComponent().setResource(resource));
          if (resource.fhirType().equals(FHIRTypes.LIBRARY.toCode())
              && ((Library) resource).hasRelatedArtifact()) {
            addRelatedArtifacts(theBundle, ((Library) resource).getRelatedArtifact());
          }
        }
      }
    }
  }

  @Override
  public Bundle packageQuestionnaire(Questionnaire theQuestionnaire) {
    var bundle = new Bundle();
    bundle.setType(BundleType.COLLECTION);
    bundle.addEntry(new BundleEntryComponent().setResource(theQuestionnaire));
    var libraryExtension = theQuestionnaire.getExtensionByUrl(Constants.CQF_LIBRARY);
    if (libraryExtension != null) {
      var libraryCanonical = (CanonicalType) libraryExtension.getValue();
      var library = (Library) searchRepositoryByCanonical(repository, libraryCanonical);
      if (library != null) {
        bundle.addEntry(new BundleEntryComponent().setResource(library));
        if (library.hasRelatedArtifact()) {
          addRelatedArtifacts(bundle, library.getRelatedArtifact());
        }
      }
    }

    return bundle;
  }
}
