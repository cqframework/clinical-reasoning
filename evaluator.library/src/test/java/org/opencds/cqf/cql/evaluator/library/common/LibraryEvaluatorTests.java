package org.opencds.cqf.cql.evaluator.library.common;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.ParameterParser;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.DataProviderExtender;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class LibraryEvaluatorTests {

    LibraryEvaluator libraryEvaluator = null;
    FhirContext fhirContext = null;

    @BeforeClass
    @SuppressWarnings("serial")
    public void SetupEvaluator() {
        fhirContext = FhirContext.forR4();

        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();

        Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = new HashSet<TypedLibrarySourceProviderFactory>() {
            {
                add(new TypedLibrarySourceProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public LibrarySourceProvider create(String url, List<String> headers) {
                        return new BundleLibrarySourceProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(LibraryEvaluatorTests.class.getResourceAsStream(url)),
                                adapterFactory);
                    }
                });
            }
        };

        Set<ModelResolverFactory> modelResolverFactories = new HashSet<ModelResolverFactory>() {
            {
                add(new FhirModelResolverFactory());
            }
        };

        LibraryLoaderFactory libraryLoaderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory(librarySourceProviderFactories);
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
                                .parseResource(LibraryEvaluatorTests.class.getResourceAsStream(url)));
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
                                .parseResource(LibraryEvaluatorTests.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(typedTerminologyProviderFactories);

        DataProviderConfigurer dataProviderConfigurer = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderConfigurer();

        DataProviderExtender dataProviderExtender = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderExtender();

        ParameterParser parameterParser = new org.opencds.cqf.cql.evaluator.common.ParameterParser();

        EndpointConverter endpointConverter = new org.opencds.cqf.cql.evaluator.builder.common.EndpointConverter(
                adapterFactory);

        libraryEvaluator = new LibraryEvaluator(fhirContext, adapterFactory, libraryLoaderFactory,
                dataProviderFactory, terminologyProviderFactory, dataProviderConfigurer, dataProviderExtender,
                parameterParser, endpointConverter);
    }

    @Test
    public void TestEXM125() {
        Endpoint endpoint = new Endpoint().setAddress("r4/EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Parameters actual = (Parameters)libraryEvaluator.evaluate(
                new VersionedIdentifier().withId("EXM125").withVersion("8.0.000"), "Patient", "numer-EXM125", null,
                null, null, endpoint, endpoint, endpoint, null, null, null);
        
        Boolean foundParam = false;
        for (ParametersParameterComponent part : actual.getParameter()) {
            if (part.getName().equals("Numerator")) {
                for (ParametersParameterComponent innerPart : part.getPart()) {
                    if (innerPart.getName().equals("value")) {
                        foundParam = true;
                        assertTrue(innerPart.getValue().castToBoolean(innerPart.getValue()).booleanValue());
                    }
                }
            }
        }

        assertTrue(foundParam);


        IParser parser = this.fhirContext.newJsonParser();
        parser.setPrettyPrint(true);
        System.out.println(parser.encodeResourceToString(actual));
    }

}