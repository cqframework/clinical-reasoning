package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaire {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

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
    return new QuestionnaireProcessor(repository);
  }

  /** Fluent interface starts here **/

  static class Assert {
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

    public QuestionnaireResult(String questionnaireName, String patientId) {
      questionnaire = questionnaireName.isEmpty() ? null : (Questionnaire) parse(questionnaireName);
      this.patientId = patientId;
    }

    public QuestionnaireResult withData(String dataAssetName) {
      dataRepository = new FhirRepository((Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withContent(String dataAssetName) {
      contentRepository = new FhirRepository((Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyRepository = new FhirRepository((Bundle) parse(dataAssetName));

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
        dataRepository = new FhirRepository(this.getClass(), List.of("tests"), false);
      }
      if (contentRepository == null) {
        contentRepository = new FhirRepository(this.getClass(), List.of("content/"), false);
      }
      if (terminologyRepository == null) {
        terminologyRepository = new FhirRepository(this.getClass(),
            List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);
      }

      repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
    }

    public GeneratedQuestionnaire prePopulate() {
      buildRepository();
      var libraryEngine = new LibraryEngine(repository);
      return new GeneratedQuestionnaire(buildProcessor(this.repository).prePopulate(questionnaire,
          patientId, parameters, bundle, libraryEngine));
    }

    public GeneratedQuestionnaireResponse populate() {
      buildRepository();
      var libraryEngine = new LibraryEngine(repository);
      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor(this.repository).populate(questionnaire, patientId,
              parameters, bundle, libraryEngine));
    }

    public Bundle questionnairePackage() {
      buildRepository();
      return buildProcessor(repository).packageQuestionnaire(questionnaire);
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
  }
}
