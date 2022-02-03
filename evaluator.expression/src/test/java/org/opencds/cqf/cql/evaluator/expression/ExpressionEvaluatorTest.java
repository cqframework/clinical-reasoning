package org.opencds.cqf.cql.evaluator.expression;

import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Procedure;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class ExpressionEvaluatorTest {
    FhirContext fhirContext;
    ExpressionEvaluator evaluator;

    private IBaseBundle loadBundle(String path) {
        InputStream stream = ExpressionEvaluatorTest.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (IBaseBundle) resource;
    }
    
    public IBaseBundle readBundle(String path) {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        return loadBundle(path);
    }

    @BeforeClass
    public void setup() {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();

        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);

        @SuppressWarnings("serial")
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

        FhirModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();

        Set<ModelResolverFactory> modelResolverFactories = new HashSet<ModelResolverFactory>() {
            {
                add(fhirModelResolverFactory);
            }
        };

        LibraryContentProviderFactory libraryContentProviderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory(
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

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory()
                .create(fhirContext.getVersion().getVersion());

        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext,
                adapterFactory, fhirTypeConverter);

        evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, libraryContentProviderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());
    }
    @Test
    public void testSimpleExpressionEvaluate() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new IntegerType(4));

        Parameters actual = (Parameters) evaluator.evaluate("1 + 3", null, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testIncludedLibraryExpressionEvaluateWithBundle() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Endpoint endpoint = new Endpoint().setAddress("EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Pair<String, String> library = Pair.of("http://localhost/fhir/Library/EXM125|8.0.000", "EXM125");
        IBaseBundle bundle = readBundle("EXM125-8.0.000-bundle.json");
        Parameters actual = (Parameters) evaluator.evaluate("not \"EXM125\".\"Numerator\"", null, null, Arrays.asList(library), null, bundle, null, endpoint, null, endpoint);
        assertTrue(expected.equalsDeep(actual));
    }
    
    @Test
    public void testIncludedLibraryExpressionEvaluateWithoutBundle() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Endpoint endpoint = new Endpoint().setAddress("EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Pair<String, String> library = Pair.of("http://localhost/fhir/Library/EXM125|8.0.000", "EXM125");
        Parameters actual = (Parameters) evaluator.evaluate("not \"EXM125\".\"Numerator\"", null, null, Arrays.asList(library), null, null, null, endpoint, endpoint, endpoint);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testFhirPathConstantList() {
        Parameters input = new Parameters();
        input.addParameter().setName("%encounters").setResource(new Encounter().setId("1"));
        input.addParameter().setName("%encounters").setResource(new Encounter().setId("2"));


        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new IntegerType(2));

        Parameters actual = (Parameters) evaluator.evaluate("%encounters.count()", input, null, null, null, null, null, null, null, null);
        
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testFhirPathConstant() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounters").setResource(new Encounter().setId("1"));
        ppc.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%encounters"));
        

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new IntegerType(1));

        Parameters actual = (Parameters) evaluator.evaluate("%encounters.count()", input, null, null, null, null, null, null, null, null);
        
        assertTrue(expected.equalsDeep(actual));
    }

    @Test(expectedExceptions = CqlException.class)
    public void testFhirPathConstantMissing() {
        Parameters input = new Parameters();
        input.addParameter().setName("%notUsed").setResource(new Encounter().setId("1"));
        input.addParameter().setName("%notUsed").setResource(new Encounter().setId("2"));

        evaluator.evaluate("%procedures.count()", null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void testEmptyListCount() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounters");
        ppc.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%encounters").setType("Encounter"));
        

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new IntegerType(0));

        Parameters actual = (Parameters) evaluator.evaluate("%encounters.count()", input, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testEmptyListExists() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounters");
        ppc.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("*").setName("%encounters").setType("Encounter"));
        

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Parameters actual = (Parameters) evaluator.evaluate("%encounters.exists()", input, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testNullSingleValueConstant() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounter");
        ppc.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("1").setName("%encounter").setType("Encounter"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(true));

        Parameters actual = (Parameters) evaluator.evaluate("IsNull(%encounter)", input, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testNonNullSingleValueConstant() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounter").setResource(new Encounter().setId("1"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Parameters actual = (Parameters) evaluator.evaluate("IsNull(%encounter)", input, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testMultipleParameters() {
        Parameters input = new Parameters();
        input.addParameter().setName("%encounters").setResource(new Encounter().setId("1"));
        input.addParameter().setName("%encounters").setResource(new Encounter().setId("2"));

        input.addParameter().setName("%procedure").setResource(new Procedure().setId("1"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(true));

        Parameters actual = (Parameters) evaluator.evaluate("%encounters.count() > 1 and not IsNull(%procedure)", input, null, null, null, null, null, null, null, null);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testExpressionParametersOverload() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounter").setResource(new Encounter().setId("1"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Parameters actual = (Parameters) evaluator.evaluate("IsNull(%encounter)", input);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testTypeExtension() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%measurereport");
        ppc.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", new ParameterDefinition().setMax("1").setName("%measurereport").setType("MeasureReport"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new BooleanType(false));

        Parameters actual = (Parameters) evaluator.evaluate("%measurereport.group.select(population).where(code.coding[0].code = 'initial-population' and count > 0).exists()", input);
        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testPropertyAccess() {
        Parameters input = new Parameters();
        ParametersParameterComponent ppc = input.addParameter();
        ppc.setName("%encounter").setResource(new Encounter().setId("1"));

        Parameters expected = new Parameters();
        expected.addParameter().setName("return").setValue(new IdType("1"));

        Parameters actual = (Parameters) evaluator.evaluate("%encounter.id", input);
        assertTrue(expected.equalsDeep(actual));
    }
}
