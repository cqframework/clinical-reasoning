package org.opencds.cqf.cql.evaluator.builder.implementations.bundle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleDataProviderBuilder;

import ca.uhn.fhir.context.FhirContext;

public class BundleDataProviderBuilderTests {
    private TestUtils testUtils = new TestUtils();
    
    private Map<String, DataProvider> getBundleDataProvider(FhirContext fhirContext, String dataBundlePath) {
        BundleDataProviderBuilder bundleDataProviderBuilder = new BundleDataProviderBuilder(null);
        IBaseBundle dataBundle = testUtils.loadBundle(fhirContext, "dataresources/DataBundle.json");
        Map<String, DataProvider> dataProviders = bundleDataProviderBuilder.build(dataBundle);
        return dataProviders;
    }
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    // @Test
    // public void test_R5StringDataProvider() throws IOException, URISyntaxException {
    //     exceptionRule.expect(NotImplementedException.class);
    //     exceptionRule.expectMessage("Sorry there is no implementation for anything newer than or equal to R5 as of now.");
    //     Map<String, DataProvider> r5DataProviderMap = getBundleDataProvider(FhirContext.forR5(), "dataresources/R5DataBundle.json");
    // }   

    @Test
    public void test_R4StringDataProvider() throws IOException, URISyntaxException {
        Map<String, DataProvider> r4DataProviderMap = getBundleDataProvider(FhirContext.forR4(), "dataresources/R4DataBundle.json");
        assertThat(r4DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }   

    @Test
    public void test_DSTU3StringDataProvider() throws IOException, URISyntaxException {
        Map<String, DataProvider> dstu3DataProviderMap = getBundleDataProvider(FhirContext.forDstu3(), "dataresources/DSTU3DataBundle.json");
        assertThat(dstu3DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }   

    // @Test
    // public void test_DSTU2StringDataProvider() throws IOException, URISyntaxException {
    //     Map<String, DataProvider> dstu2DataProviderMap = getBundleDataProvider(FhirContext.forDstu2(), "dataresources/DSTU2DataBundle.json");
    //     assertThat(dstu2DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    // }   
}