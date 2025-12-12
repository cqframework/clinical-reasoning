package org.opencds.cqf.fhir.utility.adapter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ExtensionUtil;
import ca.uhn.fhir.util.FhirTerser;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This interface exposes common functionality across all FHIR GraphDefinition versions.
 */
public interface IGraphDefinitionAdapter<GRAPHDEF extends IBaseResource> extends IKnowledgeArtifactAdapter {

    // R4
    List<IBaseBackboneElement> getBackBoneElements();

    // R5
    List<IBaseBackboneElement> getNode();

    /**
     * Proper Fhir version  class for Extension
     * @return
     * @param <EXTENSION>
     */
    <EXTENSION extends IBaseExtension<?, ?>> Class<EXTENSION> extensionClass();

    /**
     * Retrieve the reference from the RelatedArtifact
     * @param artifact - RelatedArtifact from off of Extension
     * @return the reference (either a canonical URL or a resource reference)
     */
    default <ARTIFACT extends IBaseDatatype> String getReferenceFromArtifact(ARTIFACT artifact) {
        FhirTerser terser = fhirContext().newTerser();

        IPrimitiveType<String> canonicalUrl = terser.getSingleValueOrNull(artifact, "resource", IPrimitiveType.class);
        return canonicalUrl == null ? null : canonicalUrl.getValue();
    }

    <RA extends IBaseDatatype> void validateRelatedArtifact(RA relatedArtifact, List<String> errors);

    default void extractRelatedArtifactReferences(
            GRAPHDEF graphDefinition, String referenceSource, List<IDependencyInfo> dependencies) {
        List<String> errors = new ArrayList<>();

        FhirContext ctx = fhirContext();
        FhirTerser terser = ctx.newTerser();

        // get all RelatedArtifact extensions
        List<IBaseExtension<?, ?>> extensions = ExtensionUtil.getExtensionsMatchingPredicate(graphDefinition, e -> {
            return e.getUrl().equals(Constants.ARTIFACT_RELATED_ARTIFACT)
                    || e.getUrl().equals(Constants.CPG_RELATED_ARTIFACT);
        });

        // process the extensions
        for (IBaseExtension<?, ?> extension : extensions) {
            IBaseDatatype relatedArtifact = extension.getValue();
            validateRelatedArtifact(relatedArtifact, errors);
            if (!errors.isEmpty()) {
                break;
            }
            String canonicalUrl = getReferenceFromArtifact(relatedArtifact);

            if (isBlank(canonicalUrl)) {
                errors.add("No Canonical reference");
                break;
            }

            // TODO - handle RelatedArtifacts based on type

            List<? extends IBaseExtension<?, ?>> extensionList =
                    terser.getValues(relatedArtifact, "extension", extensionClass());
            dependencies.add(new DependencyInfo(referenceSource, canonicalUrl, extensionList, ref -> {
                terser.setElement(relatedArtifact, "resource", ref);
            }));
        }

        if (!errors.isEmpty()) {
            handleGetDependenciesErrors(errors);
        }
    }

    default void handleGetDependenciesErrors(List<String> errors) {
        throw new IllegalArgumentException(String.join(", ", errors));
    }
}
