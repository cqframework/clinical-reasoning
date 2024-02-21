package org.opencds.cqf.fhir.utility.visitor.r5;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.r5LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public interface r5KnowledgeArtifactVisitor extends KnowledgeArtifactVisitor {
    //	void visit(ActivityDefinitionAdapter activityDefinition);
    //   IBase visit(r5KnowledgeArtifactAdapter knowledgeArtifact, Repository repository, IBaseParameters
    // draftParameters);
    IBase visit(r5LibraryAdapter library, Repository repository, Parameters draftParameters);

    IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters draftParameters);
    //  //	void visit(StructureDefinitionAdapter structureDefinition);
    IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters draftParameters);
}
