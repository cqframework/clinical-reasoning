package org.opencds.cqf.fhir.utility.adapter.dstu3;

import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.visitor.dstu3.dstu3KnowledgeArtifactVisitor;

public interface dstu3PlanDefinitionAdapter extends Dstu3KnowledgeArtifactAdapter, IBasePlanDefinitionAdapter {
    IBase accept(dstu3KnowledgeArtifactVisitor visitor, Repository repository, Parameters operationParameters);

    PlanDefinition get();
}
