package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public interface IStructureDefinitionAdapter extends IKnowledgeArtifactAdapter {
    default String getType() {
        return resolvePathString(get(), "type");
    }

    default List<IBaseBackboneElement> getDifferentialElements() {
        return resolvePathList(resolvePath(get(), "differential"), "element", IBaseBackboneElement.class);
    }
}
