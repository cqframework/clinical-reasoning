package org.opencds.cqf.cql.evaluator.execution.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

public class DirectoryBundlerTests {

    @Test
    public void test_directoryBundler() {
        FhirContext fhirContext = FhirContext.forR4();
        var directoryBundler = new DirectoryBundler(fhirContext);

        var file = new File("evaluator.execution/src/test/resources/r4/bundleDirectory").getAbsolutePath();

        IBaseBundle bundle = directoryBundler.bundle(file);

        assertNotNull(bundle);

        var resources = BundleUtil.toListOfResourcesOfType(fhirContext, bundle,
                fhirContext.getResourceDefinition("ValueSet").getImplementingClass());

        assertNotNull(resources);
        assertEquals(1, resources.size());

        resources = BundleUtil.toListOfResourcesOfType(fhirContext, bundle,
                fhirContext.getResourceDefinition("Patient").getImplementingClass());
        assertNotNull(resources);
        assertEquals(3, resources.size());
    }
}