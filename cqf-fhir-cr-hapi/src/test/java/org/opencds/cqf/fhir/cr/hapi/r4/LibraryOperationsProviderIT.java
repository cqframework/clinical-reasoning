package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
                "Library/ASLPDataElements", null, null, null, null, null, null, null, null, null, requestDetails);
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
    void artifact_diff_compare_computable() {
        loadBundle("ersd-small-active-bundle.json");
        loadBundle("ersd-small-drafted-bundle.json");
        Parameters diffParams = new Parameters();
        diffParams.addParameter("source", "Library/SpecificationLibrary");
        diffParams.addParameter("target", "Library/DraftSpecificationLibrary");
        diffParams.addParameter("compareComputable", new BooleanType(true));
        diffParams.addParameter("compareExecutable", new BooleanType(false));
        // we don't need a terminology endpoint if compareExecutable == false because no
        // valuesets need to be expanded
        Parameters returnedParams = ourClient
                .operation()
                .onType(Library.class)
                .named("$artifact-diff")
                .withParameters(diffParams)
                .returnResourceType(Parameters.class)
                .execute();
        List<Parameters> nestedChanges = returnedParams.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = returnedParams.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");
        // delete 2 leafs and extensions
        assertEquals(5, deleteOperations.size());
        // there aren't actually 2 operations here
        assertEquals(2, insertOperations.size());
        String path1 = insertOperations.get(0).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        String path2 = insertOperations.get(1).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        // insert the new leaf; adding a node takes multiple operations if
        // the thing being added isn't a defined complex FHIR type
        assertEquals("ValueSet.compose.include", path1);
        assertEquals("ValueSet.compose.include[1].valueSet", path2);
    }

    @Test
    void artifact_diff_compare_executable() {
        loadBundle("ersd-small-active-bundle.json");
        loadBundle("ersd-small-drafted-bundle.json");
        var diffParams = new Parameters();
        diffParams.addParameter("source", "Library/SpecificationLibrary");
        diffParams.addParameter("target", "Library/DraftSpecificationLibrary");
        diffParams.addParameter("compareExecutable", new BooleanType(true));
        diffParams.addParameter("compareComputable", new BooleanType(false));
        Parameters returnedParams = ourClient
                .operation()
                .onType(Library.class)
                .named("$artifact-diff")
                .withParameters(diffParams)
                .returnResourceType(Parameters.class)
                .execute();
        List<Parameters> nestedChanges = returnedParams.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = returnedParams.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");
        // old codes removed
        assertEquals(33, deleteOperations.size());
        // new codes added
        assertEquals(40, insertOperations.size());
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
                .collect(Collectors.toList());
    }
}
