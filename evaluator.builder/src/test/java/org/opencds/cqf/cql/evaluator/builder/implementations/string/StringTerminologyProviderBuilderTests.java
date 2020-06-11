package org.opencds.cqf.cql.evaluator.builder.implementations.string;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringTerminologyProviderBuilder;
import org.apache.commons.lang3.NotImplementedException;

public class StringTerminologyProviderBuilderTests {
    private TerminologyProvider getStringTerminologyProvider(String model, String version, String terminologyString) {
        StringTerminologyProviderBuilder stringTerminologyProviderBuilder = new StringTerminologyProviderBuilder();
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put(model, Pair.of(version, null));
        List<String> terminologyBundles = new ArrayList<String>();
        terminologyBundles.add(terminologyString);
        TerminologyProvider terminologyProvider = stringTerminologyProviderBuilder.build(models, terminologyBundles);
        return terminologyProvider;
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4StringTerminologyProvider() throws IOException, URISyntaxException {
        TestUtils testUtils = new TestUtils();
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("String Representations of Terminology Bundles is not yet supported.");
        getStringTerminologyProvider("http://hl7.org/fhir", "4.0.1", testUtils.loadString("terminologyresources/TerminologyBundle.json"));
    }
}