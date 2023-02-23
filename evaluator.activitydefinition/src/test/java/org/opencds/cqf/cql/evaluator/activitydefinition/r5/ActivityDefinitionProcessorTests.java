package org.opencds.cqf.cql.evaluator.activitydefinition.r5;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.Assert;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class ActivityDefinitionProcessorTests {
    private static FhirContext fhirContext;
    private ActivityDefinitionProcessor activityDefinitionProcessor;

    /* Commenting this out until we have a ModelResolver for R5 */
    // @BeforeClass
    public void setup() {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R5);
        FhirDal fhirDal = new MockFhirDal();
        AdapterFactory adapterFactory =
                new org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory();
        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
        FhirTypeConverter fhirTypeConverter =
                new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        CqlFhirParametersConverter cqlFhirParametersConverter =
                new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);
        Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = new HashSet<>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add(new TypedLibrarySourceProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public LibrarySourceProvider create(String url, List<String> headers) {
                        return new BundleFhirLibrarySourceProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(ActivityDefinitionProcessorTests.class
                                                .getResourceAsStream(url)),
                                adapterFactory, libraryVersionSelector);
                    }
                });
            }
        };

        ModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();

        Set<ModelResolverFactory> modelResolverFactories = new HashSet<>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add(fhirModelResolverFactory);
            }
        };

        LibrarySourceProviderFactory libraryLoaderFactory =
                new org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory(
                        fhirContext, adapterFactory, librarySourceProviderFactories,
                        libraryVersionSelector);
        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = new HashSet<>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add(new TypedRetrieveProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public RetrieveProvider create(String url, List<String> headers) {

                        return new BundleRetrieveProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(ActivityDefinitionProcessorTests.class
                                                .getResourceAsStream(url)));
                    }
                });
            }
        };

        DataProviderFactory dataProviderFactory =
                new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(fhirContext,
                        modelResolverFactories, retrieveProviderFactories);

        Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = new HashSet<>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add(new TypedTerminologyProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public TerminologyProvider create(String url, List<String> headers) {
                        return new BundleTerminologyProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(ActivityDefinitionProcessorTests.class
                                                .getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory =
                new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
                        fhirContext, typedTerminologyProviderFactories);

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        LibraryProcessor libraryProcessor =
                new LibraryProcessor(fhirContext, cqlFhirParametersConverter, libraryLoaderFactory,
                        dataProviderFactory, terminologyProviderFactory, endpointConverter,
                        fhirModelResolverFactory, CqlEvaluatorBuilder::new);

        activityDefinitionProcessor =
                new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
    }

    @Test
    public void testActivityDefinitionApply() throws FHIRException {
        Assert.assertTrue(true);
        /* Commenting this out until we have a ModelResolver for R5 */
        // Endpoint contentEndpoint = new
        // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
        // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        // Endpoint terminologyEndpoint = new
        // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
        // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        // Endpoint dataEndpoint = new
        // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
        // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        // Object result = this.activityDefinitionProcessor.apply(new
        // IdType("activityDefinition-test"), "patient-1", null, null, null, null, null,
        // null, null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint);
        // Assert.assertTrue(result instanceof MedicationRequest);
        // MedicationRequest request = (MedicationRequest) result;
        // Assert.assertTrue(request.getDoNotPerform());
    }

}
