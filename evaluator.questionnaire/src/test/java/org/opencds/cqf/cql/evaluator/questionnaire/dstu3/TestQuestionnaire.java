package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
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
    var adapterFactory = new AdapterFactory();
    var libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
    var fhirTypeConverter =
        new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    var cqlFhirParametersConverter =
        new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

    var fhirModelResolverFactory = new FhirModelResolverFactory();
    Set<ModelResolverFactory> modelResolverFactories =
        Collections.singleton(fhirModelResolverFactory);

    Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories =
        Collections.singleton(new TypedLibrarySourceProviderFactory() {
          @Override
          public String getType() {
            return Constants.HL7_FHIR_FILES;
          }

          @Override
          public LibrarySourceProvider create(String url, List<String> headers) {
            return new BundleFhirLibrarySourceProvider(fhirContext, (IBaseBundle) parse(url),
                adapterFactory, libraryVersionSelector);
          }
        });

    var librarySourceProviderFactory = new LibrarySourceProviderFactory(fhirContext, adapterFactory,
        librarySourceProviderFactories, libraryVersionSelector);

    Set<TypedRetrieveProviderFactory> retrieveProviderFactories =
        Collections.singleton(new TypedRetrieveProviderFactory() {
          @Override
          public String getType() {
            return Constants.HL7_FHIR_FILES;
          }

          @Override
          public RetrieveProvider create(String url, List<String> headers) {
            return new BundleRetrieveProvider(fhirContext, (IBaseBundle) parse(url));
          }
        });

    var dataProviderFactory =
        new DataProviderFactory(fhirContext, modelResolverFactories, retrieveProviderFactories);

    Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories =
        Collections.singleton(new TypedTerminologyProviderFactory() {
          @Override
          public String getType() {
            return Constants.HL7_FHIR_FILES;
          }

          @Override
          public TerminologyProvider create(String url, List<String> headers) {
            return new BundleTerminologyProvider(fhirContext, (IBaseBundle) parse(url));
          }
        });

    var terminologyProviderFactory =
        new TerminologyProviderFactory(fhirContext, typedTerminologyProviderFactories);

    var endpointConverter = new EndpointConverter(adapterFactory);

    var libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter,
        librarySourceProviderFactory, dataProviderFactory, terminologyProviderFactory,
        endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

    var evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter,
        librarySourceProviderFactory, dataProviderFactory, terminologyProviderFactory,
        endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

    return new QuestionnaireProcessor(fhirContext, fhirDal, libraryProcessor, evaluator);
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

      fhirDal.addAll(parse(dataAssetName));
      return this;
    }

    public QuestionnaireResult withLibrary(String dataAssetName) {
      bundle = (Bundle) parse(dataAssetName);
      return this;
    }

    public QuestionnaireResult withParameters(Parameters params) {
      parameters = params;
      return this;
    }

    public GeneratedQuestionnaire prePopulate() {
      return new GeneratedQuestionnaire(buildProcessor(fhirDal).prePopulate(baseResource, patientId,
          parameters, bundle, dataEndpoint, null, null));
    }

    public GeneratedQuestionnaireResponse populate() {
      return new GeneratedQuestionnaireResponse((QuestionnaireResponse) buildProcessor(fhirDal)
          .populate(baseResource, patientId, parameters, bundle, dataEndpoint, null, null));
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
