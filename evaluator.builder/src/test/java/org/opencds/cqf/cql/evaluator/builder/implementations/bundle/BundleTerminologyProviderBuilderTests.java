package org.opencds.cqf.cql.evaluator.builder.implementations.bundle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

import org.apache.commons.lang3.NotImplementedException;

public class BundleTerminologyProviderBuilderTests {
    private TestUtils testUtils = new TestUtils();

    private TerminologyProvider getBundleTerminologyProvider(String model, String version, IBaseBundle terminologyBundle) {
        BundleTerminologyProviderBuilder bundleTerminologyProviderBuilder = new BundleTerminologyProviderBuilder();
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put(model, Pair.of(version, null));
        TerminologyProvider terminologyProvider = bundleTerminologyProviderBuilder.build(models, terminologyBundle);
        return terminologyProvider;
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4BundleTerminologyProvider() throws IOException, URISyntaxException {
        FhirContext fhirContext = FhirContext.forR4();
        TerminologyProvider r4TerminologyProvider = getBundleTerminologyProvider("http://hl7.org/fhir", "4.0.1", testUtils.loadBundle(fhirContext, "terminologyresources/R4TerminologyBundle.json"));
        assertThat(r4TerminologyProvider, instanceOf(BundleTerminologyProvider.class));
    }

    @Test
    public void test_DSTU3BundleTerminologyProvider() throws IOException, URISyntaxException {
        FhirContext fhirContext = FhirContext.forDstu3();
        TerminologyProvider r4TerminologyProvider = getBundleTerminologyProvider("http://hl7.org/fhir", "3.0.2", testUtils.loadBundle(fhirContext, "terminologyresources/DSTU3TerminologyBundle.json"));
        assertThat(r4TerminologyProvider, instanceOf(BundleTerminologyProvider.class));
    }

    @Test
    public void test_ModelOtherThanFhirBundleTerminologyProvider() throws IOException, URISyntaxException {
        FhirContext fhirContext = FhirContext.forDstu3();
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("We currently only support FHIR-based terminology, Unknown Model: [QDM model]");
        TerminologyProvider r4TerminologyProvider = getBundleTerminologyProvider("QDM model", "3.0.2", testUtils.loadBundle(fhirContext, "terminologyresources/DSTU3TerminologyBundle.json"));
    }
}