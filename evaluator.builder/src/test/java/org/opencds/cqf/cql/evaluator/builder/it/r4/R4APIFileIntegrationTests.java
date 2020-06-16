package org.opencds.cqf.cql.evaluator.builder.it.r4;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.junit.Test;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class R4APIFileIntegrationTests {
    @Test
    public void test_R4APIFileTests() {
        String primaryLibrary = "EXM104";
        String primaryLibraryVersion = "9.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        String terminologyUri = this.getClass().getClassLoader().getResource("terminologyresources/r4").getPath().replaceFirst("/", "");
        List<String> libraryUris = new ArrayList<String>();
        String primaryLibraryPath = this.getClass().getClassLoader().getResource("libraryresources/r4/library-EXM104-9.1.000.json").getPath().replaceFirst("/", "");
        String fhirHelpersRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/r4/library-FHIRHelpers-4.0.1.json").getPath().replaceFirst("/", "");
        String mATGlobalRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/r4/library-MATGlobalCommonFunctions-5.0.000.json").getPath().replaceFirst("/", "");
        String supplementalDataRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/r4/library-SupplementalDataElements-2.0.0.json").getPath().replaceFirst("/", "");
        String TJCOverallRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/r4/library-TJCOverall-5.0.000.json").getPath().replaceFirst("/", "");
        libraryUris.add(primaryLibraryPath);
        libraryUris.add(fhirHelpersRelatedArtifact);
        libraryUris.add(mATGlobalRelatedArtifact);
        libraryUris.add(supplementalDataRelatedArtifact);
        libraryUris.add(TJCOverallRelatedArtifact);
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/r4").getPath().replaceFirst("/", "");
        Map<String, String> modelUriMap = new HashMap<String, String>();
        modelUriMap.put("http://hl7.org/fhir", dataPath);
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR4");
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        cqlEvaluatorBuilder = cqlEvaluatorBuilder.withFileLibraryLoader(libraryUris)
                .withFileTerminologyProvider(terminologyUri).withFileDataProvider(modelUriMap);
        CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        assertThat(cqlEvaluatorBuilder.getLibraryLoader(), instanceOf(TranslatingLibraryLoader.class));
        assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));

        // cqlEvaluator.evaluate(contextParameter);

        String model = "http://hl7.org/fhir";
        String version = "4.0.1";
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getLeft(), version), cqlEvaluatorBuilder.getModels().get(model).getLeft().equals(version));
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getRight(), "null"), cqlEvaluatorBuilder.getModels().get(model).getRight() == null);
    }
}