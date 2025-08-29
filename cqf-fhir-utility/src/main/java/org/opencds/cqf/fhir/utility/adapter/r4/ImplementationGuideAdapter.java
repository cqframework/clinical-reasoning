package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;

public class ImplementationGuideAdapter extends KnowledgeArtifactAdapter implements IImplementationGuideAdapter {

    public ImplementationGuideAdapter(IDomainResource implementationGuide) {
        super(implementationGuide);
        if (!(implementationGuide instanceof ImplementationGuide)) {
            throw new IllegalArgumentException(
                    "resource passed as implementationGuide argument is not a ImplementationGuide resource");
        }
    }

    public ImplementationGuideAdapter(ImplementationGuide implementationGuide) {
        super(implementationGuide);
    }

    protected ImplementationGuide getImplementationGuide() {
        return (ImplementationGuide) resource;
    }

    @Override
    public ImplementationGuide get() {
        return (ImplementationGuide) resource;
    }

    @Override
    public ImplementationGuide copy() {
        return get().copy();
    }

}
