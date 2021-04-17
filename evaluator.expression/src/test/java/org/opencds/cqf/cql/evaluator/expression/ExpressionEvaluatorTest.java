package org.opencds.cqf.cql.evaluator.expression;

import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.fhir.ucum.Canonical;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class ExpressionEvaluatorTest {
    FhirContext fhirContext;
    ExpressionEvaluator evaluator;

    @BeforeClass
    @SuppressWarnings("serial")
    public void setup() {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();

        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);

        Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories = new HashSet<TypedLibraryContentProviderFactory>() {
            {
                add(new TypedLibraryContentProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public LibraryContentProvider create(String url, List<String> headers) {
                        return new BundleFhirLibraryContentProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(ExpressionEvaluatorTest.class.getResourceAsStream(url)),
                                adapterFactory, libraryVersionSelector);
                    }
                });
            }
        };

        Set<ModelResolverFactory> modelResolverFactories = new HashSet<ModelResolverFactory>() {
            {
                add(new FhirModelResolverFactory());
            }
        };

        LibraryLoaderFactory libraryLoaderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory(
                fhirContext, adapterFactory, libraryContentProviderFactories, libraryVersionSelector);
        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = new HashSet<TypedRetrieveProviderFactory>() {
            {
                add(new TypedRetrieveProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public RetrieveProvider create(String url, List<String> headers) {

                        return new BundleRetrieveProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                .parseResource(ExpressionEvaluatorTest.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        DataProviderFactory dataProviderFactory = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(
                fhirContext, modelResolverFactories, retrieveProviderFactories);

        Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = new HashSet<TypedTerminologyProviderFactory>() {
            {
                add(new TypedTerminologyProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public TerminologyProvider create(String url, List<String> headers) {
                        return new BundleTerminologyProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                .parseResource(ExpressionEvaluatorTest.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

        RetrieveProviderConfigurer retrieveProviderConfigurer = new org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer(
                new RetrieveProviderConfig());

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder(retrieveProviderConfigurer);

        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory()
                .create(fhirContext.getVersion().getVersion());

        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext,
                adapterFactory, fhirTypeConverter);

        OperationParametersParser operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);
        
        evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, libraryLoaderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, cqlEvaluatorBuilder, operationParametersParser);
    }
    @Test
    public void testSimpleExpressionEvaluate() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("LocalExpression").setValue(new IntegerType(4));

        Parameters actual = (Parameters) evaluator.evaluate(null, "1 + 3", null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }
    
    @Test
    public void testIncludedLibraryExpressionEvaluate() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("LocalExpression").setValue(new BooleanType(true));

        Endpoint endpoint = new Endpoint().setAddress("EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Pair<String, String> library = Pair.of("http://localhost/fhir/Library/EXM125|8.0.000", "EXM125");
        Parameters actual = (Parameters) evaluator.evaluate(null, "not \"EXM125\".\"Numerator\"", null, Arrays.asList(library), null, null, null, null, endpoint, null);
        assertTrue(expected.equalsDeep(actual));
    }
}
