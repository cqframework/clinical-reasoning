package org.opencds.cqf.cql.evaluator.builder.implementations.file;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.junit.Test;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

public class FileLibraryLoaderBuilderTests {
    private Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
    @Test
    public void test_R4FileLibraryLoaderBuilder() {
        FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
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
        LibraryLoader libraryLoader = fileLibraryLoaderBuilder.build(libraryUris, models, CqlTranslatorOptions.defaultOptions());
        assertThat(libraryLoader, instanceOf(TranslatingLibraryLoader.class));
    }

    @Test
    public void test_DSTU3FileLibraryLoaderBuilder() {
        FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
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
        LibraryLoader libraryLoader = fileLibraryLoaderBuilder.build(libraryUris, models, CqlTranslatorOptions.defaultOptions());
        assertThat(libraryLoader, instanceOf(TranslatingLibraryLoader.class));
    }
}