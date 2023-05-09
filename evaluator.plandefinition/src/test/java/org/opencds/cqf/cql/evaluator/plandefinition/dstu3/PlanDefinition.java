package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
  private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

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
    private Parameters parameters;

    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

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

    public Apply withAdditionalData(String dataAssetName) {
      var data = parse(dataAssetName);
      additionalData =
          data.getIdElement().getResourceType().equals(FHIRAllTypes.BUNDLE.toCode()) ? (Bundle) data
              : new Bundle().setType(BundleType.COLLECTION)
                  .addEntry(new BundleEntryComponent().setResource((Resource) data));

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

    public GeneratedCarePlan apply() {
      buildRepository();
      var libraryEngine = new LibraryEngine(this.repository, evaluationSettings);
      return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
          new IdType("PlanDefinition", planDefinitionID), null, null, patientID, encounterID, null,
          null, null, null, null, null, null, parameters, null, additionalData, null,
          libraryEngine));
    }
  }

  static class GeneratedCarePlan {
    CarePlan carePlan;

    public GeneratedCarePlan(CarePlan carePlan) {
      this.carePlan = carePlan;
    }

    public void isEqualsTo(String expectedCarePlanAssetName) {
      try {
        JSONAssert.assertEquals(load(expectedCarePlanAssetName),
            jsonParser.encodeResourceToString(carePlan), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }
    }
  }
}
