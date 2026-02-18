package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RELEASE;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newCodePart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;
import static org.opencds.cqf.fhir.utility.Parameters.newUrlPart;

import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class LibraryOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testEvaluateLibrary() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-PatientData.json");

        var url = "http://example.org/sdh/dtr/aslp/Library/ASLPDataElements";
        var patientId = "positive";
        var parameters = newParameters(
                getFhirContext(),
                newUrlPart(getFhirContext(), "url", url),
                newStringPart(getFhirContext(), "subject", patientId),
                newPart(
                        getFhirContext(),
                        "parameters",
                        newParameters(
                                getFhirContext(),
                                newStringPart(getFhirContext(), "Service Request Id", "SleepStudy"),
                                newStringPart(getFhirContext(), "Service Request Id", "SleepStudy2"))));
        var result = ourClient
                .operation()
                .onType("Library")
                .named(ProviderConstants.CR_OPERATION_EVALUATE)
                .withParameters(parameters)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(result);
        assertEquals(16, result.getParameter().size());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("Library", "ASLPDataElements"))
                .named(ProviderConstants.CR_OPERATION_DATAREQUIREMENTS)
                .withNoParameters(Parameters.class)
                .returnResourceType(Library.class)
                .execute();
        assertInstanceOf(Library.class, result);
        assertEquals("module-definition", result.getType().getCodingFirstRep().getCode());
    }

    @Test
    void testPackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("Library", "ASLPDataElements"))
                .named(ProviderConstants.CR_OPERATION_PACKAGE)
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }

    @Test
    void testRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        var libraryId = new IdType("Library", "ASLPDataElements");
        var parameters = newParameters(
                getFhirContext(),
                newStringPart(getFhirContext(), "version", "1.0.0"),
                newCodePart(getFhirContext(), "versionBehavior", "default"));
        var result = ourClient
                .operation()
                .onInstance(libraryId)
                .named(CRMI_OPERATION_RELEASE)
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }

    @Test
    @Disabled("This is only useful running locally with a valid apiKey")
    void testManifestRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/uscore-package-bundle.json");
        loadResourceFromPath("org/opencds/cqf/fhir/cr/hapi/r4/Library-Manifest-Partial-Set-FinalDraft-2025.json");

        var terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension("vsacUsername", new StringType("apikey"));
        terminologyEndpoint.addExtension("apiKey", new StringType("API_KEY"));
        terminologyEndpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        terminologyEndpoint.setConnectionType(
                new Coding("http://hl7.org/fhir/ValueSet/endpoint-connection-type", "hl7-fhir-rest", null));
        terminologyEndpoint.setStatus(EndpointStatus.ACTIVE);
        terminologyEndpoint.setPayloadType(Collections.singletonList(
                new CodeableConcept(new Coding("http://hl7.org/fhir/ValueSet/endpoint-payload-type", "any", null))));

        var libraryId = new IdType("Library", "Manifest-Partial-Set-FinalDraft-2025");
        var parameters = newParameters(
                getFhirContext(),
                newStringPart(getFhirContext(), "version", "1.0.0"),
                newCodePart(getFhirContext(), "versionBehavior", "force"),
                newBooleanPart(getFhirContext(), "latestFromTxServer", true),
                newPart(getFhirContext(), "terminologyEndpoint", terminologyEndpoint));
        var result = ourClient
                .operation()
                .onInstance(libraryId)
                .named(CRMI_OPERATION_RELEASE)
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);

        var releaseId = new IdType("Library/Manifest-Partial-Set-FinalDraft-2025");
        var resultRelease = read(releaseId);
        assertNotNull(resultRelease);
        var packageParameters =
                newParameters(getFhirContext(), newPart(getFhirContext(), "terminologyEndpoint", terminologyEndpoint));
        result = ourClient
                .operation()
                .onInstance(releaseId)
                .named(ProviderConstants.CR_OPERATION_PACKAGE)
                .withParameters(packageParameters)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }

    @Test
    void testDeleteLibrary() {
        loadBundle("ersd-small-retired-bundle.json");

        ourClient
                .operation()
                .onInstance("Library/SpecificationLibrary")
                .named("$delete")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();

        Assertions.assertThrows(ResourceGoneException.class, () -> {
            ourClient
                    .read()
                    .resource(Library.class)
                    .withId("SpecificationLibrary")
                    .execute();
        });
    }

    @Test
    void testRetireLibrary() {
        loadBundle("ersd-active-transaction-bundle-example.json");

        ourClient
                .operation()
                .onInstance("Library/SpecificationLibrary")
                .named("$retire")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();

        Library retiredLibrary = ourClient
                .read()
                .resource(Library.class)
                .withId("SpecificationLibrary")
                .execute();
        Assertions.assertEquals(retiredLibrary.getStatus().name(), Enumerations.PublicationStatus.RETIRED.name());
    }

    @Test
    void testWithdrawLibrary() {
        loadBundle("ersd-small-approved-draft-bundle.json");

        ourClient
                .operation()
                .onInstance("Library/SpecificationLibrary")
                .named("$withdraw")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();

        Assertions.assertThrows(ResourceGoneException.class, () -> {
            ourClient
                    .read()
                    .resource(Library.class)
                    .withId("SpecificationLibrary")
                    .execute();
        });
    }

    private List<Parameters.ParametersParameterComponent> getOperationsByType(
            List<Parameters.ParametersParameterComponent> parameters, String type) {
        return parameters.stream()
                .filter(p -> p.getName().equals("operation")
                        && p.getPart().stream()
                                .anyMatch(part -> part.getName().equals("type")
                                        && ((CodeType) part.getValue())
                                                .getCode()
                                                .equals(type)))
                .toList();
    }
}
