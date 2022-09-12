package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.FhirRestFhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.RestFhirDal;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.BeforeMethod;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class PlanDefinitionProcessorRestTestBase {
    private final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    private RestFhirDal fhirDal;
    protected PlanDefinitionProcessor planDefinitionProcessor;

    private InputStream open(String asset) {
        return PlanDefinitionProcessorRestTestBase.class.getResourceAsStream(asset);
    }

    public IBaseResource load(String asset) {
        return jsonParser.parseResource(open(asset));
    }

    @BeforeMethod
    public void setup() {
//        fhirDal = (RestFhirDal) new FhirRestFhirDalFactory(
//                new ClientFactory(FhirContext.forR4Cached())).create(
//                        "http://localhost:8080/fhir", Collections.singletonList("Content-Type: application/json"));
        fhirDal = (RestFhirDal) new FhirRestFhirDalFactory(
                new ClientFactory(FhirContext.forR4Cached())).create(
                        "https://cloud.alphora.com/sandbox/r4/cqm/fhir", Collections.singletonList("Content-Type: application/json"));

        AdapterFactory adapterFactory = new AdapterFactory();
        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

        FhirModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();
        Set<ModelResolverFactory> modelResolverFactories = Collections.singleton(fhirModelResolverFactory);
        ClientFactory clientFactory = new ClientFactory(fhirContext);

        FhirRestLibrarySourceProviderFactory restLibrarySourceProviderFactory = new FhirRestLibrarySourceProviderFactory(
                clientFactory, adapterFactory, libraryVersionSelector);
        LibrarySourceProviderFactory librarySourceProviderFactory = new LibrarySourceProviderFactory(
                fhirContext, adapterFactory, Collections.singleton(restLibrarySourceProviderFactory), libraryVersionSelector);

        FhirRestRetrieveProviderFactory retrieveProviderFactory = new FhirRestRetrieveProviderFactory(fhirContext, clientFactory);
        DataProviderFactory dataProviderFactory = new DataProviderFactory(
                fhirContext, modelResolverFactories, Collections.singleton(retrieveProviderFactory));

        FhirRestTerminologyProviderFactory restTerminologyProviderFactory = new FhirRestTerminologyProviderFactory(fhirContext, clientFactory);

        TerminologyProviderFactory terminologyProviderFactory = new TerminologyProviderFactory(
                fhirContext, Collections.singleton(restTerminologyProviderFactory));

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        LibraryProcessor libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

        ExpressionEvaluator evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, CqlEvaluatorBuilder::new);

        ActivityDefinitionProcessor activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
        OperationParametersParser operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);

        planDefinitionProcessor = new PlanDefinitionProcessor(fhirContext, fhirDal, libraryProcessor, evaluator,
            activityDefinitionProcessor, operationParametersParser);
    }

    public void test(String dataAsset, String libraryAsset, String planDefinitionID,
                     String patientID, String encounterID, String expectedCarePlan) {
        Parameters params = new Parameters();

        CarePlan expected = (CarePlan) load(expectedCarePlan);

        Endpoint endpoint = new Endpoint().setAddress("https://cloud.alphora.com/sandbox/r4/cqm/fhir")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_REST));
//        Endpoint endpoint = new Endpoint().setAddress("http://localhost:8080/fhir")
//                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_REST));

        IBaseResource artifactBundle = load(libraryAsset);
        if (artifactBundle instanceof IBaseBundle) {
            fhirDal.transaction((IBaseBundle) artifactBundle);
        }

        IBaseResource baseResource = load(dataAsset);
        Bundle bundleToSend = (Bundle)baseResource;

        long start = System.currentTimeMillis();
        CarePlan actual = planDefinitionProcessor.apply(
                new IdType("PlanDefinition", planDefinitionID), patientID, encounterID,
                null, null, null, null, null,
                null, null, null, params, null,
                bundleToSend, null, endpoint, endpoint, endpoint);
        long time = System.currentTimeMillis() - start;
        System.out.println("Time to run 1st time (ms): " + time);

        String expectedJson = jsonParser.encodeResourceToString(expected);
        String actualJson = jsonParser.encodeResourceToString(actual);

        assertEquals(actualJson, expectedJson);
    }
}