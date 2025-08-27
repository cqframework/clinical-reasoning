package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DraftProviderIT extends BaseCrR4TestServer {

    private final String RELEASE_LABEL_URL = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
    private final String RELEASE_DESCRIPTION_URL =
            "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
    private final String SPEC_LIB_ID = "SpecificationLibrary";
    private final String SPEC_LIB_REF = "Library/" + SPEC_LIB_ID;

    private final List<String> badVersionList = Arrays.asList(
            "11asd1",
            "1.1.3.1",
            "1.|1.1",
            "1/.1.1",
            "-1.-1.2.1",
            "1.-1.2.1",
            "1.1.-2.1",
            "7.1..21",
            "1.2.1-draft",
            "1.2.3-draft",
            "3.2",
            "1.",
            "3.ad.2.",
            "",
            null);

    public Bundle callDraft(String id, String version) {
        var parametersEval = new Parameters();
        parametersEval.addParameter("version", version == null ? null : new StringType(version));

        return ourClient
                .operation()
                .onInstance(id)
                .named("$draft")
                .withParameters(parametersEval)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void draftOperation_test() {
        loadBundle("ersd-active-transaction-bundle-example.json");
        Library baseLib =
                ourClient.read().resource(Library.class).withId(SPEC_LIB_ID).execute();
        // Root Artifact must have approval date, releaseLabel and releaseDescription for this test
        assertTrue(baseLib.hasApprovalDate());
        assertTrue(baseLib.hasExtension(RELEASE_DESCRIPTION_URL));
        assertTrue(baseLib.hasExtension(RELEASE_LABEL_URL));
        assertTrue(baseLib.hasApprovalDate());
        String version = "1.0.1";
        String draftedVersion = version + "-draft";

        Bundle returnedBundle = callDraft(SPEC_LIB_REF, version);

        assertNotNull(returnedBundle);
        assertEquals(9, returnedBundle.getEntry().size());
        Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findAny();
        assertTrue(maybeLib.isPresent());
        Library lib = ourClient.fetchResourceFromUrl(
                Library.class, maybeLib.get().getResponse().getLocation());
        assertNotNull(lib);
        assertSame(Enumerations.PublicationStatus.DRAFT, lib.getStatus());
        assertEquals(draftedVersion, lib.getVersion());
        assertFalse(lib.hasApprovalDate());
        assertFalse(lib.hasExtension(RELEASE_DESCRIPTION_URL));
        assertFalse(lib.hasExtension(RELEASE_LABEL_URL));
        List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
        assertFalse(relatedArtifacts.isEmpty());
        forEachMetadataResource(returnedBundle.getEntry(), resource -> {
            List<RelatedArtifact> relatedArtifacts2 = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                    .createKnowledgeArtifactAdapter(resource)
                    .getRelatedArtifact();
            if (relatedArtifacts2 != null && !relatedArtifacts2.isEmpty()) {
                for (RelatedArtifact relatedArtifact : relatedArtifacts2) {
                    if (IKnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(relatedArtifact)) {
                        assertEquals(draftedVersion, Canonicals.getVersion(relatedArtifact.getResource()));
                    }
                }
            }
        });
    }

    @Test
    void draftOperation_no_effectivePeriod_test() {
        loadBundle("ersd-active-transaction-bundle-example.json");
        Library baseLib =
                ourClient.read().resource(Library.class).withId(SPEC_LIB_ID).execute();
        assertTrue(baseLib.hasEffectivePeriod());
        PlanDefinition planDef = ourClient
                .read()
                .resource(PlanDefinition.class)
                .withId("plandefinition-ersd-instance-example")
                .execute();
        assertTrue(planDef.hasEffectivePeriod());

        Bundle returnedBundle = callDraft(SPEC_LIB_REF, "1.01.21");

        forEachMetadataResource(returnedBundle.getEntry(), resource -> {
            var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(resource);
            assertFalse(((Period) adapter.getEffectivePeriod()).hasStart()
                    || ((Period) adapter.getEffectivePeriod()).hasEnd());
        });
    }

    @Test
    void draftOperation_version_conflict_test() {
        loadBundle("ersd-active-transaction-bundle-example.json");
        loadResourceFromPath("minimal-draft-to-test-version-conflict.json");
        String maybeException = null;
        try {
            callDraft(SPEC_LIB_REF, "1.0.0");
        } catch (Exception e) {
            maybeException = e.getMessage();
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.contains("already exists"));
    }

    @Test
    void draftOperation_cannot_create_draft_of_draft_test() {
        loadResourceFromPath("minimal-draft-to-test-version-conflict.json");
        String maybeException = null;
        try {
            callDraft(SPEC_LIB_REF, "1.2.1");
        } catch (Exception e) {
            maybeException = e.getMessage();
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.contains("status of 'active'"));
    }

    @Test
    void draftOperation_wrong_id_test() {
        loadBundle("ersd-draft-transaction-bundle-example.json");
        ResourceNotFoundException maybeException = null;
        try {
            callDraft(SPEC_LIB_REF, "1.3.1");
        } catch (ResourceNotFoundException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
    }

    @Test
    void draftOperation_version_format_test() {
        loadResourceFromPath("minimal-draft-to-test-version-conflict.json");
        for (String version : badVersionList) {
            UnprocessableEntityException maybeException = null;
            try {
                callDraft(SPEC_LIB_REF, version);
            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }

    private void forEachMetadataResource(
            List<Bundle.BundleEntryComponent> entries, Consumer<MetadataResource> callback) {
        entries.stream()
                .map(entry -> entry.getResponse().getLocation())
                .map(location -> switch (location.split("/")[0]) {
                    case "ActivityDefinition" -> ourClient.fetchResourceFromUrl(ActivityDefinition.class, location);
                    case "Library" -> ourClient.fetchResourceFromUrl(Library.class, location);
                    case "Measure" -> ourClient.fetchResourceFromUrl(Measure.class, location);
                    case "PlanDefinition" -> ourClient.fetchResourceFromUrl(PlanDefinition.class, location);
                    case "ValueSet" -> ourClient.fetchResourceFromUrl(ValueSet.class, location);
                    default -> null;
                })
                .forEach(callback);
    }
}
