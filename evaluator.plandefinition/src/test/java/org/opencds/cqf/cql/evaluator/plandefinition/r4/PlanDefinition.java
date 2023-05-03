package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepository;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class PlanDefinition {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

  private static InputStream open(String asset) {
    return PlanDefinition.class.getResourceAsStream(asset);
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

  public static PlanDefinitionProcessor buildProcessor(Repository repository) {
    return new PlanDefinitionProcessor(repository, EvaluationSettings.getDefault());
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static Apply that(String planDefinitionID, String patientID, String encounterID) {
      return new Apply(planDefinitionID, patientID, encounterID);
    }
  }

  static class Apply {
    private String planDefinitionID;

    private String patientID;
    private String encounterID;

    private Repository repository;
    private Repository dataRepository;
    private Repository contentRepository;
    private Repository terminologyRepository;
    private Bundle additionalData;
    private IdType additionalDataId;
    private Parameters parameters;
    private IdType expectedBundleId;
    private IdType expectedCarePlanId;

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    public Apply(String planDefinitionID, String patientID, String encounterID) {
      this.planDefinitionID = planDefinitionID;
      this.patientID = patientID;
      this.encounterID = encounterID;
    }

    public Apply withData(String dataAssetName) {
      dataRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public Apply withContent(String dataAssetName) {
      contentRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    public Apply withTerminology(String dataAssetName) {
      terminologyRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));

      return this;
    }

    private void loadAdditionalData(IBaseResource resource) {
      additionalData =
          resource.getIdElement().getResourceType().equals(FHIRAllTypes.BUNDLE.toCode())
              ? (Bundle) resource
              : new Bundle().setType(BundleType.COLLECTION)
                  .addEntry(new BundleEntryComponent().setResource((Resource) resource));
    }

    public Apply withAdditionalData(String dataAssetName) {
      var data = parse(dataAssetName);
      loadAdditionalData(data);

      return this;
    }

    public Apply withAdditionalDataId(IdType theId) {
      additionalDataId = theId;

      return this;
    }

    public Apply withParameters(Parameters params) {
      parameters = params;

      return this;
    }

    public Apply withRepository(Repository repository) {
      this.repository = repository;

      return this;
    }

    public Apply withExpectedBundleId(IdType theId) {
      expectedBundleId = theId;

      return this;
    }

    public Apply withExpectedCarePlanId(IdType theId) {
      expectedCarePlanId = theId;

      return this;
    }

    private void buildRepository() {
      if (repository != null) {
        return;
      }
      if (dataRepository == null) {
        dataRepository = new TestRepository(fhirContext, this.getClass(), List.of("tests"), false);
      }
      if (contentRepository == null) {
        contentRepository =
            new TestRepository(fhirContext, this.getClass(), List.of("resources"), false);
      }
      if (terminologyRepository == null) {
        terminologyRepository = new TestRepository(fhirContext, this.getClass(),
            List.of("vocabulary/CodeSystem", "vocabulary/ValueSet"), false);
      }

      repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
    }

    public GeneratedBundle applyR5() {
      buildRepository();
      var libraryEngine = new LibraryEngine(this.repository);
      Bundle expectedBundle = null;
      if (expectedBundleId != null) {
        try {
          expectedBundle = repository.read(Bundle.class, expectedBundleId);
        } catch (Exception e) {
        }
      }
      if (additionalDataId != null) {
        var resource =
            repository.read(fhirContext.getResourceDefinition(additionalDataId.getResourceType())
                .newInstance().getClass(), additionalDataId);
        loadAdditionalData(resource);
      }
      return new GeneratedBundle((Bundle) buildProcessor(repository).applyR5(
          new IdType("PlanDefinition", planDefinitionID), null, null, patientID, encounterID, null,
          null, null, null, null, null, null, parameters, null, additionalData, null,
          libraryEngine), expectedBundle);
    }

    public GeneratedCarePlan apply() {
      buildRepository();
      var libraryEngine = new LibraryEngine(this.repository);
      CarePlan expectedCarePlan = null;
      if (expectedCarePlanId != null) {
        try {
          expectedCarePlan = repository.read(CarePlan.class, expectedCarePlanId);
        } catch (Exception e) {
        }
      }
      if (additionalDataId != null) {
        var resource =
            repository.read(fhirContext.getResourceDefinition(additionalDataId.getResourceType())
                .newInstance().getClass(), additionalDataId);
        loadAdditionalData(resource);
      }
      return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
          new IdType("PlanDefinition", planDefinitionID), null, null, patientID, encounterID, null,
          null, null, null, null, null, null, parameters, null, additionalData, null,
          libraryEngine), expectedCarePlan);
    }

    public GeneratedPackage packagePlanDefinition() {
      buildRepository();
      return new GeneratedPackage(
          (Bundle) buildProcessor(repository)
              .packagePlanDefinition(new IdType("PlanDefinition", planDefinitionID), null, null,
                  true),
          null);
    }
  }

  static class GeneratedBundle {
    Bundle myGeneratedBundle;
    Bundle myExpectedBundle;

    public GeneratedBundle(Bundle theGeneratedBundle, Bundle theExpectedBundle) {
      myGeneratedBundle = theGeneratedBundle;
      myExpectedBundle = theExpectedBundle;
    }

    public void isEqualsTo(String expectedBundleAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedBundleAssetName),
            jsonParser.encodeResourceToString(myGeneratedBundle), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public void isEqualsToExpected() {
      try {
        JSONAssert.assertEquals(jsonParser.encodeResourceToString(myExpectedBundle),
            jsonParser.encodeResourceToString(myGeneratedBundle), true);
      } catch (JSONException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public void hasEntry(int theCount) {
      assertEquals(myGeneratedBundle.getEntry().size(), theCount);
    }

    public void hasQuestionnaireOperationOutcome() {
      assertTrue(myGeneratedBundle.getEntry().stream().map(e -> e.getResource())
          .anyMatch(r -> r.getResourceType().equals(ResourceType.Questionnaire)
              && ((Questionnaire) r).getContained().stream()
                  .anyMatch(c -> c.getResourceType().equals(ResourceType.OperationOutcome))));
    }
  }

  static class GeneratedCarePlan {
    CarePlan myGeneratedCarePlan;
    CarePlan myExpectedCarePlan;

    public GeneratedCarePlan(CarePlan theGeneratedCarePlan, CarePlan theExpectedCarePlan) {
      myGeneratedCarePlan = theGeneratedCarePlan;
      myExpectedCarePlan = theExpectedCarePlan;
    }

    public void isEqualsTo(String expectedCarePlanAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedCarePlanAssetName),
            jsonParser.encodeResourceToString(myGeneratedCarePlan), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public void isEqualsToExpected() {
      try {
        JSONAssert.assertEquals(jsonParser.encodeResourceToString(myExpectedCarePlan),
            jsonParser.encodeResourceToString(myGeneratedCarePlan), true);
      } catch (JSONException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }

    public void hasContained(int theCount) {
      assertEquals(myGeneratedCarePlan.getContained().size(), theCount);
    }

    public void hasOperationOutcome() {
      assertTrue(myGeneratedCarePlan.getContained().stream()
          .anyMatch(r -> r.getResourceType().equals(ResourceType.OperationOutcome)));
    }
  }

  static class GeneratedPackage {
    Bundle myGeneratedBundle;
    Bundle myExpectedBundle;

    public GeneratedPackage(Bundle theGeneratedBundle, Bundle theExpectedBundle) {
      myGeneratedBundle = theGeneratedBundle;
      myExpectedBundle = theExpectedBundle;
    }

    public void hasEntry(int theCount) {
      assertEquals(myGeneratedBundle.getEntry().size(), theCount);
    }
  }
}
