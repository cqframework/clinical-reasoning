package org.opencds.cqf.cql.evaluator.builder.it.dstu3;

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
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class DSTU3APIFileIntegrationTests {
    @Test
    public void test_R4APIFileTests() {
        String primaryLibrary = "EXM104_FHIR3";
        String primaryLibraryVersion = "8.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        String terminologyUri = this.getClass().getClassLoader().getResource("terminologyresources/dstu3").getPath().replaceFirst("/", "");
        List<String> libraryUris = new ArrayList<String>();
        String primaryLibraryPath = this.getClass().getClassLoader().getResource("libraryresources/dstu3/library-EXM104_FHIR3-8.1.000.json").getPath().replaceFirst("/", "");
        String fhirHelpersRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/dstu3/library-FHIRHelpers-3.0.0.json").getPath().replaceFirst("/", "");
        String mATGlobalRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/dstu3/library-MATGlobalCommonFunctions_FHIR3-4.0.000.json").getPath().replaceFirst("/", "");
        String supplementalDataRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/dstu3/library-SupplementalDataElements_FHIR3-1.0.0.json").getPath().replaceFirst("/", "");
        String TJCOverallRelatedArtifact = this.getClass().getClassLoader().getResource("libraryresources/dstu3/library-TJCOverall_FHIR3-3.6.000.json").getPath().replaceFirst("/", "");
        libraryUris.add(primaryLibraryPath);
        libraryUris.add(fhirHelpersRelatedArtifact);
        libraryUris.add(mATGlobalRelatedArtifact);
        libraryUris.add(supplementalDataRelatedArtifact);
        libraryUris.add(TJCOverallRelatedArtifact);
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/dstu3").getPath().replaceFirst("/", "");
        Map<String, String> modelUriMap = new HashMap<String, String>();
        modelUriMap.put("http://hl7.org/fhir", dataPath);
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR3");
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        cqlEvaluatorBuilder = cqlEvaluatorBuilder.withLibraryLoader(libraryUris)
                .withFileTerminologyProvider(terminologyUri).withFileDataProvider(modelUriMap);
        CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        assertThat(cqlEvaluatorBuilder.getLibraryLoader(), instanceOf(TranslatingLibraryLoader.class));
        assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));

        String model = "http://hl7.org/fhir";
        String version = "3.0.0";
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getLeft(), version), cqlEvaluatorBuilder.getModels().get(model).getLeft().equals(version));
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getRight(), "null"), cqlEvaluatorBuilder.getModels().get(model).getRight() == null);
    }
}