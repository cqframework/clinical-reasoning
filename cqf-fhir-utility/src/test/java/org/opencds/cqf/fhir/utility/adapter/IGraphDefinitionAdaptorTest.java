package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.parser.IParser;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IGraphDefinitionAdaptorTest<T extends IBaseResource> extends IBaseAdapterTest {

    Logger log = LoggerFactory.getLogger(IGraphDefinitionAdaptorTest.class);

    String PROFILE_REF = "PROFILE_REF";
    String RELATED_ARTIFACT_TYPE_1 = "RELATED_ARTIFACT_TYPE_1";
    String RELATED_ARTIFACT_TYPE_2 = "RELATED_ARTIFACT_TYPE_2";
    String RESOURCE_REF_1 = "RESOURCE_REF_1";
    String RESOURCE_REF_2 = "RESOURCE_REF_2";

    String VALID_GRAPH_DEF_JSON_TEMPLATE = """
            {
                "resourceType": "GraphDefinition",
                "meta": [{
                    "profile": "PROFILE_REF"
                }],
                "extension": [
                    {
                        "url": "http://hl7.org/fhir/StructureDefinition/artifact-relatedArtifact",
                        "valueRelatedArtifact": {
                            "type": "RELATED_ARTIFACT_TYPE_1",
                            "resource": RESOURCE_REF_1
                        }
                    },
                    {
                        "url": "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedArtifact",
                        "valueRelatedArtifact": {
                            "type": "RELATED_ARTIFACT_TYPE_2",
                            "resource": RESOURCE_REF_2
                        }
                    }
                ]
            }
            """;

    Class<T> graphDefinitionClass();

    /**
     * Returns the list of all RelatedArtifactType values that are not valid for getDependencies
     */
    List<String> getAllNonProcessableTypeForRelatedArtifact();

    @Test
    default void getDependencies_withRelatedArtifact_retrievesCorrectReference() {
        // setup
        String profileRef = "profileRef";
        String resourceRef1 = "http://example.com/canonical-url-1";
        String resourceRef2 = "http://example.com/canonical-url-2";
        IParser parser = fhirContext().newJsonParser();
        String graphDefStr = VALID_GRAPH_DEF_JSON_TEMPLATE
                .replaceAll(PROFILE_REF, profileRef)
                .replaceAll(RESOURCE_REF_1, toRelatedArtifactCanonicalReference(resourceRef1))
                .replaceAll(RESOURCE_REF_2, toRelatedArtifactCanonicalReference(resourceRef2))
                .replaceAll(RELATED_ARTIFACT_TYPE_1, "depends-on")
                .replaceAll(RELATED_ARTIFACT_TYPE_2, "depends-on");
        log.info(graphDefStr);
        T graphDefinition = parser.parseResource(graphDefinitionClass(), graphDefStr);

        // profile + 1 for each of the RelatedArtifacts
        // (but a variable in case this changes)
        int dependenciesExpected = 3;

        // test
        IGraphDefinitionAdapter adapter = getAdapterFactory().createGraphDefinition(graphDefinition);
        List<IDependencyInfo> dependencies = adapter.getDependencies();

        // verify
        assertEquals(dependenciesExpected, dependencies.size());
        // has profile
        assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(profileRef)));
        // has the canonical references for relatedartifacts
        assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(resourceRef1)));
        assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(resourceRef2)));
    }

    @Test
    default void getDependencies_noRef_isIgnored() {
        // setup
        String ref = "http://example.com/canonical";
        IParser parser = fhirContext().newJsonParser();
        String graphDefStr = String.format("""
                {
                    "resourceType": "GraphDefinition",
                    "meta": [{
                        "profile": "profileRef"
                    }],
                    "extension": [
                        {
                            "url": "http://hl7.org/fhir/StructureDefinition/artifact-relatedArtifact",
                            "valueRelatedArtifact": {
                                "type": "depends-on",
                                "resource": %s
                            }
                        },
                        {
                            "url": "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedArtifact",
                            "valueRelatedArtifact": {
                                "type": "depends-on"
                            }
                        }
                    ]
                }
                """, toRelatedArtifactCanonicalReference(ref));
        log.info(graphDefStr);
        T graphDefinition = parser.parseResource(graphDefinitionClass(), graphDefStr);

        // test
        IGraphDefinitionAdapter adapter = getAdapterFactory().createGraphDefinition(graphDefinition);
        List<IDependencyInfo> dependencies = adapter.getDependencies();

        // validate
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals("profileRef")));
        assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(ref)));
    }

    @Test
    default void getDependencies_noRelatedArtifact_processesButReturnsNothing() {
        // setup
        String graphDefStr = """
                {
                   "resourceType": "GraphDefinition",
                   "meta": [{
                        "profile": "profileRef"
                   }],
                   "extension": [
                    {
                        "url": "http://hl7.org/fhir/StructureDefinition/artifact-relatedArtifact",
                        "reference": "resource-ref"
                    },
                    {
                        "url": "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedArtifact",
                        "reference": "resource-ref2"
                    }
                ]
                }
                """;
        log.info(graphDefStr);
        IParser parser = fhirContext().newJsonParser();
        T graphDef = parser.parseResource(graphDefinitionClass(), graphDefStr);

        // test
        IGraphDefinitionAdapter adatper = getAdapterFactory().createGraphDefinition(graphDef);
        List<IDependencyInfo> dependencies = adatper.getDependencies();

        // validate
        // no relatedartifacts -> no dependencies set
        assertEquals(1, dependencies.size());
        assertEquals("profileRef", dependencies.get(0).getReference());
    }

    @Test
    default void getDependencies_invalidArtifactType_shouldBeIgnored() {
        // setup
        for (String relatedArtifactType : getAllNonProcessableTypeForRelatedArtifact()) {
            String profileRef = "profileRef";
            String resourceRef1 = "http://example.com/canonical-url-1";
            String resourceRef2 = "http://example.com/canonical-url-2";
            IParser parser = fhirContext().newJsonParser();
            String graphDefStr = VALID_GRAPH_DEF_JSON_TEMPLATE
                    .replaceAll(PROFILE_REF, profileRef)
                    .replaceAll(RESOURCE_REF_1, toRelatedArtifactCanonicalReference(resourceRef1))
                    .replaceAll(RESOURCE_REF_2, toRelatedArtifactCanonicalReference(resourceRef2))
                    .replaceAll(RELATED_ARTIFACT_TYPE_1, "depends-on")
                    .replaceAll(
                            RELATED_ARTIFACT_TYPE_2,
                            relatedArtifactType == null ? "" : relatedArtifactType); // invalid type
            System.out.println(graphDefStr);
            T graphDefinition = parser.parseResource(graphDefinitionClass(), graphDefStr);

            IGraphDefinitionAdapter adapter = getAdapterFactory().createGraphDefinition(graphDefinition);

            // test
            List<IDependencyInfo> dependencies = adapter.getDependencies();

            // validate
            assertEquals(2, dependencies.size());
            assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(profileRef)));
            assertTrue(dependencies.stream().anyMatch(d -> d.getReference().equals(resourceRef1)));
            // invalid type should not be matched
            assertFalse(dependencies.stream().anyMatch(d -> d.getReference().equals(resourceRef2)));
        }
    }
}
