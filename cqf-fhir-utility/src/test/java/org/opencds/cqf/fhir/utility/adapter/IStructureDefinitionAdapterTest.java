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

public interface IStructureDefinitionAdapterTest<T extends IBaseResource> extends IBaseAdapterTest {
    Logger log = LoggerFactory.getLogger(IStructureDefinitionAdapterTest.class);

    String RELATED_ARTIFACT_TYPE_1 = "RELATED_ARTIFACT_TYPE_1";
    String RELATED_ARTIFACT_TYPE_2 = "RELATED_ARTIFACT_TYPE_2";
    String RESOURCE_REF_1 = "RESOURCE_REF_1";
    String RESOURCE_REF_2 = "RESOURCE_REF_2";

    String TEMPLATE = """
        {
            "resourceType": "StructureDefinition",
            "url": "http://canonical.com/sd-url",
            "name": "sd-name",
            "status": "active",
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

    Class<T> structureDefinitionClass();

    @Test
    default void getRelatedArtifact_withExtensions_includesRAFromExtensions() {
        // setup
        String canonical1 = "http://canonical.com/res1";
        String canonical2 = "http://canonical.com/res2";
        IParser parser = fhirContext().newJsonParser();
        String structureDefinitionStr = TEMPLATE.replaceAll(
                        RESOURCE_REF_1, toRelatedArtifactCanonicalReference(canonical1))
                .replaceAll(RESOURCE_REF_2, toRelatedArtifactCanonicalReference(canonical2))
                .replaceAll(RELATED_ARTIFACT_TYPE_1, "depends-on")
                .replaceAll(RELATED_ARTIFACT_TYPE_2, "depends-on");
        log.info(structureDefinitionStr);

        T sd = parser.parseResource(structureDefinitionClass(), structureDefinitionStr);

        var adapter = getAdapterFactory().createStructureDefinition(sd);

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
    default void getRelatedArtifact_noRelevantExtension_returnsNothing() {
        // setup
        IParser parser = fhirContext().newJsonParser();
        String str = """
        {
            "resourceType": "StructureDefinition",
            "url": "http://canonical.com/sd-url",
            "name": "sd-name",
            "status": "active",
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
        """.replaceAll("REF", toRelatedArtifactCanonicalReference("some-ref"));

        T structuredDef = parser.parseResource(structureDefinitionClass(), str);
        IStructureDefinitionAdapter adapter = getAdapterFactory().createStructureDefinition(structuredDef);

        // test
        List<? extends ICompositeType> relatedArtifacts = adapter.getRelatedArtifact();

        // verify
        assertTrue(relatedArtifacts.isEmpty());
    }
}
