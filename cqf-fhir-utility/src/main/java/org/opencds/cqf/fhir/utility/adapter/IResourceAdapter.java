package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Constants;

public interface IResourceAdapter extends IAdapter<IBaseResource> {

    IBaseResource get();

    default IIdType getId() {
        return get().getIdElement();
    }

    IBase setProperty(String name, IBase value) throws FHIRException;

    IBase addChild(String name) throws FHIRException;

    IBase getSingleProperty(String name) throws FHIRException;

    IBase[] getProperty(String name) throws FHIRException;

    IBase[] getProperty(String name, boolean checkValid) throws FHIRException;

    IBase makeProperty(String name) throws FHIRException;

    String[] getTypesForProperty(String name) throws FHIRException;

    IBaseResource copy();

    void copyValues(IBaseResource destination);

    boolean equalsDeep(IBase other);

    boolean equalsShallow(IBase other);

    default <R extends IBaseResource> List<R> getContained() {
        return getContained(get());
    }

    default boolean hasContained() {
        return hasContained(get());
    }

    @SuppressWarnings("unchecked")
    default <R extends IBaseResource> List<R> getContained(IBaseResource base) {
        return resolvePathList(base, "contained", IBaseResource.class).stream()
                .map(r -> (R) r)
                .collect(Collectors.toList());
    }

    default Boolean hasContained(IBaseResource base) {
        return !getContained(base).isEmpty();
    }

    default void addContained(IBaseResource base) {
        var res = resolvePathList(get(), "contained", IBaseResource.class);
        res.add(base);
        getModelResolver().setValue(get(), "contained", res);
    }

    default boolean hasProperty(String propertyName) {
        // should consider caching this?
        Set<String> propNames = fhirContext().getResourceDefinition(get()).getChildren().stream()
                .map(BaseRuntimeChildDefinition::getElementName)
                .collect(Collectors.toSet());
        return propNames.contains(propertyName);
    }

    @SuppressWarnings("unchecked")
    default <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifact() {
        List<T> artifacts = new ArrayList<>();
        if (hasProperty("relatedArtifact")) {
            List<T> relatedArtifacts = resolvePathList(get(), "relatedArtifact").stream()
                    .map(r -> (T) r)
                    .toList();
            artifacts.addAll(relatedArtifacts);
        } else {
            // for KnowledgeResources that do not have relatedArtifact properties,
            // we'll filter the extensions for these 2 RelatedArtifact
            List<T> extensionArtifacts = getExtensionsByUrls(
                            get(), Set.of(Constants.CPG_RELATED_ARTIFACT, Constants.ARTIFACT_RELATED_ARTIFACT))
                    .stream()
                    .filter(ext -> {
                        return ext.getValue() != null
                                && ext.getValue().fhirType().equals("RelatedArtifact");
                    })
                    .map(ext -> (T) ext.getValue())
                    .toList();
            artifacts.addAll(extensionArtifacts);
        }
        return artifacts;
    }
}
