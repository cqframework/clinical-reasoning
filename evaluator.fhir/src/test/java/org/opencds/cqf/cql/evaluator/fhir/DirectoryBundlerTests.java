package org.opencds.cqf.cql.evaluator.fhir;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.util.BundleUtil;

public class DirectoryBundlerTests {

    @Test
    public void test_directoryBundler() {

        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        DirectoryBundler bundler = new DirectoryBundler(fhirContext);

        String file = new File("src/test/resources/r4/bundleDirectory").getAbsolutePath();

        IBaseBundle bundle = bundler.bundle(file);

        assertNotNull(bundle);

        List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(fhirContext, bundle,
                fhirContext.getResourceDefinition("ValueSet").getImplementingClass());

        assertNotNull(resources);
        assertEquals(1, resources.size());

        resources = BundleUtil.toListOfResourcesOfType(fhirContext, bundle,
                fhirContext.getResourceDefinition("Patient").getImplementingClass());
        assertNotNull(resources);
        assertEquals(3, resources.size());
    }
}