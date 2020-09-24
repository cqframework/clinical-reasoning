package org.opencds.cqf.cql.evaluator.library.common;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class LibraryProcessorTests {

    LibraryProcessor libraryProcessor = null;
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
                                        .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)),
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

        LibraryLoaderFactory libraryLoaderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory(fhirContext, adapterFactory, librarySourceProviderFactories);
        Set<RetrieveProviderFactory> retrieveProviderFactories = new HashSet<RetrieveProviderFactory>() {
            {
                add(new RetrieveProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public RetrieveProvider create(String url, List<String> headers) {

                        return new BundleRetrieveProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)));
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
                                .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(fhirContext, typedTerminologyProviderFactories);

        RetrieveProviderConfigurer retrieveProviderConfigurer = new org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer(new RetrieveProviderConfig());


        EndpointConverter endpointConverter = new org.opencds.cqf.cql.evaluator.builder.common.EndpointConverter(
                adapterFactory);

        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder(retrieveProviderConfigurer);

        libraryProcessor = new LibraryProcessor(fhirContext, adapterFactory, libraryLoaderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, cqlEvaluatorBuilder);
    }

    

    @SuppressWarnings("unused")
    private Parameters loadParameters(FhirContext fhirContext, String path) {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> parametersClass = fhirContext.getResourceDefinition("Parameters").getImplementingClass();
        if (!parametersClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Parameters", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (Parameters) resource;
    }

    private boolean compareOutputParameters(Parameters expectedOutputParameters, Parameters actualOutputParameters) {
        boolean parametersMatch = true;
        for (ParametersParameterComponent part : expectedOutputParameters.getParameter()) {
            for (ParametersParameterComponent expectedPart : actualOutputParameters.getParameter()) {
                if (part.getName().equals(expectedPart.getName())) {
                    boolean innerParameterMatch = false;
                    for (ParametersParameterComponent innerPart : part.getPart()) {
                        for (ParametersParameterComponent innerExpectedPart : expectedPart.getPart()) {
                            if (innerPart.getName().equals(innerExpectedPart.getName())) {
                                if (innerPart.hasValue() && innerExpectedPart.hasValue()) {
                                    if (innerPart.getValue().equalsDeep(innerExpectedPart.getValue())) {
                                        innerParameterMatch = true;
                                    }
                                }
                                else if (innerPart.hasValue() || innerExpectedPart.hasValue()) {
                                    System.out.println("One Inner Parameter value of either the expected or actual exists and the other does not");
                                }
                                if (innerPart.hasResource() && innerExpectedPart.hasResource()) {
                                    if (innerPart.getResource().equalsDeep(innerExpectedPart.getResource())) {
                                        innerParameterMatch = true;
                                    }
                                }
                                else if (innerPart.hasResource() || innerExpectedPart.hasResource()) {
                                    System.out.println("One Inner Parameter resource of either the expected or actual exists and the other does not");
                                }
                            }
                        }
                    }
                    if (innerParameterMatch == false) {
                        parametersMatch = false;
                    }
                    if (part.hasValue() && expectedPart.hasValue()) {
                        if (!part.getValue().equalsDeep(expectedPart.getValue())) {
                            parametersMatch = false;
                        }
                        if (!part.getResource().equalsDeep(expectedPart.getResource())) {
                            parametersMatch = false;
                        }
                    }
                    else if (part.hasValue() || expectedPart.hasValue()) {
                        parametersMatch = false;
                        System.out.println("One Parameter value of either the expected or actual exists and the other does not");
                    }
                    if (part.hasResource() && expectedPart.hasResource()) {
                        if (!part.getResource().equalsDeep(expectedPart.getResource())) {
                            parametersMatch = false;
                        }
                    }
                    else if (part.hasResource() || expectedPart.hasResource()) {
                        parametersMatch = false;
                        System.out.println("One Parameter resource of either the expected or actual exists and the other does not");
                    }
                }
            }
        }
        return parametersMatch;
    }

    @Test
    public void TestEXM125() {
        Endpoint endpoint = new Endpoint().setAddress("r4/EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Parameters actual = (Parameters)libraryProcessor.evaluate(
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

    @Test
    public void TestRuleFiltersReportable() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("IsReportable").addPart().setName("value").setValue(new BooleanType(true));

        Endpoint endpoint = new Endpoint().setAddress("r4/RuleFilters-1.0.0-bundle.json")
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Endpoint dataEndpoint = new Endpoint().setAddress("r4/tests-Reportable-bundle.json")
            .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Set<String> expressions = new HashSet<String>();
        expressions.add("IsReportable");

        Parameters actual = (Parameters)libraryProcessor.evaluate(
                new VersionedIdentifier().withId("RuleFilters").withVersion("1.0.0"), "Patient", "Reportable", null,
                null, null, endpoint, endpoint, dataEndpoint, null, null, expressions);
        
        assertTrue(compareOutputParameters(expected, actual));
    }

    @Test
    public void TestRuleFiltersNotReportable() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("IsReportable").addPart().setName("value").setValue(new BooleanType(false));

        Endpoint endpoint = new Endpoint().setAddress("r4/RuleFilters-1.0.0-bundle.json")
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Endpoint dataEndpoint = new Endpoint().setAddress("r4/tests-NotReportable-bundle.json")
            .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Set<String> expressions = new HashSet<String>();
        expressions.add("IsReportable");

        Parameters actual = (Parameters)libraryProcessor.evaluate(
                new VersionedIdentifier().withId("RuleFilters").withVersion("1.0.0"), "Patient", "NotReportable", null,
                null, null, endpoint, endpoint, dataEndpoint, null, null, expressions);
        
        assertTrue(compareOutputParameters(expected, actual));
    }
}