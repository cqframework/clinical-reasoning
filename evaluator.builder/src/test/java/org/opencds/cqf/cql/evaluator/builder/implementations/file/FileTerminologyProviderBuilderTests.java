package org.opencds.cqf.cql.evaluator.builder.implementations.file;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;

public class FileTerminologyProviderBuilderTests {
    private TerminologyProvider getTerminologyProvider(String model, String version, String libraryUri) {
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put(model, Pair.of(version, null));
        FileTerminologyProviderBuilder fileTerminologyProviderBuilder = new FileTerminologyProviderBuilder();
        TerminologyProvider terminologyProvider = fileTerminologyProviderBuilder.build(models, libraryUri);
        return terminologyProvider;
    }

    @Test
    public void test_R4FileTerminologyProviderBuilder() {
        String model = "http://hl7.org/fhir";
        String version = "4.0.1";
        String terminologyUri = this.getClass().getClassLoader().getResource("terminologyresources/r4").getPath().replaceFirst("/", "");
        TerminologyProvider terminologyProvider = getTerminologyProvider(model, version, terminologyUri);
        assertThat(terminologyProvider, instanceOf(BundleTerminologyProvider.class));
    }

    @Test
    public void test_DSTU3FileTerminologyProviderBuilder() {
        String model = "http://hl7.org/fhir";
        String version = "3.0.2";
        String terminologyUri = this.getClass().getClassLoader().getResource("terminologyresources/dstu3").getPath().replaceFirst("/", "");
        TerminologyProvider terminologyProvider = getTerminologyProvider(model, version, terminologyUri);
        assertThat(terminologyProvider, instanceOf(BundleTerminologyProvider.class));
    }
}