package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TestQuestionnaire {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
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


  public static QuestionnaireProcessor buildProcessor(FhirDal fhirDal) {
    // var adapterFactory = new AdapterFactory();
    // var libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
    // var fhirTypeConverter =
    // new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    // var cqlFhirParametersConverter =
    // new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

    // var fhirModelResolverFactory = new FhirModelResolverFactory();
    // Set<ModelResolverFactory> modelResolverFactories =
    // Collections.singleton(fhirModelResolverFactory);

    // Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories =
    // Collections.singleton(new TypedLibrarySourceProviderFactory() {
    // @Override
    // public String getType() {
    // return Constants.HL7_FHIR_FILES;
    // }

    // @Override
    // public LibrarySourceProvider create(String url, List<String> headers) {
    // return new BundleFhirLibrarySourceProvider(fhirContext, (IBaseBundle) parse(url),
    // adapterFactory, libraryVersionSelector);
    // }
    // });

    // var librarySourceProviderFactory = new LibrarySourceProviderFactory(fhirContext,
    // adapterFactory,
    // librarySourceProviderFactories, libraryVersionSelector);

    // Set<TypedRetrieveProviderFactory> retrieveProviderFactories =
    // Collections.singleton(new TypedRetrieveProviderFactory() {
    // @Override
    // public String getType() {
    // return Constants.HL7_FHIR_FILES;
    // }

    // @Override
    // public RetrieveProvider create(String url, List<String> headers) {
    // return new BundleRetrieveProvider(fhirContext, (IBaseBundle) parse(url));
    // }
    // });

    // var dataProviderFactory =
    // new DataProviderFactory(fhirContext, modelResolverFactories, retrieveProviderFactories);

    // Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories =
    // Collections.singleton(new TypedTerminologyProviderFactory() {
    // @Override
    // public String getType() {
    // return Constants.HL7_FHIR_FILES;
    // }

    // @Override
    // public TerminologyProvider create(String url, List<String> headers) {
    // return new BundleTerminologyProvider(fhirContext, (IBaseBundle) parse(url));
    // }
    // });

    // var terminologyProviderFactory =
    // new TerminologyProviderFactory(fhirContext, typedTerminologyProviderFactories);

    // var endpointConverter = new EndpointConverter(adapterFactory);

    // var libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter,
    // librarySourceProviderFactory, dataProviderFactory, terminologyProviderFactory,
    // endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

    // var evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter,
    // librarySourceProviderFactory, dataProviderFactory, terminologyProviderFactory,
    // endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

    return new QuestionnaireProcessor(fhirContext);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static QuestionnaireResult that(String questionnaireName, String patientId) {
      return new QuestionnaireResult(questionnaireName, patientId);
    }
  }

  static class QuestionnaireResult {
    private MockFhirDal fhirDal = new MockFhirDal();
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
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      // fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public QuestionnaireResult withLibrary(String dataAssetName) {
      dataEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

      // fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public QuestionnaireResult withTerminology(String dataAssetName) {
      terminologyEndpoint = new Endpoint().setAddress(dataAssetName)
          .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

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
      return new GeneratedQuestionnaire(buildProcessor(fhirDal).prePopulate(baseResource, patientId,
          parameters, bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
    }

    public GeneratedQuestionnaireResponse populate() {
      // FhirRepository data =
      // new FhirRepository(QuestionnaireProcessor.class, List.of("res/tests"), false);
      // FhirRepository content =
      // new FhirRepository(QuestionnaireProcessor.class, List.of("res/content/"), false);
      // FhirRepository terminology = new FhirRepository(QuestionnaireProcessor.class,
      // List.of("res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/"), false);

      // var data = new RestRepository(Clients.forEndpoint(fhirContext, dataEndpoint));
      // var content = new RestRepository(Clients.forEndpoint(fhirContext, contentEndpoint));
      // var terminology = new RestRepository(Clients.forEndpoint(fhirContext,
      // terminologyEndpoint));

      // var repository = Repositories.proxy(data, content, terminology);
      // var libraryEngine = new LibraryEngine(fhirContext, repository);

      return new GeneratedQuestionnaireResponse(
          (QuestionnaireResponse) buildProcessor(fhirDal).populate(baseResource, patientId,
              parameters, bundle, dataEndpoint, contentEndpoint, terminologyEndpoint));
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
