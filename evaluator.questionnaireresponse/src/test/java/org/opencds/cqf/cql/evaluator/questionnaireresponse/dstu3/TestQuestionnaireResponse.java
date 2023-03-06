package org.opencds.cqf.cql.evaluator.questionnaireresponse.dstu3;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.repository.dstu3.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaireResponse {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
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

  public static QuestionnaireResponseProcessor buildProcessor(Repository repository) {
    return new QuestionnaireResponseProcessor(repository);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static Extract that(String questionnaireResponseName) {
      return new Extract(questionnaireResponseName);
    }
  }

  static class Extract {
    private Repository repository;
    private QuestionnaireResponse baseResource;

    public Extract(String questionnaireResponseName) {
      baseResource = (QuestionnaireResponse) parse(questionnaireResponseName);
      FhirRepository data = new FhirRepository(this.getClass(), List.of("tests"), false);
      FhirRepository content = new FhirRepository(this.getClass(), List.of("content/"), false);
      FhirRepository terminology = new FhirRepository(this.getClass(),
          List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);

      this.repository = Repositories.proxy(data, content, terminology);
    }

    public GeneratedBundle extract() {
      return new GeneratedBundle((Bundle) buildProcessor(repository).extract(baseResource));
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
