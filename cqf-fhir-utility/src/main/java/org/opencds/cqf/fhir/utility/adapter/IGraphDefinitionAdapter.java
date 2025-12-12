package org.opencds.cqf.fhir.utility.adapter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This interface exposes common functionality across all FHIR GraphDefinition versions.
 */
public interface IGraphDefinitionAdapter extends IKnowledgeArtifactAdapter {

    // R4
    List<IBaseBackboneElement> getBackBoneElements();

    // R5
    List<IBaseBackboneElement> getNode();

    /**
     * Validates the RelatedArtifact fhir type to ensure it is, indeed,
     * a RelatedArtifact and has the correct type value.
     * @param relatedArtifact the potential related artifact from the extension's value field
     */
    <RA extends IBaseDatatype> boolean canProcessRelatedArtifact(RA relatedArtifact);

    /**
     * Retrieve the reference from the RelatedArtifact
     * @param artifact - RelatedArtifact from off of Extension
     * @return the reference (either a canonical URL or a resource reference)
     */
    default <ARTIFACT extends IBaseDatatype> String getReferenceFromArtifact(ARTIFACT artifact) {
        return resolvePathString(artifact, "resource");
    }

    /**
     * Extracts the RelatedArtifact.resource values from a given GraphDefinition
     * @param referenceSource the resource source
     * @param dependencies list of dependencies on which to append the found references
     */
    default void extractRelatedArtifactReferences(String referenceSource, List<IDependencyInfo> dependencies) {
        for (String url : new String[] { Constants.ARTIFACT_RELATED_ARTIFACT, Constants.CPG_RELATED_ARTIFACT }) {
            IBaseExtension<?, ?> extension = getExtensionByUrl(url);

            IBaseDatatype relatedArtifact = extension.getValue();
            if (!canProcessRelatedArtifact(relatedArtifact)) {
                continue;
            }

            String canonicalUrl = getReferenceFromArtifact(relatedArtifact);

            if (isBlank(canonicalUrl)) {
                continue;
            }

            List<? extends IBaseExtension<?, ?>> extensionList = getExtension(relatedArtifact);
            dependencies.add(new DependencyInfo(referenceSource, canonicalUrl, extensionList, ref -> {
                // do nothing
            }));
        }
    }
}
