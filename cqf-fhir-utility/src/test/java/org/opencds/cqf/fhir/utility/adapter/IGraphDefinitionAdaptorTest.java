package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;

public interface IGraphDefinitionAdaptorTest<T extends IBaseResource> {

    class RelatedArtifactInfo {

        /**
         * The extension url
         */
        public final String ExtensionUrl;
        /**
         * The canonical url.
         */
        public final String CanonicalResourceURL;

        private Enum<?> relatedArtifactType;

        public RelatedArtifactInfo(String url, String canonicalResourceUrl) {
            ExtensionUrl = url;
            CanonicalResourceURL = canonicalResourceUrl;
        }

        public Enum<?> getRelatedArtifactType() {
            return relatedArtifactType;
        }

        public RelatedArtifactInfo setRelatedArtifactType(Enum<?> theRelatedArtifactType) {
            relatedArtifactType = theRelatedArtifactType;
            return this;
        }
    }

    class GraphDefinitionInformation {

        /**
         * The profile reference to apply to the GraphDefinition
         */
        public String ProfileRef;
        /**
         * List of any/all related artifact extensions to add
         */
        public final List<RelatedArtifactInfo> RelatedArtifactInfo;

        public GraphDefinitionInformation(String profileRef, List<RelatedArtifactInfo> relatedArtifactInfos) {
            ProfileRef = profileRef;
            RelatedArtifactInfo = relatedArtifactInfos == null ? List.of() : relatedArtifactInfos;
        }
    }

    FhirContext fhirContext();

    IAdapterFactory getAdaptorFactory();

    T getGraphDefinition(GraphDefinitionInformation information);

    @Test
    default void getDependencies_withRelatedArtifact_retrievesCorrectReference() {
        // setup
        GraphDefinitionInformation graphInfo = new GraphDefinitionInformation("profileref",
            List.of(new RelatedArtifactInfo(Constants.ARTIFACT_RELATED_ARTIFACT,  "http://example.com/canonical_1"),
                new RelatedArtifactInfo(Constants.CPG_RELATED_ARTIFACT,  "http://example.com/canonical_2")));
        T graphDefinition = getGraphDefinition(graphInfo);

        // profile + 1 for each of the RelatedArtifacts
        // (but a variable in case this changes)
        int dependenciesExpected = 3;

        // test
        IGraphDefinitionAdapter adapter = getAdaptorFactory().createGraphDefinition(graphDefinition);
        List<IDependencyInfo> dependencies = adapter.getDependencies();

        // verify
        assertEquals(dependenciesExpected, dependencies.size());
        assertTrue(dependencies.stream()
            .anyMatch(d -> d.getReference().equals(graphInfo.ProfileRef)));
        for (RelatedArtifactInfo info : graphInfo.RelatedArtifactInfo) {
            assertTrue(dependencies.stream()
                .anyMatch(d -> d.getReference().equals(info.CanonicalResourceURL)));
        }
    }

    @Test
    default void getDependencies_noRef_throwsError() {
        // setup
        GraphDefinitionInformation graphInfo = new GraphDefinitionInformation("profileref",
            List.of(new RelatedArtifactInfo(Constants.ARTIFACT_RELATED_ARTIFACT,  null),
                new RelatedArtifactInfo(Constants.CPG_RELATED_ARTIFACT,  "http://example.com/canonical_2")));
        T graphDefinition = getGraphDefinition(graphInfo);

        // test
        IGraphDefinitionAdapter adapter = getAdaptorFactory().createGraphDefinition(graphDefinition);

        try {
            List<IDependencyInfo> dependencies = adapter.getDependencies();
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("No Canonical reference"));
        }
    }

    @Test
    default void getDependencies_wrongType_throwsError() {
        // setup
        FhirTerser terser = fhirContext().newTerser();
        for (Enum<?> relatedArtifactType : getAllNonProcessableTypeForRelatedArtifact()) {
            GraphDefinitionInformation graphInfo = new GraphDefinitionInformation("profileref",
                List.of(new RelatedArtifactInfo(Constants.ARTIFACT_RELATED_ARTIFACT,
                        "http://example.com/canonical_1")
                        .setRelatedArtifactType(relatedArtifactType), // we only need to set one
                    new RelatedArtifactInfo(Constants.CPG_RELATED_ARTIFACT,
                        "http://example.com/canonical_2")));
            T graphDefinition = getGraphDefinition(graphInfo);

            // test
            try {
                IGraphDefinitionAdapter adapter = getAdaptorFactory().createGraphDefinition(
                    graphDefinition);
            } catch (IllegalArgumentException ex) {
                assertTrue(ex.getMessage()
                    .contains(
                        String.format("Expected RelatedArtifact of type DEPENDSON, but found %s", relatedArtifactType.name())));
            }
        }
    }

    /**
     * Returns the list of all RelatedArtifactType values that are
     * not valid for getDependencies
     */
    List<? extends Enum<?>> getAllNonProcessableTypeForRelatedArtifact();
}
