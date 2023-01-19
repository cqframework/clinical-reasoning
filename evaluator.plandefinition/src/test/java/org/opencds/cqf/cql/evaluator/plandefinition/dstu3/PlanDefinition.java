package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.activitydefinition.dstu3.ActivityDefinitionProcessor;
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
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class PlanDefinition {
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    private static InputStream open(String asset) { return PlanDefinition.class.getResourceAsStream(asset); }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException { return load(open(asset)); }

    public static IBaseResource parse(String asset) {
        return jsonParser.parseResource(open(asset));
    }


    public static PlanDefinitionProcessor buildProcessor(FhirDal fhirDal) {
        AdapterFactory adapterFactory = new AdapterFactory();
        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

        FhirModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();
        Set<ModelResolverFactory> modelResolverFactories = Collections.singleton(fhirModelResolverFactory);

        Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = Collections.singleton(
            new TypedLibrarySourceProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public LibrarySourceProvider create(String url, List<String> headers) {
                    return new BundleFhirLibrarySourceProvider(fhirContext,
                            (IBaseBundle) parse(url), adapterFactory, libraryVersionSelector);
                }
            }
        );

        LibrarySourceProviderFactory librarySourceProviderFactory = new LibrarySourceProviderFactory(
                fhirContext, adapterFactory, librarySourceProviderFactories, libraryVersionSelector);

        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = Collections.singleton(
            new TypedRetrieveProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public RetrieveProvider create(String url, List<String> headers) {
                    return new BundleRetrieveProvider(fhirContext, (IBaseBundle) parse(url));
                }
            }
        );

        DataProviderFactory dataProviderFactory = new DataProviderFactory(
                fhirContext, modelResolverFactories, retrieveProviderFactories);

        Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = Collections.singleton(
            new TypedTerminologyProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public TerminologyProvider create(String url, List<String> headers) {
                    return new BundleTerminologyProvider(fhirContext, (IBaseBundle) parse(url));
                }
            }
        );

        TerminologyProviderFactory terminologyProviderFactory = new TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        LibraryProcessor libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

        ExpressionEvaluator evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

        ActivityDefinitionProcessor activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
        OperationParametersParser operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);

        return new PlanDefinitionProcessor(
            fhirContext, fhirDal, libraryProcessor, evaluator,
            activityDefinitionProcessor, operationParametersParser
        );
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

        private MockFhirDal fhirDal = new MockFhirDal();
        private Endpoint dataEndpoint;
        private Endpoint libraryEndpoint;
        private IBaseResource baseResource;
        private Parameters parameters;

        public Apply(String planDefinitionID, String patientID, String encounterID) {
            this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
        }

        public Apply withData(String dataAssetName) {
            dataEndpoint = new Endpoint()
                .setAddress(dataAssetName)
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

            baseResource = parse(dataAssetName);

            fhirDal.addAll(baseResource);
            return this;
        }

        public Apply withLibrary(String dataAssetName) {
            libraryEndpoint = new Endpoint()
                    .setAddress(dataAssetName)
                    .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

            fhirDal.addAll(parse(dataAssetName));
            return this;
        }

        public Apply withParameters(Parameters params) {
            parameters = params;
            return this;
        }

        public GeneratedCarePlan apply() {
            return new GeneratedCarePlan(
                    (CarePlan) buildProcessor(fhirDal)
                        .apply(
                            new IdType("PlanDefinition", planDefinitionID),
                            patientID,
                            encounterID,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            parameters,
                            null,
                            (Bundle) baseResource,
                            null,
                            dataEndpoint,
                            libraryEndpoint,
                            libraryEndpoint
                        )
            );
        }
    }

    static class GeneratedCarePlan {
        CarePlan carePlan;

        public GeneratedCarePlan(CarePlan carePlan) {
            this.carePlan = carePlan;
        }

        public void isEqualsTo(String expectedCarePlanAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedCarePlanAssetName),
                        jsonParser.encodeResourceToString(carePlan),
                        true
                );
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }
    }
}