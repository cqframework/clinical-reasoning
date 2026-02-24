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

public interface IQuestionnaireAdapterTest<T extends IBaseResource> extends IBaseAdapterTest {
    Logger log = LoggerFactory.getLogger(IQuestionnaireAdapterTest.class);

    String RELATED_ARTIFACT_TYPE_1 = "RELATED_ARTIFACT_TYPE_1";
    String RELATED_ARTIFACT_TYPE_2 = "RELATED_ARTIFACT_TYPE_2";
    String RESOURCE_REF_1 = "RESOURCE_REF_1";
    String RESOURCE_REF_2 = "RESOURCE_REF_2";

    String TEMPLATE = """
        {
            "resourceType": "Questionnaire",
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

    Class<T> questionnaireClass();

    @Test
    default void getRelatedArtifact_withExtensions_includesArtifactsFromExtensions() {
        // setup
        String canonical1 = "http://canonical.com/url-1";
        String canonical2 = "http://canonical.com/url-2";
        IParser parser = fhirContext().newJsonParser();
        String questionnaireStr = TEMPLATE.replaceAll(RESOURCE_REF_1, toRelatedArtifactCanonicalReference(canonical1))
                .replaceAll(RESOURCE_REF_2, toRelatedArtifactCanonicalReference(canonical2))
                .replaceAll(RELATED_ARTIFACT_TYPE_1, "depends-on")
                .replaceAll(RELATED_ARTIFACT_TYPE_2, "depends-on");
        log.info(questionnaireStr);

        T questionnaire = parser.parseResource(questionnaireClass(), questionnaireStr);

        IQuestionnaireAdapter adapter = getAdapterFactory().createQuestionnaire(questionnaire);

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
            "resourceType": "Questionnaire",
            "status": "active",
            "extension": [
                {
                    "url": "http://hl7.org/fhir/uv/cpg/StructureDefinition/something-else",
                    "valueRelatedArtifact": {
                        "type": "depends-on",
                        "resource": REF
                    }
                }
            ]
        }
        """.replaceAll("REF", toRelatedArtifactCanonicalReference("some-ref"));

        T questionnaire = parser.parseResource(questionnaireClass(), str);

        IQuestionnaireAdapter adapter = getAdapterFactory().createQuestionnaire(questionnaire);

        // test
        List<? extends ICompositeType> artifacts = adapter.getRelatedArtifact();

        // verify
        assertTrue(artifacts.isEmpty());
    }
}
