package org.opencds.cqf.cql.evaluator.builder.implementations.string;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringDataProviderBuilder;

public class StringDataProviderBuilderTests {
    
    private Map<String, DataProvider> getStringFHIRDataProvider(String model, String version, String dataString) {
        StringDataProviderBuilder stringDataProviderBuilder = new StringDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put(model, Pair.of(version, null));
        List<String> dataBundles = new ArrayList<String>();
        dataBundles.add(dataString);
        Map<String, DataProvider> dataProviders = stringDataProviderBuilder.build(models, dataBundles);
        return dataProviders;
    }
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4StringDataProvider() throws IOException, URISyntaxException {
        TestUtils testUtils = new TestUtils();
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("String Representations of Data Bundles is not yet supported.");
        getStringFHIRDataProvider("http://hl7.org/fhir", "4.0.1", testUtils.loadString("dataresources/DataBundle.json"));
    }    
}