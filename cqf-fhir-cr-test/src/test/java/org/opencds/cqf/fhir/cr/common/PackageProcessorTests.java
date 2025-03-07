package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;

@ExtendWith(MockitoExtension.class)
class PackageProcessorTests {
    FhirContext fhirContext = FhirContext.forR4Cached();

    @Mock
    Repository repository;

    PackageProcessor packageProcessor;

    @BeforeEach
    void setup() {
        doReturn(fhirContext).when(repository).fhirContext();
        packageProcessor = new PackageProcessor(repository);
    }

    @Test
    void testPOST() {
        var resource = new PlanDefinition().setId("test");
        var bundle = packageProcessor.packageResource(resource);
        assertNotNull(bundle);
        var entry = (BundleEntryComponent) BundleHelper.getEntryFirstRep(bundle);
        assertEquals(HTTPVerb.POST, entry.getRequest().getMethod());
        assertEquals(resource, BundleHelper.getEntryResourceFirstRep(bundle));
    }

    @Test
    void testPUT() {
        var resource = new PlanDefinition().setId("test");
        var bundle = packageProcessor.packageResource(resource, "PUT");
        assertNotNull(bundle);
        var entry = (BundleEntryComponent) BundleHelper.getEntryFirstRep(bundle);
        assertEquals(HTTPVerb.PUT, entry.getRequest().getMethod());
        assertEquals(resource, BundleHelper.getEntryResourceFirstRep(bundle));
    }
}
