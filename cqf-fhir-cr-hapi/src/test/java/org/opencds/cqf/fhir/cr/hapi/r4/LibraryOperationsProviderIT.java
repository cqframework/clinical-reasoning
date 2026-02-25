package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
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
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryEvaluateProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryReleaseProvider;
import org.springframework.beans.factory.annotation.Autowired;

class LibraryOperationsProviderIT extends BaseCrR4TestServer {
    @Autowired
    LibraryEvaluateProvider libraryEvaluateProvider;

    @Autowired
    LibraryDataRequirementsProvider libraryDataRequirementsProvider;

    @Autowired
    LibraryPackageProvider libraryPackageProvider;

    @Autowired
    LibraryReleaseProvider libraryReleaseProvider;

    @Test
    void testEvaluateLibrary() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-PatientData.json");

        var requestDetails = setupRequestDetails();
        var url = "http://example.org/sdh/dtr/aslp/Library/ASLPDataElements";
        var patientId = "positive";
        var parameters = new Parameters()
                .addParameter("Service Request Id", "SleepStudy")
                .addParameter("Service Request Id", "SleepStudy2");
        var result = libraryEvaluateProvider.evaluate(
                url, patientId, null, parameters, new BooleanType(true), null, null, null, null, null, requestDetails);

        assertNotNull(result);
        assertEquals(16, result.getParameter().size());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var requestDetails = setupRequestDetails();
        var result = libraryDataRequirementsProvider.getDataRequirements(
                "Library/ASLPDataElements", null, null, null, requestDetails);
        assertInstanceOf(Library.class, result);
        assertEquals(
                "module-definition",
                ((Library) result).getType().getCodingFirstRep().getCode());
    }

    @Test
    void testPackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var requestDetails = setupRequestDetails();
        var result = libraryPackageProvider.packageLibrary(
                "Library/ASLPDataElements", null, null, null, null, null, null, null, null, null, null, requestDetails);
        assertInstanceOf(Bundle.class, result);
    }

    @Test
    void testRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        var requestDetails = setupRequestDetails();
        var result = libraryReleaseProvider.releaseLibrary(
                "Library/ASLPDataElements", "1.0.0", new CodeType("default"), null, null, null, null, requestDetails);
        assertInstanceOf(Bundle.class, result);
    }

    @Test
    @Disabled("This is only useful running locally with a valid apiKey")
    void testManifestRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/uscore-package-bundle.json");
        loadResourceFromPath("org/opencds/cqf/fhir/cr/hapi/r4/Library-Manifest-Partial-Set-FinalDraft-2025.json");
        var requestDetails = setupRequestDetails();

        var terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension("vsacUsername", new StringType("apikey"));
        terminologyEndpoint.addExtension("apiKey", new StringType("API_KEY"));
        terminologyEndpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        terminologyEndpoint.setConnectionType(
                new Coding("http://hl7.org/fhir/ValueSet/endpoint-connection-type", "hl7-fhir-rest", null));
        terminologyEndpoint.setStatus(EndpointStatus.ACTIVE);
        terminologyEndpoint.setPayloadType(Collections.singletonList(
                new CodeableConcept(new Coding("http://hl7.org/fhir/ValueSet/endpoint-payload-type", "any", null))));

        var result = libraryReleaseProvider.releaseLibrary(
                "Library/Manifest-Partial-Set-FinalDraft-2025",
                "1.0.0",
                new CodeType("force"),
                new BooleanType(true),
                null,
                terminologyEndpoint,
                null,
                requestDetails);
        assertInstanceOf(Bundle.class, result);

        var resultRelease = read(new IdType("Library/Manifest-Partial-Set-FinalDraft-2025"));

        var terminologyEndpointParam = new Parameters.ParametersParameterComponent();
        terminologyEndpointParam.setName("terminologyEndpoint");
        terminologyEndpointParam.setResource(terminologyEndpoint);
        result = libraryPackageProvider.packageLibrary(
                "Library/Manifest-Partial-Set-FinalDraft-2025",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                terminologyEndpointParam,
                null,
                requestDetails);
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
