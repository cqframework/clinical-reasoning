package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ReleaseProviderIT extends BaseCrR4TestServer {

    private static final String SPEC_LIB_REF = "Library/SpecificationLibrary";

    public Bundle callRelease(String id, String version, String versionBehavior, String requireNonExperimental) {
        var parametersEval = new Parameters();
        parametersEval.addParameter("version", version == null ? null : new StringType(version));
        parametersEval.addParameter("versionBehavior", version == null ? null : new CodeType(versionBehavior));
        parametersEval.addParameter(
                "requireNonExperimental", version == null ? null : new CodeType(requireNonExperimental));

        return ourClient
                .operation()
                .onInstance(id)
                .named("$release")
                .withParameters(parametersEval)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void releaseResource_test() {
        loadBundle("ersd-release-bundle.json");
        readAndLoadResource("artifactAssessment-search-parameter.json");
        var existingVersion = "1.2.3";
        var versionData = "1.2.7";

        var returnResource = callRelease("Library/ReleaseSpecificationLibrary", versionData, "default", null);

        assertNotNull(returnResource);
        var maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        var releasedLibrary = ourClient.fetchResourceFromUrl(
                Library.class, maybeLib.get().getResponse().getLocation());
        // versionBehaviour == 'default' so version should be
        // existingVersion and not the new version provided in
        // the parameters
        assertEquals(existingVersion, releasedLibrary.getVersion());
        var ersdTestArtifactDependencies = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-dxtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-ostc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-lotc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-lrtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-mrtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-sdtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-sdltc",
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-sdmtc",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1063|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.360|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.120|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.362|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.528|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.408|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.409|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1469|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1866|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1906|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.480|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.481|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.761|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1223|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1182|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1181|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1184|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1601|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1600|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1603|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1602|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1082|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1439|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1436|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1435|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1446|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438|2022-10-19",
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
                "http://notOwnedTest.com/Library/notOwnedLeaf|0.1.1",
                "http://notOwnedTest.com/Library/notOwnedLeaf1|0.1.1",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/eicr-document-bundle",
                "http://hl7.org/fhir/StructureDefinition/ServiceRequest",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset-library",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset-library",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-plandefinition",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset",
                "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset",
                "http://www.nlm.nih.gov/research/umls/rxnorm",
                "http://loinc.org",
                "http://hl7.org/fhir/sid/icd-10-cm",
                "http://snomed.info/sct");
        var ersdTestArtifactComponents = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1");
        var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .map(RelatedArtifact::getResource)
                .toList();
        var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .map(RelatedArtifact::getResource)
                .toList();
        // check that the released artifact has all the required dependencies
        for (var dependency : ersdTestArtifactDependencies) {
            assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
        }
        // and components
        for (var component : ersdTestArtifactComponents) {
            assertTrue(componentsOnReleasedArtifact.contains(component));
        }
        // has extra groupers and rctc dependencies
        assertEquals(ersdTestArtifactDependencies.size(), dependenciesOnReleasedArtifact.size());
        assertEquals(ersdTestArtifactComponents.size(), componentsOnReleasedArtifact.size());
    }

    @Test
    void releaseResource_force_version() {
        loadBundle("ersd-small-approved-draft-bundle.json");
        readAndLoadResource("artifactAssessment-search-parameter.json");
        // Existing version should be "1.2.3";
        String newVersionToForce = "1.2.7";

        var returnResource = callRelease(SPEC_LIB_REF, newVersionToForce, "force", null);

        assertNotNull(returnResource);
        var maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains(SPEC_LIB_REF))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary = ourClient.fetchResourceFromUrl(
                Library.class, maybeLib.get().getResponse().getLocation());
        assertEquals(newVersionToForce, releasedLibrary.getVersion());
    }

    @Test
    void releaseResource_require_non_experimental_error() {
        // SpecificationLibrary - root is experimental but HAS experimental children
        loadBundle("ersd-small-approved-draft-experimental-bundle.json");
        // SpecificationLibrary2 - root is NOT experimental but HAS experimental children
        loadBundle("ersd-small-approved-draft-non-experimental-with-experimental-comp-bundle.json");

        readAndLoadResource("artifactAssessment-search-parameter.json");

        var version = "1.2.3";
        var versionBehavior = "default";
        var requireNonExperimental = "error";

        Exception notExpectingAnyException = null;
        // no Exception if root is experimental
        try {
            callRelease(SPEC_LIB_REF, version, versionBehavior, requireNonExperimental);
        } catch (Exception e) {
            notExpectingAnyException = e;
        }
        assertNull(notExpectingAnyException);

        UnprocessableEntityException nonExperimentalChildException = null;
        try {
            callRelease(SPEC_LIB_REF + "2", version, versionBehavior, requireNonExperimental);
        } catch (UnprocessableEntityException e) {
            nonExperimentalChildException = e;
        }
        assertNotNull(nonExperimentalChildException);
        assertTrue(nonExperimentalChildException.getMessage().contains("not Experimental"));
    }
}
