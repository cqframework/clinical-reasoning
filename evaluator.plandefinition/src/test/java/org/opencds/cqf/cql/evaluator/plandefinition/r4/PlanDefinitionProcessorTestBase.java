package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.BeforeMethod;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class PlanDefinitionProcessorTestBase {
    private final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    private MockFhirDal fhirDal;
    protected PlanDefinitionProcessor planDefinitionProcessor;
    
    private InputStream open(String asset) {
        return PlanDefinitionProcessorTestBase.class.getResourceAsStream(asset);
    }
    
    public IBaseResource load(String asset) {
        return jsonParser.parseResource(open(asset));
    }

    @BeforeMethod
    public void setup() {
        // cleans in memory database at every test. 
        fhirDal = new MockFhirDal();

        AdapterFactory adapterFactory = new AdapterFactory();
        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

        FhirModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();
        Set<ModelResolverFactory> modelResolverFactories = Collections.singleton(fhirModelResolverFactory);

        Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories = Collections.singleton(
            new TypedLibraryContentProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public LibraryContentProvider create(String url, List<String> headers) {
                    return new BundleFhirLibraryContentProvider(fhirContext,
                            (IBaseBundle) load(url), adapterFactory, libraryVersionSelector);
                }
            }
        );

        LibraryContentProviderFactory libraryContentProviderFactory = new LibraryContentProviderFactory(
                fhirContext, adapterFactory, libraryContentProviderFactories, libraryVersionSelector);

        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = Collections.singleton(
            new TypedRetrieveProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public RetrieveProvider create(String url, List<String> headers) {
                    return new BundleRetrieveProvider(fhirContext, (IBaseBundle) load(url));
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
                    return new BundleTerminologyProvider(fhirContext, (IBaseBundle) load(url));
                }
            }
        );
        
        TerminologyProviderFactory terminologyProviderFactory = new TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        LibraryProcessor libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, libraryContentProviderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());
            
        ExpressionEvaluator evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, libraryContentProviderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());

        ActivityDefinitionProcessor activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
        OperationParametersParser operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);
        
        planDefinitionProcessor = new PlanDefinitionProcessor(fhirContext, fhirDal, libraryProcessor, evaluator, 
            activityDefinitionProcessor, operationParametersParser);
    }

    public void test(String dataAsset, String libraryAsset, 
                    String planDefinitionID, String patientID, String encounterID, 
                    String expectedCarePlan) { 
        Parameters params = new Parameters();

        fhirDal.addAll(load(libraryAsset));
        fhirDal.addAll(load(dataAsset));
        
        CarePlan expected = (CarePlan) load(expectedCarePlan);

        Endpoint endpoint = new Endpoint().setAddress(libraryAsset)
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Endpoint dataEndpoint = new Endpoint().setAddress(dataAsset)
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        CarePlan actual = planDefinitionProcessor.apply(
                new IdType("PlanDefinition", planDefinitionID), patientID, encounterID, 
                null, null, null, null, null, 
                null, null, null, params, null, 
                new Bundle(), null, dataEndpoint, endpoint, endpoint);

        String expectedJson = jsonParser.encodeResourceToString(expected);
        String actualJson = jsonParser.encodeResourceToString(actual);

        //assertTrue(expected.equalsShallow(actual));
        assertEquals(actualJson, expectedJson);
    }
}