package org.opencds.cqf.cql.evaluator.builder.implementations.file;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileDataProviderBuilder;

public class FileDataProviderBuilderTests {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4FileDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("http://hl7.org/fhir", Pair.of("4.0.1", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/r4").getPath().replaceFirst("/", "");
        modelUrlMap.put("http://hl7.org/fhir", dataPath);
        Map<String, DataProvider> r4DataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
        assertThat(r4DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_DSTU3FileDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("http://hl7.org/fhir", Pair.of("3.0.2", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/dstu3").getPath().replaceFirst("/", "");
        modelUrlMap.put("http://hl7.org/fhir", dataPath);
        Map<String, DataProvider> dstu3DataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
        assertThat(dstu3DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_DSTU3NoOpDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("http://hl7.org/fhir", Pair.of("3.0.2", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        modelUrlMap.put("http://hl7.org/fhir", null);
        Map<String, DataProvider> dstu3DataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
        assertThat(dstu3DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_R4NoOpDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("http://hl7.org/fhir", Pair.of("4.0.1", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        modelUrlMap.put("http://hl7.org/fhir", null);
        Map<String, DataProvider> r4DataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
        assertThat(r4DataProviderMap.get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_QDMFileDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("urn:healthit-gov:qdm:v5_4", Pair.of("5.0.4", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/dstu3").getPath().replaceFirst("/", "");
        modelUrlMap.put("urn:healthit-gov:qdm:v5_4", dataPath);
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("QDM data providers are not yet implemented");
        Map<String, DataProvider> qdmDataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
    }

    @Test
    public void test_UnknownFileDataProviderBuilder() throws URISyntaxException {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(null);
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        models.put("Unknown", Pair.of("5.0.4", null));
        Map<String, String> modelUrlMap = new HashMap<String, String>();
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/dstu3").getPath().replaceFirst("/", "");
        modelUrlMap.put("Unknown", dataPath);
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Unknown data provider uri: Unknown");
        Map<String, DataProvider> unknownDataProviderMap = fileDataProviderBuilder.build(models, modelUrlMap);
    }
}