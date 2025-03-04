package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

public interface IStructureDefinitionAdapter extends IKnowledgeArtifactAdapter {
    default String getType() {
        return resolvePathString(get(), "type");
    }

    List<IElementDefinitionAdapter> getSnapshotElements();

    List<IElementDefinitionAdapter> getDifferentialElements();

    default IElementDefinitionAdapter getElement(String elementId) {
        return getDifferentialElements().stream()
                .filter(e -> e.getId().equals(elementId))
                .findFirst()
                .orElseGet(() -> getSnapshotElements().stream()
                        .filter(e -> e.getId().equals(elementId))
                        .findFirst()
                        .orElse(null));
    }

    /**
     * Returns the first element found with a matching path. Differential elements will be returned first. Elements with a slicing defined will be ignored.
     * @param path The path of the element without the preceding resource type. e.g. value[x] rather than Observation.value[x]
     * @return
     */
    default IElementDefinitionAdapter getElementByPath(String path) {
        return getDifferentialElements().stream()
                .filter(e -> !e.hasSlicing())
                .filter(e -> path.equals(e.getPath().substring(e.getPath().indexOf(".") + 1)))
                .findFirst()
                .orElseGet(() -> getSnapshotElements().stream()
                        .filter(e -> !e.hasSlicing())
                        .filter(e ->
                                path.equals(e.getPath().substring(e.getPath().indexOf(".") + 1)))
                        .findFirst()
                        .orElse(null));
    }

    default List<IElementDefinitionAdapter> getSliceElements(String sliceName) {
        return getDifferentialElements().stream()
                .filter(e -> e.getId().contains(sliceName) && StringUtils.isBlank(e.getSliceName()))
                .toList();
    }
}
