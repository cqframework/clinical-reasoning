package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.visitor.r4.r4KnowledgeArtifactVisitor;

public interface r4PlanDefinitionAdapter extends r4KnowledgeArtifactAdapter, IBasePlanDefinitionAdapter {
    IBase accept(r4KnowledgeArtifactVisitor visitor, Repository repository, Parameters operationParameters);

    PlanDefinition get();
}
