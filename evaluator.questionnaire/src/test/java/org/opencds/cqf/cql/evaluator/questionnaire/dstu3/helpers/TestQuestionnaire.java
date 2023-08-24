package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.QuestionnaireProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaire {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
  private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

  private static InputStream open(String asset) {
    return TestQuestionnaire.class.getResourceAsStream(asset);
  }

  public static String load(InputStream asset) throws IOException {
    return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
  }

  public static String load(String asset) throws IOException {
    return load(open(asset));
  }

  public static IBaseResource parse(String asset) {
    return jsonParser.parseResource(open(asset));
  }

  public static QuestionnaireProcessor buildProcessor(Repository repository) {
    return new QuestionnaireProcessor(repository, evaluationSettings);
  }

  /** Fluent interface starts here **/

  public static class Assert {
    public static QuestionnaireResult that(String questionnaireName, String patientId) {
      return new QuestionnaireResult(questionnaireName, patientId);
    }
  }

  static class QuestionnaireResult {
    private Questionnaire questionnaire;
    private String patientId;
    private Repository repository;
    private Repository dataRepository;
    private Repository contentRepository;
    private Repository terminologyRepository;
    private Bundle bundle;
    private Parameters parameters;

    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

    public QuestionnaireResult(String questionnaireName, String patientId) {
      questionnaire = questionnaireName.isEmpty() ? null : (Questionnaire) parse(questionnaireName);
      this.patientId = patientId;
    }

    public QuestionnaireResult withData(String dataAssetName) {
      dataRepository = new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withContent(String dataAssetName) {
      contentRepository = new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyRepository =
          new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withAdditionalData(String dataAssetName) {
      var data = parse(dataAssetName);
      bundle =
          data.getIdElement().getResourceType().equals(FHIRAllTypes.BUNDLE.toCode()) ? (Bundle) data
              : new Bundle().setType(BundleType.COLLECTION)
                  .addEntry(new BundleEntryComponent().setResource((Resource) data));

      return this;
    }

    public QuestionnaireResult withParameters(Parameters params) {
      parameters = params;

      return this;
    }

    public QuestionnaireResult withRepository(Repository repository) {
      this.repository = repository;

      return this;
    }

    private void buildRepository() {
      if (repository != null) {
        return;
      }
      if (dataRepository == null) {
        dataRepository =
            new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("tests"), false);
      }
      if (contentRepository == null) {
        contentRepository =
            new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("resources/"), false);
      }
      if (terminologyRepository == null) {
        terminologyRepository = new InMemoryFhirRepository(fhirContext, this.getClass(),
            List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);
      }

      repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
    }

    public GeneratedQuestionnaire prePopulate() {
      buildRepository();
      var libraryEngine = new LibraryEngine(repository, evaluationSettings);
      return new GeneratedQuestionnaire(buildProcessor(this.repository).prePopulate(questionnaire,
          patientId, parameters, bundle, libraryEngine));
    }

    public GeneratedQuestionnaireResponse populate() {
      buildRepository();
      var libraryEngine = new LibraryEngine(repository, evaluationSettings);
      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor(this.repository).populate(questionnaire, patientId,
              parameters, bundle, libraryEngine));
    }

    public Bundle questionnairePackage() {
      buildRepository();
      return buildProcessor(repository).packageQuestionnaire(questionnaire, true);
    }
  }

  static class GeneratedQuestionnaire {
    Questionnaire questionnaire;

    public GeneratedQuestionnaire(Questionnaire questionnaire) {
      this.questionnaire = questionnaire;
    }

    public void isEqualsTo(String expectedQuestionnaireAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedQuestionnaireAssetName),
            jsonParser.encodeResourceToString(questionnaire), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public GeneratedQuestionnaire hasErrors() {
      assertTrue(questionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES));
      assertTrue(questionnaire.hasContained());
      assertTrue(questionnaire.getContained().stream()
          .anyMatch(r -> r.getResourceType().equals(ResourceType.OperationOutcome)));

      return this;
    }
  }

  static class GeneratedQuestionnaireResponse {
    QuestionnaireResponse questionnaireResponse;

    public GeneratedQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
      this.questionnaireResponse = questionnaireResponse;
    }

    public void isEqualsTo(String expectedQuestionnaireResponseAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedQuestionnaireResponseAssetName),
            jsonParser.encodeResourceToString(questionnaireResponse), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public GeneratedQuestionnaireResponse hasErrors() {
      assertTrue(questionnaireResponse.hasExtension(Constants.EXT_CRMI_MESSAGES));
      assertTrue(questionnaireResponse.hasContained());
      assertTrue(questionnaireResponse.getContained().stream()
          .anyMatch(r -> r.getResourceType().equals(ResourceType.OperationOutcome)));

      return this;
    }
  }
}
