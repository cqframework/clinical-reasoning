package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import java.util.Optional;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class DefinitionItem {
    private String rootLinkId;
    private String resourceType;
    private BaseRuntimeElementDefinition<?> resourceDefinition;
    private Optional<IStructureDefinitionAdapter> profile;

    public DefinitionItem(String rootLinkId) {
        this.rootLinkId = rootLinkId;
    }

    public String getRootLinkId() {
        return rootLinkId;
    }
}
