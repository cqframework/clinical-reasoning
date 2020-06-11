package org.opencds.cqf.cql.evaluator.builder.it.r4;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderDataContext;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderTerminologyContext;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class R4APIBundleIntegrationTests {
    private FhirContext fhirContext = FhirContext.forR4();
    private TestUtils testUtils = new TestUtils();

    private IBaseBundle loadBundle(FhirContext fhirContext, String path) throws IOException {
        IBaseBundle allNecessaryLibraries = testUtils.loadBundle(fhirContext, path);
        
        if (allNecessaryLibraries == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(allNecessaryLibraries.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        IBaseBundle libraryDepsBundle = (IBaseBundle) allNecessaryLibraries;
        return libraryDepsBundle;
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4APIBundleTests() throws IOException {
        String primaryLibrary = "EXM104";
        String primaryLibraryVersion = "9.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        IBaseBundle terminologyBundle = this.loadBundle(fhirContext, "terminologyresources/DSTU3TerminologyBundle.json");
        IBaseBundle libraries = this.loadBundle(fhirContext, "libraryresources/R4AllNecessaryLibraries.json");
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/r4").getPath().replaceFirst("/", "");
        String model = "http://hl7.org/fhir";
        String version = "4.0.1";
        Map<String, String> modelUriMap = new HashMap<String, String>();
        modelUriMap.put(model, dataPath);
        // Can I Mock this?
        // Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        // models.put(model, Pair.of(version, null));
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no bundle implementation for anything newer or equal to R4 as of now.");
        BuilderTerminologyContext builderTerminologyContext = cqlEvaluatorBuilder.withBundleLibraryLoader(libraries);
        // BuilderDataContext builderDataContext = builderTerminologyContext.withBundleTerminologyProvider(terminologyBundle);
        // cqlEvaluatorBuilder = builderDataContext.withFileDataProvider(modelUriMap);
        // CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        // assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        // assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }
}