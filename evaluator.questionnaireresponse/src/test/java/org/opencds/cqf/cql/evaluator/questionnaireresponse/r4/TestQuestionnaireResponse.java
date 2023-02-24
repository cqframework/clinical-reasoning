package org.opencds.cqf.cql.evaluator.questionnaireresponse.r4;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaireResponse {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

  private static InputStream open(String asset) {
    return TestQuestionnaireResponse.class.getResourceAsStream(asset);
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


  public static QuestionnaireResponseProcessor buildProcessor(FhirDal fhirDal) {
    return new QuestionnaireResponseProcessor(fhirContext, fhirDal);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static Extract that(String questionnaireResponseName) {
      return new Extract(questionnaireResponseName);
    }
  }

  static class Extract {
    private MockFhirDal fhirDal = new MockFhirDal();
    private Endpoint dataEndpoint;
    private Endpoint libraryEndpoint;
    private QuestionnaireResponse baseResource;

    public Extract(String questionnaireResponseName) {
      baseResource = (QuestionnaireResponse) parse(questionnaireResponseName);
    }

    public Extract withData(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName).setConnectionType(
          new Coding().setCode(org.opencds.cqf.cql.evaluator.builder.Constants.HL7_FHIR_FILES));

      fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public Extract withLibrary(String dataAssetName) {
      libraryEndpoint = new Endpoint().setAddress(dataAssetName).setConnectionType(
          new Coding().setCode(org.opencds.cqf.cql.evaluator.builder.Constants.HL7_FHIR_FILES));

      fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public GeneratedBundle extract() {
      return new GeneratedBundle((Bundle) buildProcessor(fhirDal).extract(baseResource));
    }
  }

  static class GeneratedBundle {
    Bundle bundle;

    public GeneratedBundle(Bundle bundle) {
      this.bundle = bundle;
    }

    public void isEqualsTo(String expectedBundleAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedBundleAssetName),
            jsonParser.encodeResourceToString(bundle), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }
  }
}
