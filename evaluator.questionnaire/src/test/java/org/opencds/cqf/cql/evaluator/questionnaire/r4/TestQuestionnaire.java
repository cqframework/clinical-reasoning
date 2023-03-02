package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
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
    return new QuestionnaireProcessor(fhirContext, repository);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static QuestionnaireResult that(String questionnaireName, String patientId) {
      return new QuestionnaireResult(questionnaireName, patientId);
    }
  }

  static class QuestionnaireResult {
    private Bundle bundle;
    private Endpoint dataEndpoint;
    private Endpoint contentEndpoint;
    private Endpoint terminologyEndpoint;
    private Parameters parameters;
    private Questionnaire baseResource;
    private String patientId;
    private Repository repository;

    public QuestionnaireResult(String questionnaireName, String patientId) {
      baseResource = questionnaireName.isEmpty() ? null : (Questionnaire) parse(questionnaireName);
      this.patientId = patientId;

      FhirRepository data = new FhirRepository(this.getClass(), List.of("res/tests"), false);
      FhirRepository content = new FhirRepository(this.getClass(), List.of("res/content/"), false);
      FhirRepository terminology = new FhirRepository(this.getClass(),
          List.of("res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/"), false);

      this.repository = Repositories.proxy(data, content, terminology);
    }

    public QuestionnaireResult withData(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public QuestionnaireResult withLibrary(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public QuestionnaireResult withBundle(String dataAssetName) {
      bundle = (Bundle) parse(dataAssetName);
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

    public GeneratedQuestionnaire prePopulate() {
      return new GeneratedQuestionnaire(buildProcessor(this.repository).prePopulate(baseResource,
          patientId, parameters, bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
    }

    public GeneratedQuestionnaire prePopulateWithEngine() {
      var libraryEngine = new LibraryEngine(fhirContext, this.repository);

      return new GeneratedQuestionnaire(buildProcessor(this.repository).prePopulate(baseResource,
          patientId, parameters, bundle, libraryEngine));
    }

    public GeneratedQuestionnaireResponse populate() {
      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor(this.repository).populate(baseResource, patientId,
              parameters, bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
    }

    public GeneratedQuestionnaireResponse populateWithEngine() {
      var libraryEngine = new LibraryEngine(fhirContext, this.repository);

      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor(this.repository).populate(baseResource, patientId,
              parameters, bundle, libraryEngine));
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
