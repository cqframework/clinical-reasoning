package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaire {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R5);
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

  public static QuestionnaireProcessor buildProcessor() {
    return new QuestionnaireProcessor(fhirContext);
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

    public QuestionnaireResult(String questionnaireName, String patientId) {
      baseResource = questionnaireName.isEmpty() ? null : (Questionnaire) parse(questionnaireName);
      this.patientId = patientId;
    }

    public QuestionnaireResult withData(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(Collections.singletonList(new CodeableConcept().setCoding(
              Collections.singletonList(new Coding().setCode(Constants.HL7_FHIR_FILES)))));

      // fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public QuestionnaireResult withLibrary(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(Collections.singletonList(new CodeableConcept().setCoding(
              Collections.singletonList(new Coding().setCode(Constants.HL7_FHIR_FILES)))));

      // fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(Collections.singletonList(new CodeableConcept().setCoding(
              Collections.singletonList(new Coding().setCode(Constants.HL7_FHIR_FILES)))));

      // fhirDal.addAll(parse(dataAssetName));
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

    public GeneratedQuestionnaire prePopulate() {
      return new GeneratedQuestionnaire(buildProcessor().prePopulate(baseResource, patientId,
          parameters, bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
    }

    public GeneratedQuestionnaireResponse populate() {
      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor().populate(baseResource, patientId, parameters,
              bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
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
