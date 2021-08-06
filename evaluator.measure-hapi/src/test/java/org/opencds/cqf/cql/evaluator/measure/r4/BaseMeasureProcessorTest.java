package org.opencds.cqf.cql.evaluator.measure.r4;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.TypedFhirDalFactory;
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
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public abstract class BaseMeasureProcessorTest {

    public BaseMeasureProcessorTest(String bundleName) {
        this.endpoint = new Endpoint().setAddress(bundleName)
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        this.setup();
    }

    protected FhirContext fhirContext = null;
    protected MeasureProcessor measureProcessor = null;
    protected Endpoint endpoint = null;

    protected void validateGroupScore(MeasureReportGroupComponent group, BigDecimal score) {
        assertTrue(group.hasMeasureScore(), String.format("group \"%s\" does not have a score", group.getId()));
        assertEquals(group.getMeasureScore().getValue(), score);
    }

    protected void validateGroup(MeasureReportGroupComponent group, String populationName, int count) {
        Optional<MeasureReportGroupPopulationComponent> population = group.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));
        assertEquals(population.get().getCount(), count, String.format("expected count for population \"%s\" did not match", populationName));
    }

    protected void validateStratifier(MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, String populationName, int count) {
        Optional<StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().hasText() && x.getValue().getText().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        StratifierGroupComponent stratum = stratumOpt.get();
        Optional<StratifierGroupPopulationComponent> population = stratum.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));

        assertEquals(population.get().getCount(), count, String.format("expected count for stratum value \"%s\" population \"%s\" did not match", stratumValue, populationName));
    }

    protected void validateStratumScore(MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, BigDecimal score) {
        Optional<StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().hasText() && x.getValue().getText().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        StratifierGroupComponent stratum = stratumOpt.get();
        assertTrue(stratum.hasMeasureScore(), String.format("stratum \"%s\" does not have a score", stratum.getId()));
        assertEquals(stratum.getMeasureScore().getValue(), score);
    }

    @SuppressWarnings("serial")
    protected void setup() {
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
                                        .parseResource(SimpleMeasureProcessorTest.class.getResourceAsStream(url)),
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
                                .parseResource(SimpleMeasureProcessorTest.class.getResourceAsStream(url)));
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
                                .parseResource(SimpleMeasureProcessorTest.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

                Set<TypedFhirDalFactory> fhirDalFactories = new HashSet<TypedFhirDalFactory>() {
                    {
                        add(new TypedFhirDalFactory() {
                            @Override
                            public String getType() {
                                return Constants.HL7_FHIR_FILES;
                            }
        
                            @Override
                            public FhirDal create(String url, List<String> headers) {
                                return new BundleFhirDal(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(SimpleMeasureProcessorTest.class.getResourceAsStream(url)));
                            }
                        });
                    }
                };

        FhirDalFactory fhirDalFactory = new org.opencds.cqf.cql.evaluator.builder.dal.FhirDalFactory(fhirContext, fhirDalFactories);

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        this.measureProcessor = new MeasureProcessor(terminologyProviderFactory, dataProviderFactory,
                libraryContentProviderFactory, fhirDalFactory, endpointConverter);

    }
}
