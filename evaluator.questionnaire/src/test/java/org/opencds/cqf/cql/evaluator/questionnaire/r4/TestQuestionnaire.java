package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepository;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;
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

    public static QuestionnaireResult that(IdType theId, String thePatientId) {
      return new QuestionnaireResult(theId, thePatientId);
    }
  }

  static class QuestionnaireResult {
    private IdType questionnaireId;
    private Questionnaire questionnaire;
    private String patientId;
    private Repository repository;
    private Repository dataRepository;
    private Repository contentRepository;
    private Repository terminologyRepository;
    private Bundle bundle;
    private Parameters parameters;

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    public QuestionnaireResult(String questionnaireName, String patientId) {
      questionnaire = questionnaireName.isEmpty() ? null : (Questionnaire) parse(questionnaireName);
      questionnaireId = null;
      this.patientId = patientId;
    }

    public QuestionnaireResult(IdType theId, String thePatientId) {
      questionnaire = null;
      questionnaireId = theId;
      patientId = thePatientId;
    }

    public QuestionnaireResult withData(String dataAssetName) {
      dataRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withContent(String dataAssetName) {
      contentRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

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
      if (repository == null) {
        if (dataRepository == null) {
          dataRepository =
              new TestRepository(fhirContext, this.getClass(), List.of("tests"), false);
        }
        if (contentRepository == null) {
          contentRepository =
              new TestRepository(fhirContext, this.getClass(), List.of("resources/"), false);
        }
        if (terminologyRepository == null) {
          terminologyRepository = new TestRepository(fhirContext, this.getClass(),
              List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);
        }

        repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
      }

      if (questionnaire == null) {
        try {
          questionnaire = repository.read(Questionnaire.class, questionnaireId);
        } catch (Exception e) {
        }
      }
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
      var generatedPackage = buildProcessor(repository).packageQuestionnaire(questionnaire, true);
      return generatedPackage;
    }
  }

  static class GeneratedQuestionnaire {
    Questionnaire myQuestionnaire;
    List<QuestionnaireItemComponent> myItems;

    private void populateMyItems(List<QuestionnaireItemComponent> theItems) {
      for (var item : theItems) {
        myItems.add(item);
        if (item.hasItem()) {
          populateMyItems(item.getItem());
        }
      }
    }

    public GeneratedQuestionnaire(Questionnaire theQuestionnaire) {
      myQuestionnaire = theQuestionnaire;
      myItems = new ArrayList<>();
      populateMyItems(myQuestionnaire.getItem());
    }

    public void isEqualsTo(String expectedQuestionnaireAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedQuestionnaireAssetName),
            jsonParser.encodeResourceToString(myQuestionnaire), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public GeneratedQuestionnaire hasItems(int expectedItemCount) {
      assertEquals(myItems.size(), expectedItemCount);

      return this;
    }

    public GeneratedQuestionnaire itemHasInitial(String theLinkId) {
      var matchingItems = myItems.stream().filter(i -> i.getLinkId().equals(theLinkId))
          .collect(Collectors.toList());
      for (var item : matchingItems) {
        assertTrue(item.hasInitial());
      }

      return this;
    }
  }

  static class GeneratedQuestionnaireResponse {
    QuestionnaireResponse myQuestionnaireResponse;
    List<QuestionnaireResponseItemComponent> myItems;

    private void populateMyItems(List<QuestionnaireResponseItemComponent> theItems) {
      for (var item : theItems) {
        myItems.add(item);
        if (item.hasItem()) {
          populateMyItems(item.getItem());
        }
      }
    }

    public GeneratedQuestionnaireResponse(QuestionnaireResponse theQuestionnaireResponse) {
      myQuestionnaireResponse = theQuestionnaireResponse;
      myItems = new ArrayList<>();
      populateMyItems(myQuestionnaireResponse.getItem());
    }

    public void isEqualsTo(String expectedQuestionnaireResponseAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedQuestionnaireResponseAssetName),
            jsonParser.encodeResourceToString(myQuestionnaireResponse), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public GeneratedQuestionnaireResponse hasItems(int expectedItemCount) {
      assertEquals(myItems.size(), expectedItemCount);

      return this;
    }

    public GeneratedQuestionnaireResponse itemHasAnswer(String theLinkId) {
      var matchingItems = myItems.stream().filter(i -> i.getLinkId().equals(theLinkId))
          .collect(Collectors.toList());
      for (var item : matchingItems) {
        assertTrue(item.hasAnswer());
      }

      return this;
    }
  }
}
