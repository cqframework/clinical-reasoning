package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Resource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.repository.r5.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestItemGenerator {
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

  public static QuestionnaireItemGenerator buildGenerator(Repository repository, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
    return new QuestionnaireItemGenerator(repository, patientId, parameters, bundle, libraryEngine);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static GenerateResult that(String type, String profile, String patientId) {
      return new GenerateResult(type, profile, patientId);
    }
  }

  static class GenerateResult {
    private DataRequirement input;
    private String profileId;
    private String patientId;
    private Repository repository;
    private Repository dataRepository;
    private Repository contentRepository;
    private Repository terminologyRepository;
    private IBaseBundle bundle;
    private IBaseParameters parameters;

    public GenerateResult(String type, String profile, String patientId) {
      this.input = new DataRequirement(FHIRTypes.fromCode(type)).addProfile(profile);
      this.profileId = profile.substring(profile.lastIndexOf("/") + 1);
      this.patientId = patientId;
    }

    public GenerateResult withData(String dataAssetName) {
      dataRepository = new FhirRepository((Bundle) parse(dataAssetName));

      return this;
    }

    public GenerateResult withContent(String dataAssetName) {
      contentRepository = new FhirRepository((Bundle) parse(dataAssetName));

      return this;
    }

    public GenerateResult withTerminology(String dataAssetName) {
      terminologyRepository = new FhirRepository((Bundle) parse(dataAssetName));

      return this;
    }

    public GenerateResult withAdditionalData(String dataAssetName) {
      var data = parse(dataAssetName);
      bundle =
          data.getIdElement().getResourceType().equals(FHIRTypes.BUNDLE.toCode()) ? (Bundle) data
              : new Bundle().setType(BundleType.COLLECTION)
                  .addEntry(new BundleEntryComponent().setResource((Resource) data));

      return this;
    }

    public GenerateResult withParameters(IBaseParameters params) {
      parameters = params;

      return this;
    }

    public GenerateResult withRepository(Repository repository) {
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
        contentRepository = new FhirRepository(this.getClass(), List.of("content"), false);
      }
      if (terminologyRepository == null) {
        terminologyRepository = new FhirRepository(this.getClass(),
            List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);
      }

      repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
    }

    public GeneratedItem generateItem() {
      buildRepository();
      var libraryEngine = new LibraryEngine(repository);
      return new GeneratedItem(buildGenerator(this.repository, this.patientId, this.parameters,
          this.bundle, libraryEngine).generateItem(input, 0), profileId);
    }
  }

  static class GeneratedItem {
    Questionnaire questionnaire;

    public GeneratedItem(QuestionnaireItemComponent item, String id) {
      this.questionnaire = new Questionnaire().setItem(Collections.singletonList(item));
      this.questionnaire.setId(id);
    }

    public void isEqualsTo(String expectedItemAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedItemAssetName),
            jsonParser.encodeResourceToString(questionnaire), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }
  }
}