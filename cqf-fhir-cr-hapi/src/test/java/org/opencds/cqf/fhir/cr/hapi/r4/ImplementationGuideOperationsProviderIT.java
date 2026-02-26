package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RELEASE;
import static org.opencds.cqf.fhir.utility.Parameters.newCodePart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;

public class ImplementationGuideOperationsProviderIT extends BaseCrR4TestServer {
    private static final String IG_PATH =
            "org/opencds/cqf/fhir/cr/hapi/r4/igs/ImplementationGuide-hl7.fhir.us.core-6-1-0.json";
    private static final String IG_ID = "hl7.fhir.us.core";

    @Test
    void testDataRequirementsGet() {
        loadResourceFromPath(IG_PATH);
        var result = ourClient
                .operation()
                .onInstance(new IdType("ImplementationGuide", IG_ID))
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withNoParameters(Parameters.class)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testDataRequirementsPost() {
        loadResourceFromPath(IG_PATH);
        var parameters = newParameters(getFhirContext(), newStringPart(getFhirContext(), "id", IG_ID));
        var result = ourClient
                .operation()
                .onType("ImplementationGuide")
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withParameters(parameters)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testDataRequirementsGetWithTerminologyEndpoint() {
        loadResourceFromPath(IG_PATH);
        var terminologyEndpoint = new Endpoint();
        terminologyEndpoint.setAddress("https://tx.example.org/fhir");
        var parameters =
                newParameters(getFhirContext(), newPart(getFhirContext(), "terminologyEndpoint", terminologyEndpoint));
        var result = ourClient
                .operation()
                .onInstance(new IdType("ImplementationGuide", IG_ID))
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withParameters(parameters)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testDataRequirementsPostWithArtifactEndpointConfiguration() {
        loadResourceFromPath(IG_PATH);
        var parameters = new Parameters();
        parameters.addParameter().setName("id").setValue(new org.hl7.fhir.r4.model.StringType(IG_ID));

        var config = parameters.addParameter().setName("artifactEndpointConfiguration");
        config.addPart().setName("artifactRoute").setValue(new UriType("https://example.org/fhir"));
        config.addPart().setName("endpointUri").setValue(new UriType("https://example.org/fhir/terminology"));

        var terminologyEndpoint = new Endpoint();
        terminologyEndpoint.setAddress("https://tx.example.org/fhir");
        parameters.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);

        var result = ourClient
                .operation()
                .onType("ImplementationGuide")
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withParameters(parameters)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/uscore-package-bundle.json");
        loadResourceFromPath("org/opencds/cqf/fhir/cr/hapi/r4/igs/ImplementationGuide-hl7.fhir.us.core-6-1-0.json");
        var id = new IdType("ImplementationGuide", "hl7.fhir.us.core");
        var parameters = newParameters(
                getFhirContext(),
                newStringPart(getFhirContext(), "version", "1.0.0"),
                newCodePart(getFhirContext(), "versionBehavior", "default"));
        var result = ourClient
                .operation()
                .onInstance(id)
                .named(CRMI_OPERATION_RELEASE)
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }
}
