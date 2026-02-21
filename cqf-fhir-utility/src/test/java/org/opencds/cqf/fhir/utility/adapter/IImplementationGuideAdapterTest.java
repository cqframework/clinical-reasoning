package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.parser.IParser;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IImplementationGuideAdapterTest<T extends IBaseResource> extends IBaseAdapterTest {
    Logger log = LoggerFactory.getLogger(IImplementationGuideAdapterTest.class);

    String RELATED_ARTIFACT_TYPE_1 = "RELATED_ARTIFACT_TYPE_1";
    String RELATED_ARTIFACT_TYPE_2 = "RELATED_ARTIFACT_TYPE_2";
    String RESOURCE_REF_1 = "RESOURCE_REF_1";
    String RESOURCE_REF_2 = "RESOURCE_REF_2";
    String FHIR_VERSION = "FHIR_VERSION";

    // packageId is a required field for R4 and R5, but not dstu3
    // so we do not include it
    String TEMPLATE = """
        {
            "resourceType": "ImplementationGuide",
            "url": "http://canonical.com/ig-url",
            "name": "ig-name",
            "status": "active",
            "fhirVersion": ["FHIR_VERSION"],
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

    Class<T> implementationGuideClass();

    default String getDefaultTemplate() {
        return TEMPLATE.replaceAll(
                FHIR_VERSION, fhirContext().getVersion().getVersion().getFhirVersionString());
    }

    @Test
    default void getRelatedArtifact_withExtensions_includesRAFromExtensions() {
        // setup
        String canonical1 = "http://canonical.com/res1";
        String canonical2 = "http://canonical.com/res2";
        IParser parser = fhirContext().newJsonParser();
        String igStr = getDefaultTemplate()
                .replaceAll(RESOURCE_REF_1, toRelatedArtifactCanonicalReference(canonical1))
                .replaceAll(RESOURCE_REF_2, toRelatedArtifactCanonicalReference(canonical2))
                .replaceAll(RELATED_ARTIFACT_TYPE_1, "depends-on")
                .replaceAll(RELATED_ARTIFACT_TYPE_2, "depends-on");
        log.info(igStr);

        T ig = parser.parseResource(implementationGuideClass(), igStr);

        var adapter = getAdapterFactory().createImplementationGuide(ig);

        // test
        List<? extends ICompositeType> relatedArtifacts = adapter.getRelatedArtifact();

        // verify
        assertEquals(2, relatedArtifacts.size());
        assertTrue(relatedArtifacts.stream().allMatch(a -> a.fhirType().equals("RelatedArtifact")));
        assertTrue(relatedArtifacts.stream()
                .anyMatch(ra -> parser.encodeToString(ra).contains(canonical1)));
        assertTrue(relatedArtifacts.stream()
                .anyMatch(ra -> parser.encodeToString(ra).contains(canonical2)));
    }

    @Test
    default void getRelatedArtifact_noValidExtensions_returnsNothing() {
        // setup
        IParser parser = fhirContext().newJsonParser();
        String str = """
         {
            "resourceType": "ImplementationGuide",
            "url": "http://canonical.com/ig-url",
            "name": "ig-name",
            "status": "active",
            "fhirVersion": "FHIR_VERSION",
            "extension": [
                {
                    "url": "http://hl7.org/fhir/StructureDefinition/something-else",
                    "valueRelatedArtifact": {
                        "type": "depends-on",
                        "resource": REF
                    }
                }
            ]
        }
        """.replaceAll(
                        FHIR_VERSION, fhirContext().getVersion().getVersion().getFhirVersionString())
                .replaceAll("REF", toRelatedArtifactCanonicalReference("some-ref"));

        T ig = parser.parseResource(implementationGuideClass(), str);

        IImplementationGuideAdapter adapter = getAdapterFactory().createImplementationGuide(ig);

        // test
        List<? extends ICompositeType> artifacts = adapter.getRelatedArtifact();

        // verify
        assertTrue(artifacts.isEmpty());
    }
}
