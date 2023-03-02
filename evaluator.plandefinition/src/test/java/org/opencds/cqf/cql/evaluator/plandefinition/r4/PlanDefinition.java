package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.opencds.cqf.fhir.api.Repository;
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
    AdapterFactory adapterFactory = new AdapterFactory();
    FhirTypeConverter fhirTypeConverter =
        new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    ActivityDefinitionProcessor activityDefinitionProcessor =
        new ActivityDefinitionProcessor(fhirContext, repository);
    OperationParametersParser operationParametersParser =
        new OperationParametersParser(adapterFactory, fhirTypeConverter);

    return new PlanDefinitionProcessor(fhirContext, repository, activityDefinitionProcessor,
        operationParametersParser);
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
    private Endpoint dataEndpoint;
    private Endpoint contentEndpoint;
    private Endpoint terminologyEndpoint;
    private Bundle additionalData;
    private Parameters parameters;

    public Apply(String planDefinitionID, String patientID, String encounterID) {
      this.planDefinitionID = planDefinitionID;
      this.patientID = patientID;
      this.encounterID = encounterID;
      FhirRepository data = new FhirRepository(this.getClass(), List.of("res/tests"), false);
      FhirRepository content = new FhirRepository(this.getClass(), List.of("res/content/"), false);
      FhirRepository terminology = new FhirRepository(this.getClass(),
          List.of("res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/"), false);

      this.repository = Repositories.proxy(data, content, terminology);
    }

    public Apply withData(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public Apply withContent(String dataAssetName) {
      contentEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public Apply withTerminology(String dataAssetName) {
      terminologyEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      return this;
    }

    public Apply withAdditionalData(String dataAssetName) {
      additionalData = (Bundle) parse(dataAssetName);

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

    public GeneratedBundle applyR5() {
      return new GeneratedBundle((Bundle) buildProcessor(repository).applyR5(
          new IdType("PlanDefinition", planDefinitionID), patientID, encounterID, null, null, null,
          null, null, null, null, null, parameters, null, additionalData, null, dataEndpoint,
          contentEndpoint, terminologyEndpoint));
    }

    public GeneratedBundle applyR5WithEngine() {
      var libraryEngine = new LibraryEngine(fhirContext, this.repository);

      return new GeneratedBundle((Bundle) buildProcessor(repository).applyR5(
          new IdType("PlanDefinition", planDefinitionID), patientID, encounterID, null, null, null,
          null, null, null, null, null, parameters, null, additionalData, null, libraryEngine));
    }

    public GeneratedCarePlan apply() {
      return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
          new IdType("PlanDefinition", planDefinitionID), patientID, encounterID, null, null, null,
          null, null, null, null, null, parameters, null, additionalData, null, dataEndpoint,
          contentEndpoint, terminologyEndpoint));
    }

    public GeneratedCarePlan applyWithEngine() {
      var libraryEngine = new LibraryEngine(fhirContext, this.repository);

      return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
          new IdType("PlanDefinition", planDefinitionID), patientID, encounterID, null, null, null,
          null, null, null, null, null, parameters, null, additionalData, null, libraryEngine));
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
