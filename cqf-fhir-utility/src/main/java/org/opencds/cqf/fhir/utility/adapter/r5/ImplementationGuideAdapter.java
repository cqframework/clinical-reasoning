package org.opencds.cqf.fhir.utility.adapter.r5;

import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;

import com.fasterxml.jackson.databind.cfg.ContextAttributes.Impl;

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

    // implementationGuide is not compatible with expected MetadataResource return
    // type in KnowledgeArtifactAdapter
    // @Override
    // public ImplementationGuide get() {
    // return (ImplementationGuide) resource;
    // }

    // implementationGuide is not compatible with expected MetadataResource return
    // type in KnowledgeArtifactAdapter
    // @Override
    // public ImplementationGuide copy() {
    // return get().copy();
    // }

}
